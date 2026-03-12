package com.innowise.internship.userservice.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import com.innowise.internship.userservice.model.PaymentCard;

public final class PaymentCardSpecification {

    private PaymentCardSpecification() {}

    public static Specification<PaymentCard> filterByActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? cb.conjunction()
                : cb.equal(root.get("active"), active);
    }
}
