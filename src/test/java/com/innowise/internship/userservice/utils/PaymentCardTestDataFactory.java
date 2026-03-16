package com.innowise.internship.userservice.utils;

import java.time.LocalDate;
import java.util.UUID;

import lombok.experimental.UtilityClass;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.model.PaymentCard;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;

@UtilityClass
public class PaymentCardTestDataFactory {

    public final String DEFAULT_NUMBER = "1234567890123456";
    public final String DEFAULT_HOLDER = "John Doe";
    public final LocalDate DEFAULT_EXPIRATION_DATE = LocalDate.now().plusYears(1);

    public PaymentCardCreateRequest buildPaymentCardCreateRequest() {
        return new PaymentCardCreateRequest(DEFAULT_NUMBER, DEFAULT_HOLDER, DEFAULT_EXPIRATION_DATE);
    }

    public PaymentCardCreateRequest buildPaymentCardCreateRequest(String number) {
        return new PaymentCardCreateRequest(number, DEFAULT_HOLDER, DEFAULT_EXPIRATION_DATE);
    }

    public static String uniqueCardNumber() {
        return "411111111111" + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000));
    }

    public PaymentCardUpdateRequest buildPaymentCardUpdateRequest() {
        return new PaymentCardUpdateRequest(DEFAULT_HOLDER, DEFAULT_EXPIRATION_DATE);
    }

    public PaymentCard buildPaymentCard(User user) {
        return buildPaymentCard(UUID.randomUUID(), user);
    }

    public PaymentCard buildPaymentCard(UUID cardId, User user) {
        PaymentCard card = new PaymentCard();
        card.setId(cardId);
        card.setUser(user);
        card.setNumber(DEFAULT_NUMBER);
        card.setHolder(DEFAULT_HOLDER);
        card.setExpirationDate(DEFAULT_EXPIRATION_DATE);
        card.setStatus(PaymentCardStatus.ACTIVE);
        return card;
    }

    public PaymentCardResponse buildPaymentCardResponse(PaymentCard card) {
        return new PaymentCardResponse(
                card.getId(),
                card.getUser().getId(),
                card.getNumber(),
                card.getHolder(),
                card.getExpirationDate(),
                card.getStatus(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    public PaymentCardResponse buildPaymentCardResponse(UUID cardId, UUID userId) {
        return new PaymentCardResponse(
                cardId,
                userId,
                DEFAULT_NUMBER,
                DEFAULT_HOLDER,
                DEFAULT_EXPIRATION_DATE,
                PaymentCardStatus.ACTIVE,
                null,
                null
        );
    }
}
