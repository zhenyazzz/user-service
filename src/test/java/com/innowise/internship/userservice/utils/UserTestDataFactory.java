package com.innowise.internship.userservice.utils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.experimental.UtilityClass;

import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.internal.InternalUserResponse;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.UserStatus;

@UtilityClass
public class UserTestDataFactory {

    public final String DEFAULT_NAME = "John";
    public final String DEFAULT_SURNAME = "Doe";
    public final String NORMALIZED_NAME = "john";
    public final String NORMALIZED_SURNAME = "doe";
    public final String DEFAULT_EMAIL = "john@example.com";
    public final LocalDate DEFAULT_BIRTH_DATE = null;
    public final String WRONG_EMAIL = "wrong@example.com";
    public final String NORMALIZED_EMAIL_WHITESPACE = "  John@Example.COM  ";

    public UserCreateRequest buildUserCreateRequest() {
        return buildUserCreateRequest(UUID.randomUUID(), DEFAULT_EMAIL);
    }

    public UserCreateRequest buildUserCreateRequest(String email) {
        return buildUserCreateRequest(UUID.randomUUID(), email);
    }

    public UserCreateRequest buildUserCreateRequest(UUID userId, String email) {
        return new UserCreateRequest(userId, DEFAULT_NAME, DEFAULT_SURNAME, DEFAULT_BIRTH_DATE, email);
    }

    public UserUpdateRequest buildUserUpdateRequest() {
        return new UserUpdateRequest(DEFAULT_NAME, DEFAULT_SURNAME, DEFAULT_BIRTH_DATE);
    }

    public User buildUser() {
        return buildUser(UUID.randomUUID());
    }

    public User buildUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setName(DEFAULT_NAME);
        user.setSurname(DEFAULT_SURNAME);
        user.setBirthDate(DEFAULT_BIRTH_DATE);
        user.setEmail(DEFAULT_EMAIL);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public UserResponse buildUserResponse(UUID id) {
        return new UserResponse(
                id,
                DEFAULT_NAME,
                DEFAULT_SURNAME,
                DEFAULT_BIRTH_DATE,
                DEFAULT_EMAIL,
                UserStatus.ACTIVE,
                null,
                null,
                List.of()
        );
    }

    public UserResponse buildUserResponse() {
        return buildUserResponse(UUID.randomUUID());
    }

    public UserResponse buildUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getBirthDate(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                List.of()
        );
    }

    public InternalUserResponse buildInternalUserResponse(UUID id) {
        return new InternalUserResponse(id, DEFAULT_NAME, DEFAULT_SURNAME, DEFAULT_BIRTH_DATE, DEFAULT_EMAIL);
    }
}
