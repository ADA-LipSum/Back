package com.ada.proj.repository;

import com.ada.proj.entity.CommunityBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityBannerRepository extends JpaRepository<CommunityBanner, Long> {

    List<CommunityBanner> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    List<CommunityBanner> findAllByOrderByDisplayOrderAscCreatedAtAsc();
}
