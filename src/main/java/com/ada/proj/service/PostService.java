package com.ada.proj.service;

import com.ada.proj.dto.*;
import com.ada.proj.entity.PostLike;
import com.ada.proj.repository.PostLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PostLikeRepository postLikeRepository;

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
                writerName = writerUser.isUseNickname()
                        ? writerUser.getUserNickname()
                        : writerUser.getUserRealname();
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

        if (req.getContent() != null) {
            p.setContent(req.getContent());
        }

        return postRepository.save(p).getPostUuid();
    }

    // 목록
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> list(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));

        Page<PostSummaryResponse> result =
                postRepository.findAllByOrderByWritedAtDesc(pageable)
                        .map(p -> {
                            User u = userRepository.findByUuid(p.getWriterUuid()).orElse(null);
                            return PostSummaryResponse.builder()
                                    .postUuid(p.getPostUuid())
                                    .seq(p.getSeq())
                                    .title(p.getTitle())
                                    .writer(p.getWriter())
                                    .writerProfileImage(u != null ? u.getProfileImage() : null)
                                    .writedAt(p.getWritedAt())
                                    .likes(p.getLikes())
                                    .views(p.getViews())
                                    .comments(p.getComments())
                                    .isDev(p.getIsDev())
                                    .devTags(p.getDevTags())
                                    .tag(formatTag(p))
                                    .build();
                        });

        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent()
        );
    }

    // 상세(+조회수)
    @Transactional
    public PostDetailResponse detail(String uuid) {

        postRepository.increaseViews(uuid);

        Post p = postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));

        User u = userRepository.findByUuid(p.getWriterUuid()).orElse(null);

        return PostDetailResponse.builder()
                .postUuid(p.getPostUuid())
                .seq(p.getSeq())
                .writerUuid(p.getWriterUuid())
                .writer(p.getWriter())
                .writerProfileImage(u != null ? u.getProfileImage() : null)
                .title(p.getTitle())
                .content(p.getContent())
                .images(p.getImages())
                .videos(p.getVideos())
                .writedAt(p.getWritedAt())
                .updatedAt(p.getUpdatedAt())
                .likes(p.getLikes())
                .views(p.getViews())
                .comments(p.getComments())
                .isDev(p.getIsDev())
                .devTags(p.getDevTags())
                .build();
    }

    // 수정
    @Transactional
    public void update(String uuid, PostUpdateRequest req) {
        Post p = postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));

        if (req.getTitle() != null) p.setTitle(req.getTitle());
        if (req.getContent() != null) p.setContent(req.getContent());
        if (req.getImages() != null) p.setImages(req.getImages());
        if (req.getVideos() != null) p.setVideos(req.getVideos());
        if (req.getIsDev() != null) p.setIsDev(req.getIsDev());
        if (req.getDevTags() != null) p.setDevTags(req.getDevTags());
    }

    // 삭제
    @Transactional
    public void delete(String uuid) {
        if (!postRepository.existsById(uuid)) {
            throw new EntityNotFoundException("Post not found: " + uuid);
        }
        postRepository.deleteById(uuid);
    }

    private String formatTag(Post p) {
        if (Boolean.TRUE.equals(p.getIsDev())) {
            return (p.getDevTags() != null && !p.getDevTags().isBlank())
                    ? "개발(" + p.getDevTags() + ")"
                    : "개발";
        }
        return "일반";
    }

    @Transactional
    public boolean toggleLike(String userUuid, String postUuid) {

        Post post = postRepository.findById(postUuid)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        boolean alreadyLiked = postLikeRepository.existsByUserUuidAndPostUuid(userUuid, postUuid);

        if (alreadyLiked) {
            // 좋아요 취소
            postLikeRepository.deleteByUserUuidAndPostUuid(userUuid, postUuid);
            post.setLikes(Math.max(0, post.getLikes() - 1));
            return false; // 좋아요 취소됨
        } else {
            // 좋아요 추가
            PostLike like = PostLike.builder()
                    .userUuid(userUuid)
                    .postUuid(postUuid)
                    .build();
            postLikeRepository.save(like);

            post.setLikes(post.getLikes() + 1);
            return true; // 좋아요 눌림
        }
    }
}