package com.ada.proj.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.StoreItemCreateRequest;
import com.ada.proj.entity.StoreItem;
import com.ada.proj.repository.StoreItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreItemService {

    private final StoreItemRepository storeItemRepository;

    private void ensureTeacher(Authentication auth) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        boolean isTeacherOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String authName = a.getAuthority();
                    return authName.equals("TEACHER") || authName.equals("ADMIN");
                });

        if (!isTeacherOrAdmin) {
            throw new SecurityException("Teacher 또는 Admin 권한만 사용할 수 있는 기능입니다.");
        }
    }

    private void ensureAdmin(Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        if (!isAdmin) throw new SecurityException("Forbidden");
    }

    private void ensureSelfOrAdmin(Authentication auth, String uuid) {
        if (auth == null) throw new SecurityException("Unauthenticated");

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        String principal = auth.getName();

        if (!isAdmin && !uuid.equals(principal)) {
            throw new SecurityException("Forbidden");
        }
    }

    @Transactional
    public StoreItem createItem(StoreItemCreateRequest req, Authentication auth) {
        ensureTeacher(auth);
        // imageUrl "null" 문자열이 들어오면 실제 null 로 치환 (선택적 처리)
        String imageUrl = (req.getImageUrl() != null && req.getImageUrl().equalsIgnoreCase("null"))
            ? null
            : req.getImageUrl();

        StoreItem item = StoreItem.builder()
            .name(req.getName())
            .price(req.getPrice())
            .category(req.getCategory())
            .storeType(req.getStoreType())
            .imageUrl(imageUrl)
            .build();

        return storeItemRepository.save(item);
    }
}