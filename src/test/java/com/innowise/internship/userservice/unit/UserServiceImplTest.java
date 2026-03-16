package com.innowise.internship.userservice.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.data.jpa.domain.Specification;

import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.exception.user.UserAlreadyExistsException;
import com.innowise.internship.userservice.exception.user.UserNotFoundException;
import com.innowise.internship.userservice.mapper.UserMapper;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;
import com.innowise.internship.userservice.model.enums.UserStatus;
import com.innowise.internship.userservice.repository.PaymentCardRepository;
import com.innowise.internship.userservice.repository.UserRepository;
import com.innowise.internship.userservice.service.impl.UserServiceImpl;

import com.innowise.internship.userservice.utils.UserTestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl unit tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("createUser when email is free saves and returns response")
    void createUser_Success() {
        UserCreateRequest request = UserTestDataFactory.buildUserCreateRequest();
        User user = UserTestDataFactory.buildUser();
        UserResponse response = UserTestDataFactory.buildUserResponse(user.getId());

        when(userMapper.normalizeEmail(request.email())).thenReturn(UserTestDataFactory.DEFAULT_EMAIL);
        when(userRepository.existsByEmail(UserTestDataFactory.DEFAULT_EMAIL)).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertThat(result).isEqualTo(response);
        verify(userMapper).normalizeEmail(request.email());
        verify(userRepository).existsByEmail(UserTestDataFactory.DEFAULT_EMAIL);
        verify(userMapper).toEntity(request);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("createUser when email exists throws UserAlreadyExistsException")
    void createUser_whenEmailExists_throwsUserAlreadyExistsException() {
        UserCreateRequest request = UserTestDataFactory.buildUserCreateRequest();
        when(userMapper.normalizeEmail(request.email())).thenReturn(UserTestDataFactory.DEFAULT_EMAIL);
        when(userRepository.existsByEmail(UserTestDataFactory.DEFAULT_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(UserTestDataFactory.DEFAULT_EMAIL);

        verify(userMapper).normalizeEmail(request.email());
        verify(userRepository).existsByEmail(UserTestDataFactory.DEFAULT_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserById when user exists returns response")
    void getUserById_Success() {
        User user = UserTestDataFactory.buildUser();
        UserResponse response = UserTestDataFactory.buildUserResponse(user.getId());
        when(userRepository.findByIdAndStatus(user.getId(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserById(user.getId());

        assertThat(result).isEqualTo(response);
        verify(userRepository).findByIdAndStatus(user.getId(), UserStatus.ACTIVE);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("getUserById when user not exists throws UserNotFoundException")
    void getUserById_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findByIdAndStatus(id, UserStatus.ACTIVE);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getAllUsers when users exist returns page of responses")
    @SuppressWarnings("unchecked")
    void getAllUsers_Success() {
        User user = UserTestDataFactory.buildUser();
        UserResponse response = UserTestDataFactory.buildUserResponse(user.getId());

        Pageable pageable = Pageable.ofSize(10);
        Page<User> usersPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(usersPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAllUsers(null, null, pageable);

        assertThat(result.getContent()).hasSize(1).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("updateUser when user exists updates and returns response")
    void updateUser_Success() {
        User user = UserTestDataFactory.buildUser();
        UserUpdateRequest request = UserTestDataFactory.buildUserUpdateRequest();
        UserResponse response = UserTestDataFactory.buildUserResponse(user.getId());

        when(userRepository.findByIdAndStatus(user.getId(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUser(user.getId(), request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findByIdAndStatus(user.getId(), UserStatus.ACTIVE);
        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("updateUser when user not exists throws UserNotFoundException")
    void updateUser_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        UserUpdateRequest request = UserTestDataFactory.buildUserUpdateRequest();

        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(id, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findByIdAndStatus(id, UserStatus.ACTIVE);
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("deleteUser (soft delete) when user exists sets status DELETED")
    void deleteUser_whenUserExists_setsStatusDeleted() {
        User user = UserTestDataFactory.buildUser();
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByIdAndStatus(user.getId(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(paymentCardRepository.updateStatusByUserId(user.getId(), PaymentCardStatus.DELETED, PaymentCardStatus.ACTIVE)).thenReturn(0);
        when(userRepository.save(user)).thenReturn(user);

        userService.deleteUser(user.getId());

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).findByIdAndStatus(user.getId(), UserStatus.ACTIVE);
        verify(paymentCardRepository).updateStatusByUserId(user.getId(), PaymentCardStatus.DELETED, PaymentCardStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteUser when user not exists throws UserNotFoundException")
    void deleteUser_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findByIdAndStatus(id, UserStatus.ACTIVE);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findIdByEmail when email exists returns id")
    void findIdByEmail_Success() {
        String email = UserTestDataFactory.NORMALIZED_EMAIL_WHITESPACE;
        UUID id = UUID.randomUUID();

        when(userMapper.normalizeEmail(email)).thenReturn(UserTestDataFactory.DEFAULT_EMAIL);
        when(userRepository.findIdByEmailAndStatus(UserTestDataFactory.DEFAULT_EMAIL, UserStatus.ACTIVE)).thenReturn(Optional.of(id));

        UUID result = userService.findIdByEmail(email);

        assertThat(result).isEqualTo(id);
        verify(userMapper).normalizeEmail(email);
        verify(userRepository).findIdByEmailAndStatus(UserTestDataFactory.DEFAULT_EMAIL, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("findIdByEmail when email not exists throws UserNotFoundException")
    void findIdByEmail_whenEmailNotExists_throwsUserNotFoundException() {
        String email = UserTestDataFactory.NORMALIZED_EMAIL_WHITESPACE;

        when(userMapper.normalizeEmail(email)).thenReturn(UserTestDataFactory.DEFAULT_EMAIL);
        when(userRepository.findIdByEmailAndStatus(UserTestDataFactory.DEFAULT_EMAIL, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findIdByEmail(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with email: " + email);

        verify(userMapper).normalizeEmail(email);
        verify(userRepository).findIdByEmailAndStatus(UserTestDataFactory.DEFAULT_EMAIL, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("findIdByEmail normalizes email (trim, lowerCase) before calling repository")
    void findIdByEmail_normalizesEmailBeforeLookup() {
        UUID id = UUID.randomUUID();
        when(userMapper.normalizeEmail(UserTestDataFactory.NORMALIZED_EMAIL_WHITESPACE))
                .thenReturn(UserTestDataFactory.DEFAULT_EMAIL);
        when(userRepository.findIdByEmailAndStatus(UserTestDataFactory.DEFAULT_EMAIL, UserStatus.ACTIVE)).thenReturn(Optional.of(id));

        UUID result = userService.findIdByEmail(UserTestDataFactory.NORMALIZED_EMAIL_WHITESPACE);

        assertThat(result).isEqualTo(id);
        verify(userMapper).normalizeEmail(UserTestDataFactory.NORMALIZED_EMAIL_WHITESPACE);
        verify(userRepository).findIdByEmailAndStatus(UserTestDataFactory.DEFAULT_EMAIL, UserStatus.ACTIVE);
    }
}
