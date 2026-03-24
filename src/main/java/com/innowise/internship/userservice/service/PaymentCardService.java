package com.innowise.internship.userservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;

public interface PaymentCardService {
    PaymentCardResponse createCard(UUID userId, PaymentCardCreateRequest request);

    PaymentCardResponse getCardById(UUID id);

    Page<PaymentCardResponse> getAllCards(Pageable pageable);

    List<PaymentCardResponse> getCardsByUserId(UUID userId);

    PaymentCardResponse updateCard(UUID id, PaymentCardUpdateRequest request);

    PaymentCardResponse deleteCard(UUID id);

    boolean isCardOwner(UUID userId, UUID cardId);
}
