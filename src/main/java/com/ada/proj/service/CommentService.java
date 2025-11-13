package com.ada.proj.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ada.proj.entity.CommentLike;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.CommentCreateRequest;
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.CommentUpdateRequest;
import com.ada.proj.entity.Comment;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.User;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.repository.CommentLikeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Unauthenticated");
        }

        String uuid = authentication.getName(); // JWT subject = uuid
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("User not found: " + uuid));
    }

    @Transactional
    public CommentResponse createComment(CommentCreateRequest request) {
        User user = getCurrentUser();

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(user);

        // 작성 당시 표시 이름 저장(닉네임 or 실명)
        String displayName = user.isUseNickname()
                ? user.getUserNickname()
                : user.getUserRealname();
        comment.setAuthorName(displayName);

        comment.setPost(post);

        // 대댓글 처리
        if (request.getParentId() != null) {
            if (request.getParentId().equals(comment.getId())) {
                throw new IllegalArgumentException("Cannot reply to itself");
            }
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParent(parent);
        }

        return new CommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post)
                .stream()
                .map(CommentResponse::new)
                .toList();
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest req) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 체크: 댓글 작성자(seq) = 현재 로그인 사용자(seq)
        if (!comment.getAuthor().getSeq().equals(currentUser.getSeq())) {
            throw new AccessDeniedException("본인의 댓글만 수정할 수 있습니다.");
        }

        // 내용 수정
        comment.setContent(req.getContent());
        // edited = true는 @PreUpdate 자동 처리

        Comment saved = commentRepository.save(comment);

        return new CommentResponse(saved);
    }

    @Transactional
    public void deleteComment(Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 체크
        if (!comment.getAuthor().getSeq().equals(currentUser.getSeq())) {
            throw new AccessDeniedException("본인의 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        Optional<CommentLike> existing = commentLikeRepository.findByCommentAndUser(comment, currentUser);

        boolean liked;

        if (existing.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existing.get());
            comment.setLikes(comment.getLikes() - 1);
            liked = false;
        } else {
            // 좋아요 추가
            CommentLike like = CommentLike.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build();

            commentLikeRepository.save(like);
            comment.setLikes(comment.getLikes() + 1);
            liked = true;
        }

        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likes", comment.getLikes());

        return result;
    }

    @Transactional
    public Map<String, Object> toggleFixed(Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 체크: 게시글 작성자만 가능
        if (!comment.getPost().getWriterUuid().equals(currentUser.getUuid())) {
            throw new AccessDeniedException("게시글 작성자만 댓글을 고정하거나 해제할 수 있습니다.");
        }

        // 고정 토글
        boolean newState = !comment.isFixed();
        comment.setFixed(newState);

        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("fixed", newState);
        result.put("commentId", comment.getId());

        return result;
    }
}
