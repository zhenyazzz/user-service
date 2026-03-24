package com.innowise.internship.userservice.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.innowise.internship.userservice.model.User;
import com.innowise.internship.userservice.model.enums.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @EntityGraph(value = "User.withCards", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findByIdAndStatus(UUID id, UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.status = :status")
    Optional<User> findByIdAndStatusForUpdate(@Param("id") UUID id, @Param("status") UserStatus status);

    @Query("SELECT u.id FROM User u WHERE u.email = :email AND u.status = :status")
    Optional<UUID> findIdByEmailAndStatus(@Param("email") String email, @Param("status") UserStatus status);

    boolean existsByEmail(String email);

    @EntityGraph(value = "User.withCards", type = EntityGraph.EntityGraphType.LOAD)
    Page<User> findAll(Specification<User> specification, Pageable pageable);

}
