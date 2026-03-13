package com.innowise.internship.userservice.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;

import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.exception.user.UserAlreadyExistsException;
import com.innowise.internship.userservice.exception.user.UserNotFoundException;
import com.innowise.internship.userservice.mapper.UserMapper;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.repository.UserRepository;
import com.innowise.internship.userservice.repository.specification.UserSpecification;
import com.innowise.internship.userservice.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    @CacheEvict(value = "users_pages", allEntries = true)
    @CachePut(value = "users", key = "#result.id")
    public UserResponse createUser(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findIdByEmail(email).ifPresent(id -> {
            throw new UserAlreadyExistsException(email);
        });

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
    @Cacheable(value = "users_pages", key = "{#name, #surname, #active, #pageable.pageNumber, #pageable.pageSize, #pageable.sort?.toString()}")
    public Page<UserResponse> getAllUsers(String name, String surname, Boolean active, Pageable pageable) {
        Page<User> users = userRepository.findAll(
            UserSpecification.filterByNameAndSurnameAndActive(name, surname, active), 
            pageable
        );
        return users.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users_pages", allEntries = true)
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
        @CacheEvict(value = "users", key = "#id")
    }, put = {
        @CachePut(value = "users", key = "#result.id")
    })
    public UserResponse activateUser(UUID id) {
        User user = findUserOrThrow(id);
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users_pages", allEntries = true),
        @CacheEvict(value = "users", key = "#id")
    }, put = {
        @CachePut(value = "users", key = "#result.id")
    })
    public UserResponse deactivateUser(UUID id) {
        User user = findUserOrThrow(id);
        user.setActive(false);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findIdByEmail(String email) {
        return userRepository.findIdByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }


}
