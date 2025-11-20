package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.repository.OrderRepository;
import com.sparta.payment_system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentEntityController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public PaymentEntityController(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        try {
            // Order 확인
            if (payment.getOrder() == null || payment.getOrder().getOrderId() == null) {
                return ResponseEntity.badRequest().build();
            }

            Optional<Order> orderOpt = orderRepository.findById(Long.valueOf(payment.getOrder().getOrderId()));
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            payment.setOrder(orderOpt.get());

            Payment savedPayment = paymentRepository.save(payment);
            return ResponseEntity.ok(savedPayment);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        try {
            List<Payment> payments = paymentRepository.findAll();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        try {
            Optional<Payment> payment = paymentRepository.findById(id);
            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(@PathVariable Long id, @RequestBody Payment paymentDetails) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(id);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Payment payment = paymentOpt.get();

            if (paymentDetails.getOrder() != null && paymentDetails.getOrder().getOrderId() != null) {
                Optional<Order> orderOpt = orderRepository.findById(Long.valueOf(payment.getOrder().getOrderId()));
                orderOpt.ifPresent(payment::setOrder);
            }

            payment.setMethodId(paymentDetails.getMethodId());
            payment.setPaymentKey(paymentDetails.getPaymentKey());
            payment.setAmount(paymentDetails.getAmount());
            payment.setPointsUsed(paymentDetails.getPointsUsed());
            payment.setDiscountAmount(paymentDetails.getDiscountAmount());
            payment.setStatus(paymentDetails.getStatus());
            payment.setPaidAt(paymentDetails.getPaidAt());

            Payment updatedPayment = paymentRepository.save(payment);
            return ResponseEntity.ok(updatedPayment);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        try {
            if (paymentRepository.existsById(id)) {
                paymentRepository.deleteById(id);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(@PathVariable Long orderId) {
        try {
            Optional<Payment> payment = paymentRepository.findByOrder_OrderId(orderId);
            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/payment-key/{paymentKey}")
    public ResponseEntity<Payment> getPaymentByPaymentKey(@PathVariable String paymentKey) {
        try {
            Optional<Payment> payment = paymentRepository.findByPaymentKey(paymentKey);
            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable Payment.PaymentStatus status) {
        try {
            List<Payment> payments = paymentRepository.findByStatus(status);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/method/{methodId}")
    public ResponseEntity<List<Payment>> getPaymentsByMethod(@PathVariable Long methodId) {
        try {
            List<Payment> payments = paymentRepository.findByMethodId(methodId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

