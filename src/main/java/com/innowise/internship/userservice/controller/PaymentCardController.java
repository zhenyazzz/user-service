package com.innowise.internship.userservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.security.SecurityUtils;
import com.innowise.internship.userservice.service.PaymentCardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<PaymentCardResponse> create(@Valid @RequestBody PaymentCardCreateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PaymentCardResponse response = paymentCardService.createCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.userId() == #userId")
    public ResponseEntity<List<PaymentCardResponse>> getCardsByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentCardResponse>> getAll(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentCardService.getAllCards(active, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @paymentCardService.isCardOwner(authentication.principal.userId(), #id)")
    public ResponseEntity<PaymentCardResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentCardService.getCardById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @paymentCardService.isCardOwner(authentication.principal.userId(), #id)")
    public ResponseEntity<PaymentCardResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentCardUpdateRequest request) {
        return ResponseEntity.ok(paymentCardService.updateCard(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @paymentCardService.isCardOwner(authentication.principal.userId(), #id)")
    public ResponseEntity<PaymentCardResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentCardService.activateCard(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or @paymentCardService.isCardOwner(authentication.principal.userId(), #id)")
    public ResponseEntity<PaymentCardResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentCardService.deactivateCard(id));
    }
}
