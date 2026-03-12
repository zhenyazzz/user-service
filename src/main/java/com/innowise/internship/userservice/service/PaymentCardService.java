package com.innowise.internship.userservice.service;

import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PaymentCardService {
    PaymentCardResponse createCard(UUID userId, PaymentCardCreateRequest request);

    PaymentCardResponse getCardById(UUID id);

    Page<PaymentCardResponse> getAllCards(Boolean active, Pageable pageable);

    List<PaymentCardResponse> getCardsByUserId(UUID userId);

    PaymentCardResponse updateCard(UUID id, PaymentCardUpdateRequest request);

    PaymentCardResponse activateCard(UUID id);

    PaymentCardResponse deactivateCard(UUID id);

    boolean isCardOwner(UUID userId, UUID cardId);
}
