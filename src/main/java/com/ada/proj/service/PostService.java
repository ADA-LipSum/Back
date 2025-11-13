
package com.ada.proj.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.User;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 생성
    @Transactional
    public String create(PostCreateRequest req) {
        String writerUuid = req.getWriterUuid();
        if (writerUuid == null || writerUuid.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                writerUuid = auth.getName();
            }
        }

        String writerName = null;
        if (writerUuid != null && !writerUuid.isBlank()) {
            User writerUser = userRepository.findByUuid(writerUuid).orElse(null);
            if (writerUser != null) {
                writerName = writerUser.isUseNickname() ? writerUser.getUserNickname() : writerUser.getUserRealname();
            }
        }

        Post p = Post.builder()
                .writerUuid(writerUuid)
                .title(req.getTitle())
                .images(req.getImages())
                .videos(req.getVideos())
                .writer(writerName)
                .isDev(req.getIsDev() != null ? req.getIsDev() : false)
                .devTags(req.getDevTags())
                .build();
        // 콘텐츠 저장
        String md = req.getContent();
        if (md != null) {
            p.setContent(md);
        }
        return postRepository.save(p).getPostUuid();
    }

    // 목록(최신)
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
                .isDev(p.getIsDev())
                .devTags(p.getDevTags())
                .tag(formatTag(p))
                        .build());
    }

    // 상세(+조회수)
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
        // 콘텐츠 갱신
        if (req.getContent() != null) {
            String md = req.getContent();
            p.setContent(md);
        }
        if (req.getImages() != null) p.setImages(req.getImages());
        if (req.getVideos() != null) p.setVideos(req.getVideos());
        if (req.getIsDev() != null) p.setIsDev(req.getIsDev());
        if (req.getDevTags() != null) p.setDevTags(req.getDevTags());
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
                .content(p.getContent())
                .images(p.getImages())
                .videos(p.getVideos())
                .writer(p.getWriter())
                .writedAt(p.getWritedAt())
                .updatedAt(p.getUpdatedAt())
                .likes(p.getLikes())
                .views(p.getViews())
                .comments(p.getComments())
                .isDev(p.getIsDev())
                .devTags(p.getDevTags())
                .build();
    }

    private String formatTag(Post p) {
        Boolean dev = p.getIsDev();
        if (dev != null && dev) {
            String langs = p.getDevTags();
            if (langs != null && !langs.isBlank()) {
                return "개발(" + langs + ")";
            }
            return "개발";
        }
        return "일반";
    }
}