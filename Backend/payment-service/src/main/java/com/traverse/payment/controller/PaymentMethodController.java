package com.traverse.payment.controller;

import com.traverse.payment.dto.CreatePaymentMethodRequest;
import com.traverse.payment.dto.PaymentMethodResponse;
import com.traverse.payment.entity.PaymentMethod;
import com.traverse.payment.service.PaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody CreatePaymentMethodRequest request) {
        PaymentMethod paymentMethod = paymentMethodService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(paymentMethod));
    }

    @GetMapping
    public List<PaymentMethodResponse> findAll(@RequestParam(required = false) Long userId) {
        List<PaymentMethod> paymentMethods = userId == null
                ? paymentMethodService.findAll()
                : paymentMethodService.findByUserId(userId);
        return paymentMethods.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PaymentMethodResponse findById(@PathVariable Long id) {
        return toResponse(paymentMethodService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.getId(), paymentMethod.getUserId(), paymentMethod.getProvider(),
                paymentMethod.getBrand(), paymentMethod.getLast4(), paymentMethod.getExpiryMonth(),
                paymentMethod.getExpiryYear(), paymentMethod.getPayerEmail(), paymentMethod.isDefault(),
                paymentMethod.getCreatedAt());
    }
}
