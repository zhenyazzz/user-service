package com.innowise.internship.userservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.innowise.internship.userservice.dto.request.PaymentCardCreateRequest;
import com.innowise.internship.userservice.dto.request.PaymentCardUpdateRequest;
import com.innowise.internship.userservice.dto.response.PaymentCardResponse;
import com.innowise.internship.userservice.model.PaymentCard;
import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "status", expression = "java(PaymentCardStatus.ACTIVE)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardCreateRequest request, User user);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponse toResponse(PaymentCard card);

    List<PaymentCardResponse> toResponseList(List<PaymentCard> cards);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(PaymentCardUpdateRequest request, @MappingTarget PaymentCard card);
}
