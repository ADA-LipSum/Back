package com.ada.proj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PurchaseItemRequest {

    @NotBlank
    private String itemUuid;
}