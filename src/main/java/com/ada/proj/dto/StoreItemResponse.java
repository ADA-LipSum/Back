package com.ada.proj.dto;

import com.ada.proj.entity.StoreItem;
import com.ada.proj.entity.StoreType;
import lombok.Data;

@Data
public class StoreItemResponse {

    private String itemUuid;
    private String name;
    private int price;
    private String category;
    private StoreType storeType;
    private String imageUrl;

    public static StoreItemResponse from(StoreItem item) {
        StoreItemResponse r = new StoreItemResponse();
        r.setItemUuid(item.getItemUuid());
        r.setName(item.getName());
        r.setPrice(item.getPrice());
        r.setCategory(item.getCategory());
        r.setStoreType(item.getStoreType());
        r.setImageUrl(item.getImageUrl());
        return r;
    }
}