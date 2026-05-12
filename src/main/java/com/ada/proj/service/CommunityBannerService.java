package com.ada.proj.service;

import com.ada.proj.dto.BannerCreateRequest;
import com.ada.proj.dto.BannerResponse;
import com.ada.proj.entity.CommunityBanner;
import com.ada.proj.repository.CommunityBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityBannerService {

    private final CommunityBannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc()
                .stream().map(BannerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc()
                .stream().map(BannerResponse::from).toList();
    }

    @Transactional
    public BannerResponse createBanner(BannerCreateRequest request) {
        CommunityBanner banner = CommunityBanner.builder()
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .title(request.getTitle())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse updateBanner(Long id, BannerCreateRequest request) {
        CommunityBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("배너를 찾을 수 없습니다. id=" + id));

        if (request.getImageUrl() != null) banner.setImageUrl(request.getImageUrl());
        if (request.getLinkUrl() != null) banner.setLinkUrl(request.getLinkUrl());
        if (request.getTitle() != null) banner.setTitle(request.getTitle());
        if (request.getDisplayOrder() != null) banner.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) banner.setActive(request.getActive());

        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new IllegalArgumentException("배너를 찾을 수 없습니다. id=" + id);
        }
        bannerRepository.deleteById(id);
    }
}
