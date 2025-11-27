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

/**
 * 환불 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 외부 PG사 연동 및 내부 시스템의 상태 변화(재고, 포인트, 멤버십)를 관리합니다.
 */
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


    /**
     * 사용자 요청에 따라 PG사에 환불을 요청하고 내부 시스템의 환불 후처리를 진행합니다.
     * 결제 건의 소유권과 상태(PAID)를 검증합니다.
     */
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
            portOneClient.cancelPayment(payment.getPaymentKey(), accessToken, requestDto.getReason());
        } catch (Exception e) {
            // PG사 요청 실패 시 예외 발생, 트랜잭션 롤백 유도
            throw new RuntimeException("PortOne 환불 요청 실패: " + e.getMessage());
        }

        // 내부 시스템 후처리
        Refund savedRefund = processRefundLogic(payment, requestDto.getReason());

        return convertToRefundResponseDto(savedRefund);
    }

    /**
     * PG사 웹훅(Webhook)으로부터 환불 완료 통보를 받아 내부 시스템 환불 후처리를 진행합니다.
     * PG사 요청이 성공했음을 가정하고 내부 로직만 수행합니다.
     */
    @Transactional
    public void processWebhookRefund(String paymentKey, String reason) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));

        processRefundLogic(payment, reason);
    }


    /**
     * 실제 환불 처리 로직을 수행합니다.
     * Payment, Order 상태 변경, 재고 복구, 포인트 복구/회수, 멤버십 업데이트를 포함합니다.
     */
    private Refund processRefundLogic(Payment payment, String reason) {

        if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            Optional<Refund> existingRefund = refundRepository.findByPaymentId(payment.getPaymentId()).stream().findFirst();
            return existingRefund
                    .orElseThrow(() -> new IllegalStateException("Payment already refunded but refund record missing."));
        }

        // Refund 기록 생성
        Refund refund = new Refund();
        refund.setPaymentId(payment.getPaymentId());
        refund.setAmount(payment.getAmount());
        refund.setReason(reason);
        refund.setStatus(Refund.RefundStatus.COMPLETED);
        Refund savedRefund = refundRepository.save(refund);

        // Payment 및 Order 상태 변경
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        User user = getUserByPayment(payment);

        // 사용 포인트 복구 (잔액 증가)
        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(payment.getPointsUsed()));
            createPointTransaction(user, payment.getPointsUsed(), PointTransaction.PointType.EARNED);
        }

        // 적립 포인트 회수 (잔액 감소)
        BigDecimal earnedPoints = payment.getAmount().multiply(new BigDecimal("0.01"));
        if (earnedPoints.compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().subtract(earnedPoints));
            createPointTransaction(user, earnedPoints, PointTransaction.PointType.USED);
        }

        // 멤버십 업데이트 (환불로 인해 누적 결제액 감소)
        userRepository.save(user);
        updateMembershipLevel(user);

        return savedRefund;
    }


    /**
     * 결제 엔티티를 통해 해당 결제의 사용자 엔티티를 조회합니다.
     */
    private User getUserByPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null) throw new IllegalArgumentException("Payment has no associated order");
        return userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));
    }

    /**
     * 포인트 거래 내역을 생성하고 저장합니다.
     */
    private void createPointTransaction(User user, BigDecimal amount, PointTransaction.PointType type) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setPointsChanged(amount);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());
        pointTransactionRepository.save(transaction);
    }

    /**
     * 사용자의 총 결제 금액을 재계산하여 멤버십 등급을 업데이트합니다.
     */
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


    /**
     * Refund 엔티티를 응답 DTO로 변환합니다.
     */
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