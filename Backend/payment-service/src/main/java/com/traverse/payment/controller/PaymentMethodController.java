package com.traverse.payment.controller;

import com.traverse.payment.dto.CreatePaymentMethodRequest;
import com.traverse.payment.dto.PaymentMethodResponse;
import com.traverse.payment.entity.PaymentMethod;
import com.traverse.payment.entity.Role;
import com.traverse.payment.exception.PaymentMethodNotFoundException;
import com.traverse.payment.security.AuthenticatedUser;
import com.traverse.payment.service.PaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody CreatePaymentMethodRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        PaymentMethod paymentMethod = paymentMethodService.create(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(paymentMethod));
    }

    /**
     * Travelers/managers only ever see their own saved methods; an admin may
     * list everything (oversight) or a specific user's via {@code ?userId}.
     */
    @GetMapping
    public List<PaymentMethodResponse> findAll(@RequestParam(required = false) Long userId,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        List<PaymentMethod> paymentMethods;
        if (principal.role() == Role.ADMIN) {
            paymentMethods = userId == null ? paymentMethodService.findAll() : paymentMethodService.findByUserId(userId);
        } else {
            paymentMethods = paymentMethodService.findByUserId(principal.id());
        }
        return paymentMethods.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PaymentMethodResponse findById(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return toResponse(requireOwned(id, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        requireOwned(id, principal);
        paymentMethodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Loads a payment method, enforcing that the caller owns it (or is an
     * admin). A cross-user access is reported as "not found" rather than
     * "forbidden" so the endpoint doesn't leak which ids exist.
     */
    private PaymentMethod requireOwned(Long id, AuthenticatedUser principal) {
        PaymentMethod method = paymentMethodService.findById(id);
        if (principal.role() != Role.ADMIN && !method.getUserId().equals(principal.id())) {
            throw new PaymentMethodNotFoundException(id);
        }
        return method;
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.getId(), paymentMethod.getUserId(), paymentMethod.getProvider(),
                paymentMethod.getBrand(), paymentMethod.getLast4(), paymentMethod.getExpiryMonth(),
                paymentMethod.getExpiryYear(), paymentMethod.getPayerEmail(), paymentMethod.isDefault(),
                paymentMethod.getCreatedAt());
    }
}
