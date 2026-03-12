package com.innowise.internship.userservice.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.innowise.internship.userservice.model.User;

public final class UserSpecification {

    private static final char LIKE_ESCAPE = '\\';

    private UserSpecification() {}

    public static Specification<User> filterByNameAndSurnameAndActive(String name, String surname, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

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

            if (active != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("active"), active));
            }

            return predicate;
        };
    }

    private static String escapeForLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}