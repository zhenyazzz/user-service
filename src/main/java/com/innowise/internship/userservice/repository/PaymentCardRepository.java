package com.innowise.internship.userservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.innowise.internship.userservice.model.PaymentCard;
import com.innowise.internship.userservice.model.enums.PaymentCardStatus;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>, JpaSpecificationExecutor<PaymentCard> {

    @Query(
        value = "SELECT pc FROM PaymentCard pc LEFT JOIN FETCH pc.user WHERE (:status IS NULL OR pc.status = :status)",
        countQuery = "SELECT COUNT(pc) FROM PaymentCard pc WHERE (:status IS NULL OR pc.status = :status)"
    )
    Page<PaymentCard> findAllWithUser(@Param("status") PaymentCardStatus status, Pageable pageable);

    @Query("SELECT pc FROM PaymentCard pc JOIN FETCH pc.user WHERE pc.user.id = :userId AND pc.status = :status")
    List<PaymentCard> findAllByUserIdAndStatusWithUser(@Param("userId") UUID userId, @Param("status") PaymentCardStatus status);

    long countByUserIdAndStatus(UUID userId, PaymentCardStatus status);

    boolean existsByNumber(String number);

    boolean existsByIdAndUser_Id(UUID id, UUID userId);

    @Query("SELECT pc FROM PaymentCard pc JOIN FETCH pc.user WHERE pc.id = :id AND pc.status = :status")
    Optional<PaymentCard> findByIdWithUserAndStatus(@Param("id") UUID id, @Param("status") PaymentCardStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PaymentCard pc " +
        "SET pc.status = :status, pc.updatedAt = CURRENT_TIMESTAMP " +
        "WHERE pc.user.id = :userId AND pc.status = :oldStatus")
    int updateStatusByUserId(@Param("userId") UUID userId, @Param("status") PaymentCardStatus status, @Param("oldStatus") PaymentCardStatus oldStatus);
}
