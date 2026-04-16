package com.innowise.internship.userservice.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.innowise.internship.userservice.dto.internal.InternalUserResponse;
import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.exception.user.InvalidUserStateException;
import com.innowise.internship.userservice.exception.user.UserAlreadyExistsException;
import com.innowise.internship.userservice.exception.user.UserNotFoundException;
import com.innowise.internship.userservice.mapper.UserMapper;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;
import com.innowise.internship.userservice.model.enums.UserStatus;
import com.innowise.internship.userservice.repository.PaymentCardRepository;
import com.innowise.internship.userservice.repository.UserRepository;
import com.innowise.internship.userservice.repository.specification.UserSpecification;
import com.innowise.internship.userservice.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users_pages", allEntries = true)
    }, put = {
            @CachePut(value = "users", key = "#result.id")
    })
    public UserResponse createUser(UserCreateRequest request) {
        String email = userMapper.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        return userMapper.toResponse(userRepository.save(userMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(UUID id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users_pages", key = "{#name, #surname, #pageable.pageNumber, #pageable.pageSize, #pageable.sort?.toString()}")
    public Page<UserResponse> getAllUsers(String name, String surname, Pageable pageable) {
        ListParams filter = normalizeListParams(name, surname);
        Specification<User> specification = UserSpecification.buildFilter(filter.nameParam(), filter.surnameParam());
        Page<User> users = userRepository.findAll(specification, pageable);
        return users.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users_pages", allEntries = true),
            @CacheEvict(value = "internal_users", key = "#id")
    }, put = {
            @CachePut(value = "users", key = "#id")
    })
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user = findUserOrThrow(id);
        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users_pages", allEntries = true),
            @CacheEvict(value = "internal_users", key = "#id"),
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = {"cards", "cards_pages", "cards_user"}, allEntries = true)
    })
    public void deleteUser(UUID id) {
        User user = findUserOrThrow(id);
        paymentCardRepository.updateStatusByUserId(id, PaymentCardStatus.DELETED, PaymentCardStatus.ACTIVE);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users_pages", allEntries = true),
            @CacheEvict(value = "internal_users", key = "#id"),
            @CacheEvict(value = "cards_user", key = "#id"),
            @CacheEvict(value = "cards_pages", allEntries = true)
    }, put = {
            @CachePut(value = "users", key = "#id")
    })
    public UserResponse restoreUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        if (user.getStatus() != UserStatus.DELETED) {
            throw new InvalidUserStateException("User is not deleted: " + id);
        }
        paymentCardRepository.updateStatusByUserId(id, PaymentCardStatus.ACTIVE, PaymentCardStatus.DELETED);
        user.setStatus(UserStatus.ACTIVE);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findIdByEmail(String email) {
        return userRepository.findIdByEmailAndStatus(userMapper.normalizeEmail(email), UserStatus.ACTIVE)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "internal_users", key = "#id")
    public InternalUserResponse getInternalUserById(UUID id) {
        User user = findUserOrThrow(id);
        return userMapper.toInternalResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserResponse> getInternalUsersByIds(List<UUID> ids) {
        List<UUID> distinctIds = normalizeInternalUserIds(ids);
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByIdInAndStatus(distinctIds, UserStatus.ACTIVE).stream()
                .map(userMapper::toInternalResponse)
                .toList();
    }

    private List<UUID> normalizeInternalUserIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> distinct = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return distinct.isEmpty() ? List.of() : distinct;
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    private ListParams normalizeListParams(String name, String surname) {
        String nameParam = (name != null && !name.trim().isEmpty()) ? name.trim() : null;
        String surnameParam = (surname != null && !surname.trim().isEmpty()) ? surname.trim() : null;
        return new ListParams(nameParam, surnameParam);
    }

    private record ListParams(String nameParam, String surnameParam) {}
}
