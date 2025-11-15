package com.ada.proj.repository;

import com.ada.proj.entity.StoreItem;
import com.ada.proj.entity.StoreType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreItemRepository extends JpaRepository<StoreItem, String> {

    @Query("""
        SELECT i FROM StoreItem i
        WHERE (:name IS NULL OR i.name LIKE CONCAT('%', :name, '%'))
          AND (:minPrice IS NULL OR i.price >= :minPrice)
          AND (:maxPrice IS NULL OR i.price <= :maxPrice)
          AND (:category IS NULL OR i.category = :category)
          AND (:storeType IS NULL OR i.storeType = :storeType)
    """)
    List<StoreItem> filter(
            @Param("name") String name,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("category") String category,
            @Param("storeType") StoreType storeType
    );
}