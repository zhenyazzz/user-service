package com.innowise.internship.userservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.innowise.internship.userservice.model.PaymentCard;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>, JpaSpecificationExecutor<PaymentCard> {

    @Query("SELECT pc FROM PaymentCard pc JOIN FETCH pc.user WHERE pc.user.id = :userId")
    List<PaymentCard> findAllByUserIdWithUser(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    boolean existsByNumber(String number);

    @Query("SELECT pc.id FROM PaymentCard pc WHERE pc.id = :id AND pc.user.id = :userId")
    Optional<UUID> findIdByIdAndUser_Id(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT pc FROM PaymentCard pc JOIN FETCH pc.user WHERE pc.id = :id")
    Optional<PaymentCard> findByIdWithUser(@Param("id") UUID id);
}
