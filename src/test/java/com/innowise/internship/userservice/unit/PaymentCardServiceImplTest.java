package com.innowise.internship.userservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.exception.card.CardLimitExceededException;
import com.innowise.internship.userservice.exception.card.PaymentCardAlreadyExistsException;
import com.innowise.internship.userservice.exception.card.PaymentCardNotFoundException;
import com.innowise.internship.userservice.exception.user.UserNotFoundException;
import com.innowise.internship.userservice.repository.PaymentCardRepository;
import com.innowise.internship.userservice.mapper.PaymentCardMapper;
import com.innowise.internship.userservice.model.PaymentCard;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;
import com.innowise.internship.userservice.model.enums.UserStatus;
import com.innowise.internship.userservice.repository.UserRepository;
import com.innowise.internship.userservice.config.CardProperties;
import com.innowise.internship.userservice.service.impl.PaymentCardServiceImpl;
import com.innowise.internship.userservice.utils.PaymentCardTestDataFactory;
import com.innowise.internship.userservice.utils.UserTestDataFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCardServiceImpl unit tests")
class PaymentCardServiceImplTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardProperties cardProperties;

    @InjectMocks
    private PaymentCardServiceImpl paymentCardService;


    @Test
    @DisplayName("createCard when user exists creates and returns response")
    void createCard_Success() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);
        PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest();
        PaymentCardResponse response = PaymentCardTestDataFactory.buildPaymentCardResponse(card.getId(), user.getId());

        when(userRepository.findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(paymentCardRepository.existsByNumber(request.number())).thenReturn(false);
        when(paymentCardRepository.countByUserIdAndStatus(user.getId(), PaymentCardStatus.ACTIVE)).thenReturn(0L);
        when(cardProperties.getMaxPerUser()).thenReturn(5);
        when(paymentCardMapper.toEntity(request, user)).thenReturn(card);
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        PaymentCardResponse result = paymentCardService.createCard(user.getId(), request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE);
        verify(paymentCardRepository).existsByNumber(request.number());
        verify(paymentCardRepository).countByUserIdAndStatus(user.getId(), PaymentCardStatus.ACTIVE);
        verify(cardProperties).getMaxPerUser();
        verify(paymentCardMapper).toEntity(request, user);
        verify(paymentCardRepository).save(card);
        verify(paymentCardMapper).toResponse(card);
    }

    @Test
    @DisplayName("createCard when user not exists throws UserNotFoundException")
    void createCard_whenUserNotExists_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest();

        when(userRepository.findByIdAndStatusForUpdate(userId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.createCard(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + userId);

        verify(userRepository).findByIdAndStatusForUpdate(userId, UserStatus.ACTIVE);
        verify(paymentCardRepository, never()).existsByNumber(request.number());
        verify(paymentCardRepository, never()).countByUserIdAndStatus(userId, PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper, never()).toEntity(eq(request), any());
        verify(paymentCardRepository, never()).save(any());
        verify(paymentCardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("createCard when card number already exists throws PaymentCardAlreadyExistsException")
    void createCard_whenCardNumberAlreadyExists_throwsPaymentCardAlreadyExistsException() {
        User user = UserTestDataFactory.buildUser();
        PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest();
        when(userRepository.findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(paymentCardRepository.existsByNumber(request.number())).thenReturn(true);

        assertThatThrownBy(() -> paymentCardService.createCard(user.getId(), request))
                .isInstanceOf(PaymentCardAlreadyExistsException.class)
                .hasMessage("Card with number already exists");

        verify(userRepository).findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE);
        verify(paymentCardRepository).existsByNumber(request.number());
        verify(paymentCardRepository, never()).countByUserIdAndStatus(user.getId(), PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper, never()).toEntity(request, user);
        verify(paymentCardRepository, never()).save(any());
        verify(paymentCardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("createCard when user has 5 cards creating 6th throws CardLimitExceededException")
    void createCard_whenCardLimitExceeded_throwsCardLimitExceededException() {
        User user = UserTestDataFactory.buildUser();
        PaymentCardCreateRequest request = PaymentCardTestDataFactory.buildPaymentCardCreateRequest();

        when(userRepository.findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(paymentCardRepository.existsByNumber(request.number())).thenReturn(false);
        when(paymentCardRepository.countByUserIdAndStatus(user.getId(), PaymentCardStatus.ACTIVE)).thenReturn(5L);
        when(cardProperties.getMaxPerUser()).thenReturn(5);

        assertThatThrownBy(() -> paymentCardService.createCard(user.getId(), request))
                .isInstanceOf(CardLimitExceededException.class)
                .hasMessage("User " + user.getId() + " cannot have more than 5 cards");

        verify(userRepository).findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE);
        verify(paymentCardRepository).existsByNumber(request.number());
        verify(paymentCardRepository).countByUserIdAndStatus(user.getId(), PaymentCardStatus.ACTIVE);
        verify(cardProperties, times(2)).getMaxPerUser();
        verify(paymentCardMapper, never()).toEntity(request, user);
        verify(paymentCardRepository, never()).save(any());
        verify(paymentCardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getCardById when card exists returns response")
    void getCardById_whenCardExists_returnsResponse() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);
        PaymentCardResponse response = PaymentCardTestDataFactory.buildPaymentCardResponse(card);

        when(paymentCardRepository.findByIdWithUserAndStatus(card.getId(), PaymentCardStatus.ACTIVE)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        PaymentCardResponse result = paymentCardService.getCardById(card.getId());

        assertThat(result).isEqualTo(response);
        verify(paymentCardRepository).findByIdWithUserAndStatus(card.getId(), PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper).toResponse(card);
    }

    @Test
    @DisplayName("getCardById when card not exists throws PaymentCardNotFoundException")
    void getCardById_whenCardNotExists_throwsPaymentCardNotFoundException() {
        UUID cardId = UUID.randomUUID();

        when(paymentCardRepository.findByIdWithUserAndStatus(cardId, PaymentCardStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.getCardById(cardId))
                .isInstanceOf(PaymentCardNotFoundException.class)
                .hasMessage("Payment card not found with id: " + cardId);

        verify(paymentCardRepository).findByIdWithUserAndStatus(cardId, PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("isCardOwner when card belongs to user returns true")
    void isCardOwner_whenUserIsOwner_returnsTrue() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);

        when(paymentCardRepository.existsByIdAndUser_Id(card.getId(), user.getId())).thenReturn(true);

        boolean result = paymentCardService.isCardOwner(user.getId(), card.getId());

        assertThat(result).isTrue();
        verify(paymentCardRepository).existsByIdAndUser_Id(card.getId(), user.getId());
    }

    @Test
    @DisplayName("isCardOwner when card does not belong to user returns false")
    void isCardOwner_whenUserIsNotOwner_returnsFalse() {
        User owner = UserTestDataFactory.buildUser();
        User otherUser = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(owner);

        when(paymentCardRepository.existsByIdAndUser_Id(card.getId(), otherUser.getId())).thenReturn(false);

        boolean result = paymentCardService.isCardOwner(otherUser.getId(), card.getId());

        assertThat(result).isFalse();
        verify(paymentCardRepository).existsByIdAndUser_Id(card.getId(), otherUser.getId());
    }

    @Test
    @DisplayName("isCardOwner when card not exists returns false")
    void isCardOwner_whenCardNotExists_returnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        when(paymentCardRepository.existsByIdAndUser_Id(cardId, userId)).thenReturn(false);

        boolean result = paymentCardService.isCardOwner(userId, cardId);

        assertThat(result).isFalse();
        verify(paymentCardRepository).existsByIdAndUser_Id(cardId, userId);
    }

    @Test
    @DisplayName("getAllCards when cards exist returns response")
    void getAllCards_whenCardsExist_returnsResponse() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);
        PaymentCardResponse response = PaymentCardTestDataFactory.buildPaymentCardResponse(card);
        Pageable pageable = Pageable.ofSize(10);
        Page<PaymentCard> cardsPage = new PageImpl<>(List.of(card), pageable, 1);

        when(paymentCardRepository.findAllWithUser(eq(PaymentCardStatus.ACTIVE), eq(pageable))).thenReturn(cardsPage);
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        Page<PaymentCardResponse> result = paymentCardService.getAllCards(pageable);

        assertThat(result.getContent()).hasSize(1).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(paymentCardRepository).findAllWithUser(eq(PaymentCardStatus.ACTIVE), eq(pageable));
        verify(paymentCardMapper).toResponse(card);
    }
    
    @Test
    @DisplayName("getCardsByUserId when cards exist returns response")
    void getCardsByUserId_whenCardsExist_returnsResponse() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);
        PaymentCardResponse response = PaymentCardTestDataFactory.buildPaymentCardResponse(card);
        List<PaymentCard> cards = List.of(card);

        when(paymentCardRepository.findAllByUserIdAndStatusWithUser(user.getId(), PaymentCardStatus.ACTIVE)).thenReturn(cards);
        when(paymentCardMapper.toResponseList(cards)).thenReturn(List.of(response));

        List<PaymentCardResponse> result = paymentCardService.getCardsByUserId(user.getId());

        assertThat(result).hasSize(1).containsExactly(response);

        verify(paymentCardRepository).findAllByUserIdAndStatusWithUser(user.getId(), PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper).toResponseList(cards);
    }

    @Test
    @DisplayName("getCardsByUserId when cards do not exist returns empty list")
    void getCardsByUserId_whenCardsDoNotExist_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        List<PaymentCard> emptyList = List.of();

        when(paymentCardRepository.findAllByUserIdAndStatusWithUser(userId, PaymentCardStatus.ACTIVE)).thenReturn(emptyList);
        when(paymentCardMapper.toResponseList(emptyList)).thenReturn(List.of());

        List<PaymentCardResponse> result = paymentCardService.getCardsByUserId(userId);

        assertThat(result).isEmpty();
        verify(paymentCardRepository).findAllByUserIdAndStatusWithUser(userId, PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper).toResponseList(emptyList);
    }

    @Test
    @DisplayName("updateCard when card exists updates and returns response")
    void updateCard_whenCardExists_updatesAndReturnsResponse() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);
        PaymentCardUpdateRequest request = PaymentCardTestDataFactory.buildPaymentCardUpdateRequest();
        PaymentCardResponse response = PaymentCardTestDataFactory.buildPaymentCardResponse(card);

        when(paymentCardRepository.findByIdWithUserAndStatus(card.getId(), PaymentCardStatus.ACTIVE)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toResponse(card)).thenReturn(response);
        when(paymentCardRepository.save(card)).thenReturn(card);

        PaymentCardResponse result = paymentCardService.updateCard(card.getId(), request);

        assertThat(result).isEqualTo(response);
        verify(paymentCardRepository).findByIdWithUserAndStatus(card.getId(), PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper).updateEntity(request, card);
        verify(paymentCardRepository).save(card);
        verify(paymentCardMapper).toResponse(card);
    }

    @Test
    @DisplayName("updateCard when card not exists throws PaymentCardNotFoundException")
    void updateCard_whenCardNotExists_throwsPaymentCardNotFoundException() {
        UUID cardId = UUID.randomUUID();
        PaymentCardUpdateRequest request = PaymentCardTestDataFactory.buildPaymentCardUpdateRequest();

        when(paymentCardRepository.findByIdWithUserAndStatus(cardId, PaymentCardStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.updateCard(cardId, request))
                .isInstanceOf(PaymentCardNotFoundException.class)
                .hasMessage("Payment card not found with id: " + cardId);

        verify(paymentCardRepository).findByIdWithUserAndStatus(cardId, PaymentCardStatus.ACTIVE);
        verify(paymentCardMapper, never()).updateEntity(eq(request), any());
        verify(paymentCardRepository, never()).save(any());
        verify(paymentCardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("deleteCard (soft delete) when card exists sets status DELETED and returns response")
    void deleteCard_whenCardExists_setsStatusDeletedAndReturnsResponse() {
        User user = UserTestDataFactory.buildUser();
        PaymentCard card = PaymentCardTestDataFactory.buildPaymentCard(user);
        card.setStatus(PaymentCardStatus.ACTIVE);
        PaymentCardResponse response = PaymentCardTestDataFactory.buildPaymentCardResponse(card);

        when(paymentCardRepository.findByIdWithUserAndStatus(card.getId(), PaymentCardStatus.ACTIVE)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toResponse(card)).thenReturn(response);

        PaymentCardResponse result = paymentCardService.deleteCard(card.getId());

        assertThat(result).isEqualTo(response);
        assertThat(card.getStatus()).isEqualTo(PaymentCardStatus.DELETED);
        verify(paymentCardRepository).findByIdWithUserAndStatus(card.getId(), PaymentCardStatus.ACTIVE);
        verify(paymentCardRepository).save(card);
        verify(paymentCardMapper).toResponse(card);
    }

    @Test
    @DisplayName("deleteCard when card not exists throws PaymentCardNotFoundException")
    void deleteCard_whenCardNotExists_throwsPaymentCardNotFoundException() {
        UUID cardId = UUID.randomUUID();

        when(paymentCardRepository.findByIdWithUserAndStatus(cardId, PaymentCardStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.deleteCard(cardId))
                .isInstanceOf(PaymentCardNotFoundException.class)
                .hasMessage("Payment card not found with id: " + cardId);

        verify(paymentCardRepository).findByIdWithUserAndStatus(cardId, PaymentCardStatus.ACTIVE);
        verify(paymentCardRepository, never()).save(any());
    }

}
