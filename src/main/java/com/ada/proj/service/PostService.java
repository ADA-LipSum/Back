
package com.ada.proj.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.entity.Post;
import com.ada.proj.repository.PostRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    // MarkdownService는 더 이상 DB 저장을 위해 사용하지 않습니다 (클라이언트/뷰에서 렌더링).

    // 생성
    @Transactional
    public String create(PostCreateRequest req) {
        Post p = Post.builder()
                .writerUuid(req.getWriterUuid())
                .title(req.getTitle())
                .images(req.getImages())
                .videos(req.getVideos())
                .writer(req.getWriter())
                .build();
        // 단일 소스 저장: contentMd만 저장
        String md = req.getContentMd();
        if (md != null) {
            p.setContentMd(md);
            // 과거 호환을 위해 HTML 렌더링 로직은 서비스 내부에서만 사용하고, DB 저장은 하지 않습니다.
        }
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
        // 기존 데이터 호환 로직 제거: 이제 contentMd만 사용합니다.
        return toDetail(p);
    }

    // 수정
    @Transactional
    public void update(String uuid, PostUpdateRequest req) {
        Post p = postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));

        if (req.getTitle()  != null) p.setTitle(req.getTitle());
        // 콘텐츠 갱신: contentMd만 사용
        if (req.getContentMd() != null) {
            String md = req.getContentMd();
            p.setContentMd(md);
            // DB에 contentHtml은 더 이상 저장하지 않습니다.
        }
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
                .contentMd(p.getContentMd())
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