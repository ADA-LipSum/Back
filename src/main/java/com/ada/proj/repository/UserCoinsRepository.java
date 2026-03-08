package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.UserCoins;

public interface UserCoinsRepository extends JpaRepository<UserCoins, Long> {

    Optional<UserCoins> findByCoinUuid(String coinUuid);

    Page<UserCoins> findByUserUuidOrderByCreatedAtDescSeqDesc(String userUuid, Pageable pageable);
}
