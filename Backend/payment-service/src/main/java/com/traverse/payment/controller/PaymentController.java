package com.traverse.payment.controller;

import com.traverse.payment.dto.ChargeRequest;
import com.traverse.payment.dto.PaymentResponse;
import com.traverse.payment.entity.Payment;
import com.traverse.payment.entity.PaymentStatus;
import com.traverse.payment.security.AuthenticatedUser;
import com.traverse.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments/charges")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Charge the caller's own saved payment method for a travel booking. */
    @PostMapping
    public ResponseEntity<PaymentResponse> charge(@Valid @RequestBody ChargeRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser principal) {
        Payment payment = paymentService.charge(principal.id(), request);
        HttpStatus status = payment.getStatus() == PaymentStatus.FAILED
                ? HttpStatus.PAYMENT_REQUIRED
                : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(toResponse(payment));
    }

    /** The caller's own payment history. */
    @GetMapping("/mine")
    public List<PaymentResponse> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return paymentService.history(principal.id()).stream().map(this::toResponse).toList();
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getUserId(), p.getTravelId(), p.getPaymentMethodId(),
                p.getProvider(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getExternalChargeId(),
                p.getCreatedAt());
    }
}
