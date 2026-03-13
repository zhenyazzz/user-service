package com.innowise.internship.userservice.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.UserResponse;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserById(UUID id);

    Page<UserResponse> getAllUsers(String name, String surname, Boolean active, Pageable pageable);

    UserResponse updateUser(UUID id, UserUpdateRequest request);

    UserResponse activateUser(UUID id);

    UserResponse deactivateUser(UUID id);

    UUID findIdByEmail(String email);
}
