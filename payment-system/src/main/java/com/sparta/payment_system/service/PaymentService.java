package com.sparta.payment_system.service;

import com.sparta.payment_system.client.PortOneClient;
import com.sparta.payment_system.dto.payment.PaymentRequestDto;
import com.sparta.payment_system.dto.payment.PaymentResponseDto;
import com.sparta.payment_system.dto.payment.PaymentVerificationDto;
import com.sparta.payment_system.dto.payment.PortOnePaymentReadyResponseDto;
import com.sparta.payment_system.entity.*;
import com.sparta.payment_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 결제 플로우 전반을 관리하고 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PortOneClient portOneClient;


    /**
     * 외부 결제를 위해 결제 준비 정보를 생성하고 저장합니다.
     * 재고, 포인트 사용을 검증하여 최종 결제 금액을 확정합니다.
     *
     * @param userId 사용자 ID
     * @param orderId 주문 ID
     * @param requestDto 결제 요청 DTO (포인트 사용 여부)
     * @return PortOne 결제 준비 응답 DTO
     */
    @Transactional
    public PortOnePaymentReadyResponseDto createPaymentReady(
            Long userId,
            Long orderId,
            PaymentRequestDto requestDto
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("인증된 사용자(ID: " + userId + ")를 찾을 수 없습니다."));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("해당 주문에 대한 결제 권한이 없습니다.");
        }

        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct().getStock() < item.getQuantity()) {
                throw new IllegalStateException("재고 부족: " + item.getProduct().getName());
            }
        }

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal pointsUsed = BigDecimal.ZERO;
        BigDecimal finalPaymentAmount = totalAmount;

        if (requestDto.isUsePoint()) {
            pointsUsed = user.getTotalPoints().min(totalAmount);
            finalPaymentAmount = totalAmount.subtract(pointsUsed);
        }


        Optional<Payment> existingPayment = paymentRepository.findByOrder_OrderId(orderId);

        Payment payment;
        if (existingPayment.isPresent()) {

            payment = existingPayment.get();
        } else {
            payment = new Payment();
            payment.setOrder(order);
        }

        payment.setAmount(finalPaymentAmount);
        payment.setPointsUsed(pointsUsed);
        payment.setDiscountAmount(BigDecimal.ZERO);


        payment.setStatus(Payment.PaymentStatus.FAILED);

        payment.setMethodId(Payment.PaymentMethod.CARD.getId());

        String newPaymentKey = "T" + System.currentTimeMillis() + orderId;
        payment.setPaymentKey(newPaymentKey);

        Payment savedPayment = paymentRepository.save(payment);

        PortOnePaymentReadyResponseDto readyInfo = new PortOnePaymentReadyResponseDto();
        readyInfo.setPaymentId(savedPayment.getPaymentId());
        readyInfo.setPaymentKey(newPaymentKey);
        readyInfo.setAmount(finalPaymentAmount);

        String orderName = order.getOrderItems().get(0).getProduct().getName();
        if (order.getOrderItems().size() > 1) {
            orderName += " 외 " + (order.getOrderItems().size() - 1) + "건";
        }
        readyInfo.setOrderName(orderName);

        return readyInfo;
    }

    /**
     * PortOne과 최종 검증 후, 결제 완료 후처리를 진행합니다.
     * 재고 차감, 포인트 처리, 주문 상태 변경, 멤버십 업데이트를 포함합니다.
     *
     * @param paymentKey PortOne 결제 고유 키
     * @param currentUserId 현재 사용자 ID
     * @return 결제 완료 응답 DTO
     */
    @Transactional
    public PaymentResponseDto completePaymentVerification(String paymentKey , Long currentUserId) {

        Payment payment = getPaymentByPaymentKey(paymentKey);
        User user = getUserByPayment(payment);


        if (currentUserId != null) {
            if (!payment.getOrder().getUserId().equals(currentUserId)) {
                throw new SecurityException("해당 결제 건에 대한 권한이 없습니다.");
            }
        }


        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            return convertToPaymentResponseDto(payment, user, user.getMembershipRank().name(), user.getMembershipRank().name());
        }

        PaymentVerificationDto verification = portOneClient.getPayment(payment.getPaymentKey());

        if (!"Paid".equalsIgnoreCase(verification.getStatus())) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("결제 검증 실패 또는 금액 불일치.");
        }

        if (payment.getAmount().compareTo(verification.getAmount()) != 0) {
            cancelPaymentIfNecessary(payment.getPaymentKey(), "금액 위변조 감지");
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("결제 금액 불일치. 위변조 가능성.");
        }

        String prevRank = user.getMembershipRank().name();

        processPostPaymentActions(payment, payment.getOrder(), user);

        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return convertToPaymentResponseDto(payment, user, prevRank, user.getMembershipRank().name());
    }

    /**
     * 특정 결제 내역의 상세 정보를 조회합니다.
     *
     * @param paymentId 결제 ID
     * @param currentUserId 현재 사용자 ID
     * @return 결제 응답 DTO
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentDetails(Long paymentId, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId: " + paymentId));
        User user = getUserByPayment(payment);


        if (!payment.getOrder().getUserId().equals(currentUserId)) {
            throw new SecurityException("해당 결제 내역을 조회할 권한이 없습니다.");
        }

        return convertToPaymentResponseDto(payment, user, user.getMembershipRank().name(), user.getMembershipRank().name());
    }

    /**
     * 특정 결제를 실패 상태로 처리하고, 관련된 주문을 취소 상태로 변경합니다.
     *
     * @param paymentKey 결제 고유 키
     */
    @Transactional
    public void failPaymentByPaymentKey(String paymentKey) {
        Payment payment = getPaymentByPaymentKey(paymentKey);

        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new IllegalStateException("이미 PAID된 결제는 실패 처리할 수 없습니다.");
        }

        payment.setStatus(Payment.PaymentStatus.FAILED);

        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        paymentRepository.save(payment);
    }

    private void processPostPaymentActions(Payment payment, Order order, User user) {
        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) throw new IllegalStateException("재고 부족으로 결제 완료 불가");
            product.setStock(newStock);
            productRepository.save(product);
        }

        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().subtract(payment.getPointsUsed()));
            createPointTransaction(user, payment.getPointsUsed(), PointTransaction.PointType.USED);
        }
        BigDecimal earnedPoints = payment.getAmount().multiply(new BigDecimal("0.01"));
        if (earnedPoints.compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(earnedPoints));
            createPointTransaction(user, earnedPoints, PointTransaction.PointType.EARNED);
        }
        userRepository.save(user);

        updateMembershipLevel(user);
    }

    private User getUserByPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null) throw new IllegalArgumentException("Payment has no associated order");
        return userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));
    }

    @Transactional(readOnly = true)
    private Payment getPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));
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

    private void cancelPaymentIfNecessary(String paymentKey, String reason) {
        try {
            String accessToken = portOneClient.getAccessToken();
            portOneClient.cancelPayment(paymentKey, accessToken, reason);
        } catch (Exception e) {

            System.err.println("PortOne 취소 요청 실패 (결제 키: " + paymentKey + "): " + e.getMessage());
        }
    }


    private PaymentResponseDto convertToPaymentResponseDto(
            Payment payment,
            User user,
            String previousMembershipRank,
            String updatedMembershipRank) {

        PaymentResponseDto dto = new PaymentResponseDto();

        dto.setPaymentId(payment.getPaymentId() != null ? payment.getPaymentId().toString() : null);
        dto.setOrderId(payment.getOrder() != null ? payment.getOrder().getOrderId().toString() : null);
        dto.setUserId(user.getUserId());
        dto.setTotalAmount(payment.getAmount());
        dto.setStatus(payment.getStatus().name());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        dto.setUsePoint(payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0);
        dto.setUsedPointAmount(payment.getPointsUsed());
        dto.setEarnedPointAmount(payment.getStatus() == Payment.PaymentStatus.PAID ?
                payment.getAmount().multiply(new BigDecimal("0.01")) : BigDecimal.ZERO);
        dto.setFinalPointBalance(user.getTotalPoints());

        dto.setPreviousMembershipRank(previousMembershipRank);
        dto.setUpdatedMembershipRank(updatedMembershipRank);

        if (payment.getOrder() != null && payment.getOrder().getOrderItems() != null) {
            dto.setOrderItems(payment.getOrder().getOrderItems().stream()
                    .map(item -> {
                        PaymentResponseDto.OrderItemInfo info = new PaymentResponseDto.OrderItemInfo();
                        info.setProductId(item.getProduct().getProductId());
                        info.setQuantity(item.getQuantity());
                        info.setPrice(item.getPrice());
                        info.setProductName(item.getProduct().getName());
                        return info;
                    })
                    .collect(Collectors.toList()));
        } else {
            dto.setOrderItems(Collections.emptyList());
        }

        return dto;
    }
}