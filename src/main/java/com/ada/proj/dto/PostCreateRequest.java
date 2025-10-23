// src/main/java/com/ada/proj/dto/post/PostCreateRequest.java
package com.ada.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostCreateRequest {
    @NotBlank
    private String writerUuid;          // FK: users.uuid

    @NotBlank @Size(max = 20)
    private String title;

    private String texts;
    private String images;              // URL
    private String videos;              // URL
    private String writer;              // 닉네임
}