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
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("createUser when email is free saves and returns response")
    void createUser_Success() {
        UserCreateRequest request = UserTestDataFactory.buildUserCreateRequest();
        User user = UserTestDataFactory.buildUser();
        UserResponse response = UserTestDataFactory.buildUserResponse(user.getId());

        when(userRepository.findIdByEmail(UserTestDataFactory.DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findIdByEmail(UserTestDataFactory.DEFAULT_EMAIL);
        verify(userMapper).toEntity(request);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("createUser when email exists throws UserAlreadyExistsException")
    void createUser_whenEmailExists_throwsUserAlreadyExistsException() {
        UserCreateRequest request = UserTestDataFactory.buildUserCreateRequest();
        when(userRepository.findIdByEmail(UserTestDataFactory.DEFAULT_EMAIL)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(UserTestDataFactory.DEFAULT_EMAIL);

        verify(userRepository).findIdByEmail(UserTestDataFactory.DEFAULT_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserById when user exists returns response")
    void getUserById_Success() {
        User user = UserTestDataFactory.buildUser();
        UserResponse response = UserTestDataFactory.buildUserResponse(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserById(user.getId());

        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(user.getId());
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("getUserById when user not exists throws UserNotFoundException")
    void getUserById_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findById(id);
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

        Page<UserResponse> result = userService.getAllUsers(null, null, null, pageable);

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

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUser(user.getId(), request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(user.getId());
        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("updateUser when user not exists throws UserNotFoundException")
    void updateUser_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        UserUpdateRequest request = UserTestDataFactory.buildUserUpdateRequest();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(id, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findById(id);
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("activateUser when user exists sets active and returns response")
    void activateUser_whenUserExists_returnsResponse() {
        User user = UserTestDataFactory.buildUser();
        user.setActive(false);
        UserResponse response = UserTestDataFactory.buildUserResponse(user);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.activateUser(user.getId());

        assertThat(result).isEqualTo(response);
        assertThat(user.getActive()).isTrue();
        verify(userRepository).findById(user.getId());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("activateUser when user not exists throws UserNotFoundException")
    void activateUser_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivateUser when user exists sets inactive and returns response")
    void deactivateUser_whenUserExists_returnsResponse() {
        User user = UserTestDataFactory.buildUser();
        user.setActive(true);
        UserResponse response = UserTestDataFactory.buildUserResponse(user);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.deactivateUser(user.getId());

        assertThat(result).isEqualTo(response);
        assertThat(user.getActive()).isFalse();
        verify(userRepository).findById(user.getId());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("deactivateUser when user not exists throws UserNotFoundException")
    void deactivateUser_whenUserNotExists_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + id);

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findIdByEmail when email exists returns id")
    void findIdByEmail_Success() {
        String email = UserTestDataFactory.DEFAULT_EMAIL;
        UUID id = UUID.randomUUID();

        when(userRepository.findIdByEmail(email)).thenReturn(Optional.of(id));

        UUID result = userService.findIdByEmail(email);

        assertThat(result).isEqualTo(id);
        verify(userRepository).findIdByEmail(email);
    }

    @Test
    @DisplayName("findIdByEmail when email not exists throws UserNotFoundException")
    void findIdByEmail_whenEmailNotExists_throwsUserNotFoundException() {
        String email = UserTestDataFactory.DEFAULT_EMAIL;

        when(userRepository.findIdByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findIdByEmail(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with email: " + email);

        verify(userRepository).findIdByEmail(email);
    }

    @Test
    @DisplayName("findIdByEmail normalizes email (trim, lowerCase) before calling repository")
    void findIdByEmail_normalizesEmailBeforeLookup() {
        UUID id = UUID.randomUUID();
        when(userRepository.findIdByEmail(UserTestDataFactory.DEFAULT_EMAIL)).thenReturn(Optional.of(id));

        UUID result = userService.findIdByEmail(UserTestDataFactory.NORMALIZED_EMAIL_WHITESPACE);

        assertThat(result).isEqualTo(id);
        verify(userRepository).findIdByEmail(UserTestDataFactory.DEFAULT_EMAIL);
    }
}
