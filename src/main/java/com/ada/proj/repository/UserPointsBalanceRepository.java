package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.UserPointsBalance;

import jakarta.persistence.LockModeType;

public interface UserPointsBalanceRepository extends JpaRepository<UserPointsBalance, String> {

    Optional<UserPointsBalance> findByUserUuid(String userUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from UserPointsBalance b where b.userUuid = :userUuid")
    Optional<UserPointsBalance> findByUserUuidForUpdate(@Param("userUuid") String userUuid);
}
