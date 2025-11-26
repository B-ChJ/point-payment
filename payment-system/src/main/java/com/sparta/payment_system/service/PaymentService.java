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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PortOneClient portOneClient;


    // 1. 결제 생성 (Ready API)
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

        // 1. 현재 사용자가 주문의 소유자인지 확인
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("해당 주문에 대한 결제 권한이 없습니다.");
        }

        // 2. 재고 사전 확인
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct().getStock() < item.getQuantity()) {
                throw new IllegalStateException("재고 부족: " + item.getProduct().getName());
            }
        }

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal pointsUsed = BigDecimal.ZERO;
        BigDecimal finalPaymentAmount = totalAmount;

        // 3. 포인트 사용 로직
        if (requestDto.isUsePoint()) {
            // 포인트 사용 검증 로직은 기존과 동일
            pointsUsed = user.getTotalPoints().min(totalAmount);
            finalPaymentAmount = totalAmount.subtract(pointsUsed);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
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

        // 주문 이름 설정
        String orderName = order.getOrderItems().get(0).getProduct().getName();
        if (order.getOrderItems().size() > 1) {
            orderName += " 외 " + (order.getOrderItems().size() - 1) + "건";
        }
        readyInfo.setOrderName(orderName);

        return readyInfo;
    }

    // 2. 결제 완료 처리
    @Transactional
    public PaymentResponseDto completePaymentVerification(String paymentKey , Long currentUserId) { // 🌟 메서드 이름 변경 및 userId 사용

        Payment payment = getPaymentByPaymentKey(paymentKey);
        User user = getUserByPayment(payment);


        if (currentUserId != null && !payment.getOrder().getUserId().equals(currentUserId)) {
            throw new SecurityException("해당 결제 건에 대한 권한이 없습니다.");
        }

        // 이미 완료된 경우, 불필요한 재처리 방지
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            return convertToPaymentResponseDto(payment, user, user.getMembershipRank().name(), user.getMembershipRank().name());
        }

        // PortOne 결제 검증 (금액 일치 확인)
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

        // 내부 상태 업데이트 및 후처리
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 이전 랭크 정보가 없으면, 현재 랭크로 설정하여 반환
        return convertToPaymentResponseDto(payment, user, prevRank, user.getMembershipRank().name());
    }

    // 3. 결제 내역 조회
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentDetails(Long paymentId, Long currentUserId) { // 🌟 메서드 이름 변경 및 userId 추가
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId: " + paymentId));
        User user = getUserByPayment(payment);


        if (!payment.getOrder().getUserId().equals(currentUserId)) {
            throw new SecurityException("해당 결제 내역을 조회할 권한이 없습니다.");
        }

        return convertToPaymentResponseDto(payment, user, user.getMembershipRank().name(), user.getMembershipRank().name());
    }

    //결제실패 처리 (기존 로직 유지)
    @Transactional
    public void failPaymentByPaymentKey(String paymentKey) {
        Payment payment = getPaymentByPaymentKey(paymentKey);

        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new IllegalStateException("이미 PAID된 결제는 실패 처리할 수 없습니다.");
        }

        payment.setStatus(Payment.PaymentStatus.FAILED);

        // 실패 시 주문 상태 동기화
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        paymentRepository.save(payment);
    }

    private void processPostPaymentActions(Payment payment, Order order, User user) {
        // 1. 주문 상태 동기화 및 재고 실차감 (기존 로직 유지)
        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) throw new IllegalStateException("재고 부족으로 결제 완료 불가");
            product.setStock(newStock);
            productRepository.save(product);
        }

        // 2. 포인트 사용 차감 및 적립
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

        // 3. 멤버십 업데이트
        updateMembershipLevel(user);
    }


    private User getUserByPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null) throw new IllegalArgumentException("Payment has no associated order");
        // 🌟 userId를 통해 사용자 조회
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

        // 1. 기본 정보
        dto.setPaymentId(payment.getPaymentId() != null ? payment.getPaymentId().toString() : null);
        dto.setOrderId(payment.getOrder() != null ? payment.getOrder().getOrderId().toString() : null);
        dto.setUserId(user.getUserId());
        dto.setTotalAmount(payment.getAmount());
        dto.setStatus(payment.getStatus().name());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        // 2. 포인트 정보
        dto.setUsePoint(payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0);
        dto.setUsedPointAmount(payment.getPointsUsed());
        // 적립 포인트는 완료 단계에서만 계산 가능 (생성 단계라면 0)
        dto.setEarnedPointAmount(payment.getStatus() == Payment.PaymentStatus.PAID ?
                payment.getAmount().multiply(new BigDecimal("0.01")) : BigDecimal.ZERO);
        dto.setFinalPointBalance(user.getTotalPoints());

        // 3. 멤버십 정보
        dto.setPreviousMembershipRank(previousMembershipRank);
        dto.setUpdatedMembershipRank(updatedMembershipRank);

        // 4. 주문 상품 정보 매핑
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