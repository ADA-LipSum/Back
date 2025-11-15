package com.ada.proj.dto;

import com.ada.proj.entity.StoreType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreItemCreateRequest {

    @NotBlank
    private String name;

    @Min(1)
    private int price;

    @NotBlank
    private String category;

    @NotNull
    private StoreType storeType;

    @NotBlank
    private String imageUrl;
}
