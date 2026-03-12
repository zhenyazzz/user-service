package com.innowise.internship.userservice.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.innowise.internship.userservice.exception.security.SecurityContextException;

import lombok.experimental.UtilityClass;


@UtilityClass
public class SecurityUtils {
 
    public Optional<CurrentUser> getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof CurrentUser)
                .map(CurrentUser.class::cast);
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().map(CurrentUser::userId).orElseThrow(() -> new SecurityContextException("User not found in security context"));
    }

    public String getCurrentUserEmail() {
        return getCurrentUser().map(CurrentUser::email).orElseThrow(() -> new SecurityContextException("User not found in security context"));
    }

    public String getCurrentUserRole() {
        return getCurrentUser().map(CurrentUser::role).orElseThrow(() -> new SecurityContextException("User not found in security context"));
    }

    public boolean isAdmin() {
        return Roles.ADMIN.matches(getCurrentUserRole());
    }

}
