
package com.sparta.payment_system.service;

import com.sparta.payment_system.client.PortOneClient;
import com.sparta.payment_system.dto.payment.PaymentVerificationDto; // 💡 DTO Import
import com.sparta.payment_system.entity.*;
import com.sparta.payment_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RefundRepository refundRepository;
    private final PointTransactionRepository pointTransactionRepository;

    // 재고 관리 및 결제 검증을 위한 의존성
    private final ProductRepository productRepository;
    private final PortOneClient portOneClient;

    /**
     * 결제 생성 및 준비
     * - 재고 확인
     * - 포인트 사용 계산
     * - Payment 엔티티 생성 (FAILED 상태)
     */
    @Transactional
    public Payment createPayment(Long orderId, boolean usePoints) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid orderId"));
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        // 1. 재고 사전 확인
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct().getStock() < item.getQuantity()) {
                throw new IllegalStateException("재고 부족: " + item.getProduct().getName());
            }
        }

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal pointsUsed = BigDecimal.ZERO;

        // 2. 포인트 사용 로직
        if (usePoints) {
            pointsUsed = user.getTotalPoints().min(totalAmount);
            totalAmount = totalAmount.subtract(pointsUsed);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setPointsUsed(pointsUsed);
        payment.setDiscountAmount(BigDecimal.ZERO);
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setMethodId(Payment.PaymentMethod.CARD.getId());

        return paymentRepository.save(payment);
    }


    @Transactional
    public Payment completePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));

        if (payment.getStatus() == Payment.PaymentStatus.PAID) return payment;

        // 1. PortOne 결제 검증 )

        PaymentVerificationDto verification = portOneClient.getPayment(payment.getPaymentKey());

        if (verification == null) {
            throw new IllegalStateException("PortOne에서 결제 정보를 찾을 수 없습니다.");
        }

        // 금액 비교
        if (payment.getAmount().compareTo(verification.getAmount()) != 0) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("결제 금액 불일치 (위변조 의심)");
        }

        // PortOne 상태 확인
        if (!"paid".equalsIgnoreCase(verification.getStatus())) {
            throw new IllegalStateException("PortOne 결제 상태가 PAID가 아닙니다: " + verification.getStatus());
        }

        // 2. 내부 상태 업데이트
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        // 3. 주문 상태 동기화
        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        // 4. 재고 실차감 로직
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) {
                throw new IllegalStateException("재고 부족으로 결제 완료 불가");
            }
            product.setStock(newStock);
            productRepository.save(product);
        }

        User user = getUserByPayment(payment);

        // 5. 사용 포인트 차감
        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().subtract(payment.getPointsUsed()));
            createPointTransaction(user, payment.getPointsUsed(), PointTransaction.PointType.USED);
        }

        // 6. 포인트 적립 (결제 금액의 1%)
        BigDecimal earnedPoints = payment.getAmount().multiply(new BigDecimal("0.01"));
        if (earnedPoints.compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(earnedPoints));
            createPointTransaction(user, earnedPoints, PointTransaction.PointType.EARNED);
        }

        userRepository.save(user); // 포인트 변경사항 저장
        updateMembershipLevel(user);

        return paymentRepository.save(payment);
    }


    @Transactional
    public Payment completePaymentByPaymentKey(String paymentKey) {
        // 1. paymentKey로 Payment 엔티티 조회 (getPaymentByPaymentKey는 이미 구현되어 있다고 가정)
        Payment payment = getPaymentByPaymentKey(paymentKey);

        // 2. 조회된 Payment ID로 기존의 복잡한 완료 처리 로직 호출
        return completePayment(payment.getPaymentId());
    }

    // PaymentService.java 내부에 추가


    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId: " + paymentId));
    }

    @Transactional
    public Payment failPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));
        payment.setStatus(Payment.PaymentStatus.FAILED);

        // 실패 시 주문 상태 동기화
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        return paymentRepository.save(payment);
    }


    @Transactional
    public Refund refundPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));

        return processRefundLogic(payment, reason);
    }


    @Transactional
    public Mono<Boolean> cancelPayment(String paymentKey, String reason) {
        return Mono.fromCallable(() -> {
            Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));

            if (payment.getStatus() != Payment.PaymentStatus.PAID) {
                return false;
            }

            processRefundLogic(payment, reason);
            return true;
        });
    }


    private Refund processRefundLogic(Payment payment, String reason) {
        if (payment.getStatus() != Payment.PaymentStatus.PAID)
            throw new IllegalStateException("Payment is not completed, cannot refund");

        // 1. Refund 기록 생성
        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setReason(reason);
        refund.setStatus(Refund.RefundStatus.COMPLETED); // 즉시 완료 처리
        refundRepository.save(refund);

        // 2. Payment 상태 변경
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // 3. 주문 상태 동기화
        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 4. 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        User user = getUserByPayment(payment);

        // 5. 사용 포인트 복구
        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(payment.getPointsUsed()));
            createPointTransaction(user, payment.getPointsUsed(), PointTransaction.PointType.EARNED); // 혹은 RESTORED
        }

        // 6. 적립 포인트 회수
        BigDecimal earnedPoints = payment.getAmount().multiply(new BigDecimal("0.01"));
        if (earnedPoints.compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().subtract(earnedPoints));
            createPointTransaction(user, earnedPoints, PointTransaction.PointType.USED); // 혹은 DEDUCTED
        }

        userRepository.save(user);
        updateMembershipLevel(user);

        return refund;
    }


    private void createPointTransaction(User user, BigDecimal amount, PointTransaction.PointType type) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setPointsChanged(amount);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());
        pointTransactionRepository.save(transaction);
    }

    private User getUserByPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null) throw new IllegalArgumentException("Payment has no associated order");
        return userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));
    }

    private void updateMembershipLevel(User user) {
        BigDecimal totalSpent = paymentRepository.findAllByOrderUserId(user.getUserId())
                .stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSpent.compareTo(new BigDecimal("100000")) >= 0) {
            user.setMembershipRank(MembershipRank.VVIP);
        } else if (totalSpent.compareTo(new BigDecimal("50000")) >= 0) {
            user.setMembershipRank(MembershipRank.VIP);
        } else {
            user.setMembershipRank(MembershipRank.NORMAL);
        }

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));
    }
}



