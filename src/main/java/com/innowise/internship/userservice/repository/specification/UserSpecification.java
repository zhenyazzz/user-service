package com.innowise.internship.userservice.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.UserStatus;

public final class UserSpecification {

    private static final char LIKE_ESCAPE = '\\';

    private UserSpecification() {}

    public static Specification<User> buildFilter(String name, String surname) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("status"), UserStatus.ACTIVE));

            if (StringUtils.hasText(name)) {
                String safeName = escapeForLike(name.toLowerCase());
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + safeName + "%", LIKE_ESCAPE));
            }

            if (StringUtils.hasText(surname)) {
                String safeSurname = escapeForLike(surname.toLowerCase());
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("surname")), "%" + safeSurname + "%", LIKE_ESCAPE));
            }

            return predicate;
        };
    }

    private static String escapeForLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}