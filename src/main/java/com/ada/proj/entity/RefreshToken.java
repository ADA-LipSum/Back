package com.ada.proj.entity;

import java.util.concurrent.TimeUnit;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("rt")
public class RefreshToken {

    @Id
    private String uuid;

    private String token;

    private boolean rememberMe;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private long ttl;
}
