package com.innowise.internship.userservice.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.exception.card.CardLimitExceededException;
import com.innowise.internship.userservice.exception.card.PaymentCardAlreadyExistsException;
import com.innowise.internship.userservice.exception.card.PaymentCardNotFoundException;
import com.innowise.internship.userservice.exception.user.UserNotFoundException;
import com.innowise.internship.userservice.mapper.PaymentCardMapper;
import com.innowise.internship.userservice.model.PaymentCard;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.repository.PaymentCardRepository;
import com.innowise.internship.userservice.repository.UserRepository;
import com.innowise.internship.userservice.config.CardProperties;
import com.innowise.internship.userservice.repository.specification.PaymentCardSpecification;
import com.innowise.internship.userservice.service.PaymentCardService;

import lombok.RequiredArgsConstructor;

@Service("paymentCardService")
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {
    private final PaymentCardRepository paymentCardRepository;
    private final PaymentCardMapper paymentCardMapper;
    private final UserRepository userRepository;
    private final CardProperties cardProperties;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cards_pages", allEntries = true),
        @CacheEvict(value = "cards_user", key = "#userId")
    })
    @CachePut(value = "cards", key = "#result.id")
    public PaymentCardResponse createCard(UUID userId, PaymentCardCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (paymentCardRepository.existsByNumber(request.number())) {
            throw new PaymentCardAlreadyExistsException("Card with number already exists");
        }

        long cardCount = paymentCardRepository.countByUserId(userId);
        if (cardCount >= cardProperties.getMaxPerUser()) {
            throw new CardLimitExceededException(userId, cardProperties.getMaxPerUser());
        }

        PaymentCard card = paymentCardMapper.toEntity(request, user);

        return paymentCardMapper.toResponse(paymentCardRepository.save(card));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#id")
    public PaymentCardResponse getCardById(UUID id) {
        PaymentCard card = findPaymentCardOrThrow(id);
        return paymentCardMapper.toResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cards_pages", key = "{#active, #pageable.pageNumber, #pageable.pageSize, #pageable.sort?.toString()}")
    public Page<PaymentCardResponse> getAllCards(Boolean active, Pageable pageable) {
        var spec = PaymentCardSpecification.filterByActive(active);
        Page<PaymentCard> cards = paymentCardRepository.findAll(spec, pageable);
        return cards.map(paymentCardMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cards_user", key = "#userId")
    public List<PaymentCardResponse> getCardsByUserId(UUID userId) {
        List<PaymentCard> cards = paymentCardRepository.findAllByUserIdWithUser(userId);
        return paymentCardMapper.toResponseList(cards);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cards_pages", allEntries = true),
        @CacheEvict(value = "cards_user", key = "#result.userId")
    }, put = {
        @CachePut(value = "cards", key = "#id")
    })
    public PaymentCardResponse updateCard(UUID id, PaymentCardUpdateRequest request) {
        PaymentCard card = findPaymentCardOrThrow(id);
        paymentCardMapper.updateEntity(request, card);
        return paymentCardMapper.toResponse(paymentCardRepository.save(card));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cards_pages", allEntries = true),
        @CacheEvict(value = "cards_user", key = "#result.userId")
    }, put = {
        @CachePut(value = "cards", key = "#result.id")
    })
    public PaymentCardResponse activateCard(UUID id) {
        PaymentCard card = findPaymentCardOrThrow(id);
        card.setActive(true);
        return paymentCardMapper.toResponse(paymentCardRepository.save(card));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cards_pages", allEntries = true),
        @CacheEvict(value = "cards_user", key = "#result.userId")
    }, put = {
        @CachePut(value = "cards", key = "#result.id")
    })
    public PaymentCardResponse deactivateCard(UUID id) {
        PaymentCard card = findPaymentCardOrThrow(id);
        card.setActive(false);
        return paymentCardMapper.toResponse(paymentCardRepository.save(card));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCardOwner(UUID userId, UUID cardId) {
        return paymentCardRepository.findIdByIdAndUser_Id(cardId, userId).isPresent();
    }

    private PaymentCard findPaymentCardOrThrow(UUID id) {
        return paymentCardRepository.findByIdWithUser(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
    }
}
