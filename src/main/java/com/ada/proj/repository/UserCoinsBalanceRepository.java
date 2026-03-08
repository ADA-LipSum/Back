package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.UserCoinsBalance;

import jakarta.persistence.LockModeType;

public interface UserCoinsBalanceRepository extends JpaRepository<UserCoinsBalance, String> {

    Optional<UserCoinsBalance> findByUserUuid(String userUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from UserCoinsBalance b where b.userUuid = :userUuid")
    Optional<UserCoinsBalance> findByUserUuidForUpdate(@Param("userUuid") String userUuid);
}
