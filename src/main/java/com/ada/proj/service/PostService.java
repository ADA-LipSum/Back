package com.ada.proj.service;

import com.ada.proj.dto.*;
import com.ada.proj.entity.Post;
import com.ada.proj.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // 생성
    @Transactional
    public String create(PostCreateRequest req) {
        Post p = Post.builder()
                .writerUuid(req.getWriterUuid())
                .title(req.getTitle())
                .texts(req.getTexts())
                .images(req.getImages())
                .videos(req.getVideos())
                .writer(req.getWriter())
                .build();
        return postRepository.save(p).getPostUuid();
    }

    // 목록(최신순)
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));
        return postRepository.findAllByOrderByWritedAtDesc(pageable)
                .map(p -> PostSummaryResponse.builder()
                        .postUuid(p.getPostUuid())
                        .seq(p.getSeq())
                        .title(p.getTitle())
                        .writer(p.getWriter())
                        .writedAt(p.getWritedAt())
                        .likes(p.getLikes())
                        .views(p.getViews())
                        .comments(p.getComments())
                        .build());
    }

    // 상세 (조회수 +1)
    @Transactional
    public PostDetailResponse detail(String uuid) {
        postRepository.increaseViews(uuid);
        Post p = postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));
        return toDetail(p);
    }

    // 수정
    @Transactional
    public void update(String uuid, PostUpdateRequest req) {
        Post p = postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));

        if (req.getTitle()  != null) p.setTitle(req.getTitle());
        if (req.getTexts()  != null) p.setTexts(req.getTexts());
        if (req.getImages() != null) p.setImages(req.getImages());
        if (req.getVideos() != null) p.setVideos(req.getVideos());
        if (req.getWriter() != null) p.setWriter(req.getWriter());
        // updatedAt 은 @PreUpdate 로 자동 갱신
    }

    // 삭제
    @Transactional
    public void delete(String uuid) {
        if (!postRepository.existsById(uuid)) {
            throw new EntityNotFoundException("Post not found: " + uuid);
        }
        postRepository.deleteById(uuid);
    }

    // 좋아요 +1
    @Transactional
    public void like(String uuid) {
        if (postRepository.increaseLikes(uuid) == 0) {
            throw new EntityNotFoundException("Post not found: " + uuid);
        }
    }

    private PostDetailResponse toDetail(Post p) {
        return PostDetailResponse.builder()
                .postUuid(p.getPostUuid())
                .seq(p.getSeq())
                .writerUuid(p.getWriterUuid())
                .title(p.getTitle())
                .texts(p.getTexts())
                .images(p.getImages())
                .videos(p.getVideos())
                .writer(p.getWriter())
                .writedAt(p.getWritedAt())
                .updatedAt(p.getUpdatedAt())
                .likes(p.getLikes())
                .views(p.getViews())
                .comments(p.getComments())
                .build();
    }
}