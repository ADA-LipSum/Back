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

        String uuid = authentication.getName();
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("User not found: " + uuid));
    }

    /** 댓글 생성 */
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request) {

        User user = getCurrentUser();

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(user);

        // 표시명 저장
        String displayName = user.isUseNickname()
                ? user.getUserNickname()
                : user.getUserRealname();
        comment.setAuthorName(displayName);

        comment.setPost(post);

        // 대댓글
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParent(parent);
        }

        Comment saved = commentRepository.save(comment);

        return buildResponse(saved);
    }

    /** 댓글 조회 (작성자 이름 + 프로필 이미지 포함) */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(String postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<Comment> comments =
                commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post);

        return comments.stream()
                .map(this::buildResponse)
                .toList();
    }

    /** 댓글 수정 */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest req) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getSeq().equals(currentUser.getSeq())) {
            throw new AccessDeniedException("본인의 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(req.getContent());

        Comment saved = commentRepository.save(comment);

        return buildResponse(saved);
    }

    /** 댓글 삭제 */
    @Transactional
    public void deleteComment(Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getSeq().equals(currentUser.getSeq())) {
            throw new AccessDeniedException("본인의 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    /** 댓글 좋아요 토글 */
    @Transactional
    public Map<String, Object> toggleLike(Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        Optional<CommentLike> existing =
                commentLikeRepository.findByCommentAndUser(comment, currentUser);

        boolean liked;

        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            comment.setLikes(comment.getLikes() - 1);
            liked = false;
        } else {
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

    /** 댓글 고정 토글 */
    @Transactional
    public Map<String, Object> toggleFixed(Long commentId) {

        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getPost().getWriterUuid().equals(currentUser.getUuid())) {
            throw new AccessDeniedException("게시글 작성자만 가능");
        }

        boolean newState = !comment.isFixed();
        comment.setFixed(newState);

        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("fixed", newState);
        result.put("commentId", comment.getId());

        return result;
    }

    /** Comment → Response 변환 */
    private CommentResponse buildResponse(Comment comment) {

        User author = comment.getAuthor();

        String displayName = author.isUseNickname()
                ? author.getUserNickname()
                : author.getUserRealname();

        return CommentResponse.builder()
                .commentId(comment.getId())
                .writerUuid(author.getUuid())
                .writer(displayName)
                .writerProfileImage(author.getProfileImage())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}