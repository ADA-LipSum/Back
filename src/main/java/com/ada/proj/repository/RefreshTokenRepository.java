package com.ada.proj.repository;

import org.springframework.data.repository.CrudRepository;

import com.ada.proj.entity.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
