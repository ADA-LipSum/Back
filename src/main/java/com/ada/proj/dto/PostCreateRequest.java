// src/main/java/com/ada/proj/dto/post/PostCreateRequest.java
package com.ada.proj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostCreateRequest {
    // 서버에서 인증 정보로 자동 설정됩니다. 요청 바디에 포함하지 않습니다.
    @Schema(description = "서버에서 자동 설정", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String writerUuid;          // FK: users.uuid

    @NotBlank @Size(max = 20)
    private String title;

    private String texts;
    private String images;              // URL
    private String videos;              // URL
    private String writer;              // 닉네임
}