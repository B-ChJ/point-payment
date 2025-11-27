package com.sparta.payment_system.service;

import com.sparta.payment_system.client.PortOneClient;
import com.sparta.payment_system.dto.refund.RefundRequestDto;
import com.sparta.payment_system.dto.refund.RefundResponseDto;
import com.sparta.payment_system.entity.*;
import com.sparta.payment_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;


    //사용자 요청에 의한 환불
    @Transactional
    public RefundResponseDto createRefund(RefundRequestDto requestDto, Long currentUserId) {

        Payment payment = paymentRepository.findById(requestDto.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));


        if (!payment.getOrder().getUserId().equals(currentUserId)) {
            throw new SecurityException("해당 결제 건에 대한 환불 권한이 없습니다.");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태(PAID)에서만 환불 요청이 가능합니다.");
        }

        // PG사 환불 요청 PortOne API 호출
        try {
            String accessToken = portOneClient.getAccessToken();
            // PortOne에 환불 요청
            portOneClient.cancelPayment(payment.getPaymentKey(), accessToken, requestDto.getReason());
        } catch (Exception e) {
            // PG사 요청 실패 시 예외 발생, 트랜잭션 롤백 유도
            throw new RuntimeException("PortOne 환불 요청 실패: " + e.getMessage());
        }

        // 3. 내부 시스템 후처리 (로직의 재사용성을 위해 분리)
        Refund savedRefund = processRefundLogic(payment, requestDto.getReason());

        return convertToRefundResponseDto(savedRefund);
    }

    @Transactional
    public void processWebhookRefund(String paymentKey, String reason) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));

        processRefundLogic(payment, reason);
    }



    private Refund processRefundLogic(Payment payment, String reason) {

        // 1. 중복 환불 방지
        if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            Optional<Refund> existingRefund = refundRepository.findByPaymentId(payment.getPaymentId()).stream().findFirst();
            return existingRefund
                    .orElseThrow(() -> new IllegalStateException("Payment already refunded but refund record missing."));
        }

        // 2. Refund 기록 생성
        Refund refund = new Refund();
        refund.setPaymentId(payment.getPaymentId());
        refund.setAmount(payment.getAmount());
        refund.setReason(reason);
        refund.setStatus(Refund.RefundStatus.COMPLETED);
        Refund savedRefund = refundRepository.save(refund);

        // 3. Payment 및 Order 상태 변경
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

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

        // 5. 사용 포인트 복구 (잔액 증가)
        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(payment.getPointsUsed()));
            createPointTransaction(user, payment.getPointsUsed(), PointTransaction.PointType.EARNED);
        }

        // 6. 적립 포인트 회수 (잔액 감소)
        BigDecimal earnedPoints = payment.getAmount().multiply(new BigDecimal("0.01"));
        if (earnedPoints.compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().subtract(earnedPoints));
            createPointTransaction(user, earnedPoints, PointTransaction.PointType.USED);
        }

        // 7. 멤버십 업데이트 (환불로 인해 누적 결제액 감소)
        userRepository.save(user);
        updateMembershipLevel(user);

        return savedRefund;
    }


    private User getUserByPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null) throw new IllegalArgumentException("Payment has no associated order");
        return userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));
    }

    private void createPointTransaction(User user, BigDecimal amount, PointTransaction.PointType type) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setPointsChanged(amount);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());
        pointTransactionRepository.save(transaction);
    }

    private void updateMembershipLevel(User user) {
        // PAID 상태의 결제 금액만 합산하여 등급 결정
        BigDecimal totalSpentBigDecimal = paymentRepository.findAllByOrderUserId(user.getUserId())
                .stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalPaid = totalSpentBigDecimal.longValue();
        MembershipRank newRank = MembershipRank.fromTotalPaid(totalPaid);

        user.setMembershipRank(newRank);
        userRepository.save(user);
    }


    private RefundResponseDto convertToRefundResponseDto(Refund refund) {
        RefundResponseDto dto = new RefundResponseDto();
        dto.setRefundId(refund.getRefundId());
        dto.setPaymentId(refund.getPaymentId());
        dto.setAmount(refund.getAmount());
        dto.setStatus(refund.getStatus().name());
        dto.setReason(refund.getReason());

        dto.setRefundedAt(refund.getRefundedAt());

        return dto;
    }
}