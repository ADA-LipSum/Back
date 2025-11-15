package com.ada.proj.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_items",
        indexes = {
                @Index(name = "idx_store_items_category", columnList = "category"),
                @Index(name = "idx_store_items_store_type", columnList = "store_type"),
                @Index(name = "idx_store_items_price", columnList = "price")
        })
public class StoreItem {

    @Id
    @Column(name = "item_uuid", length = 36, nullable = false)
    private String itemUuid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_type", length = 20, nullable = false)
    private StoreType storeType;

    @PrePersist
    public void prePersist() {
        if (this.itemUuid == null) {
            this.itemUuid = UUID.randomUUID().toString();
        }
    }
}