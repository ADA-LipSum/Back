package com.ada.proj.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.CommentCreateRequest;
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.CommentUpdateRequest;
import com.ada.proj.entity.Comment;
import com.ada.proj.entity.CommentLike;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.User;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.CommentLikeRepository;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserBanService userBanService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Unauthenticated");
        }

        String uuid = authentication.getName();
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public CommentResponse createComment(@NonNull CommentCreateRequest request) {

        User user = getCurrentUser();

        userBanService.checkUserBanned(user);

        Long postSeq = Objects.requireNonNull(request.getPostSeq(), "postSeq is required");
        Post post = postRepository.findBySeq(postSeq)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(user);

        // 표시명 저장
        String displayName = user.isUseNickname()
                ? user.getUserNickname()
                : user.getUserRealname();
        comment.setAuthorName(displayName);

        comment.setImages(toImageCsv(request.getImages()));
        comment.setPost(post);

        // 대댓글
        Long parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent comment not found"));
            if (parent.getPost() == null || !post.getSeq().equals(parent.getPost().getSeq())) {
                throw new IllegalArgumentException("Parent comment does not belong to this post");
            }
            comment.setParent(parent);
        }

        Comment saved = commentRepository.save(comment);

        // 댓글 수 갱신 (대댓글 포함 전체 개수)
        updatePostCommentsCount(post);

        log.info("[COMMENT-CREATE] postUuid={}, commentId={}, parentId={}",
            post.getPostUuid(), saved.getId(),
            request.getParentId() != null ? request.getParentId() : "ROOT");

        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(@NonNull Long postSeq) {

        Post post = postRepository.findBySeq(postSeq)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        List<Comment> allComments = commentRepository.findByPostOrderByCreatedAtAsc(post,
                org.springframework.data.domain.PageRequest.of(0, 500));

        // build parentId -> children list map
        var childrenMap = new java.util.HashMap<Long, java.util.List<Comment>>();
        java.util.List<Comment> roots = new java.util.ArrayList<>();

        for (Comment c : allComments) {
            if (c.getParent() == null) {
                roots.add(c);
            } else {
                Long pid = c.getParent().getId();
                childrenMap.computeIfAbsent(pid, k -> new java.util.ArrayList<>()).add(c);
            }
        }

        // build responses from roots using the childrenMap (no further DB queries)
        return roots.stream()
                .map(r -> buildResponseWithChildren(r, childrenMap))
                .toList();
    }

    @Transactional
    public CommentResponse updateComment(@NonNull Long commentId, @NonNull CommentUpdateRequest req) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getSeq().equals(currentUser.getSeq())) {
            throw new AccessDeniedException("본인의 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(req.getContent());
        if (req.getImages() != null) {
            comment.setImages(toImageCsv(req.getImages()));
        }

        Comment saved = commentRepository.save(comment);

        return buildResponse(saved);
    }

    @Transactional
    public void deleteComment(@NonNull Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getSeq().equals(currentUser.getSeq())) {
            throw new AccessDeniedException("본인의 댓글만 삭제할 수 있습니다.");
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);
        updatePostCommentsCount(post);
        log.info("[COMMENT-DELETE] postUuid={}, deletedCommentId={}", post.getPostUuid(), commentId);
    }

    @Transactional
    public Map<String, Object> addLike(@NonNull Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (commentLikeRepository.findByCommentAndUser(comment, currentUser).isPresent()) {
            throw new IllegalStateException("이미 좋아요한 댓글입니다.");
        }

        CommentLike like = CommentLike.builder()
                .comment(comment)
                .user(currentUser)
                .build();
        commentLikeRepository.save(Objects.requireNonNull(like));
        // @PreUpdate 발동 없이 likes 컬럼만 직접 업데이트 (edited=true 오염 방지)
        commentRepository.incrementLikes(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", true);
        result.put("likes", comment.getLikes() + 1);
        return result;
    }

    @Transactional
    public Map<String, Object> removeLike(@NonNull Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        CommentLike existing = commentLikeRepository.findByCommentAndUser(comment, currentUser)
                .orElseThrow(() -> new IllegalStateException("좋아요하지 않은 댓글입니다."));

        commentLikeRepository.delete(existing);
        // @PreUpdate 발동 없이 likes 컬럼만 직접 업데이트 (0 미만 방지 + edited=true 오염 방지)
        commentRepository.decrementLikes(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", false);
        result.put("likes", Math.max(comment.getLikes() - 1, 0));
        return result;
    }

    @Transactional
    public Map<String, Object> pinComment(@NonNull Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getPost().getWriterUuid().equals(currentUser.getUuid())) {
            throw new AccessDeniedException("게시글 작성자만 가능");
        }
        if (comment.isFixed()) {
            throw new IllegalStateException("이미 고정된 댓글입니다.");
        }
        // 기존 핀 댓글 해제 후 새 핀 설정 (게시글당 핀은 1개)
        commentRepository.unpinAllByPost(comment.getPost());
        comment.setFixed(true);
        commentRepository.save(comment);
        Map<String, Object> result = new HashMap<>();
        result.put("fixed", true);
        result.put("commentId", comment.getId());
        return result;
    }

    @Transactional
    public Map<String, Object> unpinComment(@NonNull Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getPost().getWriterUuid().equals(currentUser.getUuid())) {
            throw new AccessDeniedException("게시글 작성자만 가능");
        }
        if (!comment.isFixed()) {
            throw new IllegalStateException("고정되지 않은 댓글입니다.");
        }
        comment.setFixed(false);
        commentRepository.save(comment);
        Map<String, Object> result = new HashMap<>();
        result.put("fixed", false);
        result.put("commentId", comment.getId());
        return result;
    }

    private CommentResponse buildResponse(Comment comment) {
        User author = comment.getAuthor();
        String displayName = author.isUseNickname() ? author.getUserNickname() : author.getUserRealname();

        return CommentResponse.builder()
                .commentId(comment.getId())
                .writerUuid(author.getUuid())
                .writerCustomId(author.getCustomId())
                .writer(displayName)
                .writerProfileImage(author.getProfileImage())
                .content(comment.getContent())
                .images(fromImageCsv(comment.getImages()))
                .createdAt(comment.getCreatedAt())
                .children(Collections.emptyList())
                .build();
    }

    // Build CommentResponse using pre-fetched child lists from childrenMap to avoid extra queries
    private CommentResponse buildResponseWithChildren(Comment comment, java.util.Map<Long, java.util.List<Comment>> childrenMap) {
        User author = comment.getAuthor();
        String displayName = author.isUseNickname() ? author.getUserNickname() : author.getUserRealname();

        java.util.List<CommentResponse> childResponses = childrenMap.getOrDefault(comment.getId(), Collections.emptyList()).stream()
                .map(c -> buildResponseWithChildren(c, childrenMap))
                .toList();

        return CommentResponse.builder()
                .commentId(comment.getId())
                .writerUuid(author.getUuid())
                .writerCustomId(author.getCustomId())
                .writer(displayName)
                .writerProfileImage(author.getProfileImage())
                .content(comment.getContent())
                .images(fromImageCsv(comment.getImages()))
                .createdAt(comment.getCreatedAt())
                .children(childResponses)
                .build();
    }

    private String toImageCsv(List<String> images) {
        if (images == null || images.isEmpty()) return null;
        return String.join(",", images);
    }

    private List<String> fromImageCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.asList(csv.split(","));
    }

    private void updatePostCommentsCount(Post post) {
        long total = commentRepository.countByPost(post);
        post.setComments((int) total);
    }
}
