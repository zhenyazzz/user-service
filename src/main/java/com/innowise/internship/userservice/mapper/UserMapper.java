package com.innowise.internship.userservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.innowise.internship.userservice.dto.request.UserCreateRequest;
import com.innowise.internship.userservice.dto.request.UserUpdateRequest;
import com.innowise.internship.userservice.dto.response.UserResponse;
import com.innowise.internship.userservice.model.PaymentCard;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;
import com.innowise.internship.userservice.model.enums.UserStatus;

@Mapper(componentModel = "spring", uses = PaymentCardMapper.class)
public interface UserMapper {

    default String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    @Mapping(target = "status", expression = "java(UserStatus.ACTIVE)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "email", expression = "java(normalizeEmail(request.email()))")
    User toEntity(UserCreateRequest request);

    @Mapping(target = "paymentCards", source = "activeCards")
    UserResponse toResponse(User user, List<PaymentCard> activeCards);

    default UserResponse toResponse(User user) {
        if (user == null) return null;
        List<PaymentCard> active = user.getPaymentCards() == null ? List.of()
                : user.getPaymentCards().stream()
                        .filter(pc -> PaymentCardStatus.ACTIVE.equals(pc.getStatus()))
                        .toList();
        return toResponse(user, active);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}
