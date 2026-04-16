package com.innowise.internship.userservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.innowise.internship.userservice.dto.internal.InternalUserResponse;
import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.UserResponse;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserById(UUID id);

    Page<UserResponse> getAllUsers(String name, String surname, Pageable pageable);

    UserResponse updateUser(UUID id, UserUpdateRequest request);

    void deleteUser(UUID id);

    UserResponse restoreUser(UUID id);

    UUID findIdByEmail(String email);

    InternalUserResponse getInternalUserById(UUID id);

    List<InternalUserResponse> getInternalUsersByIds(List<UUID> ids);
}
