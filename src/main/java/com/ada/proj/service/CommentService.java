package com.ada.proj.service;

import com.ada.proj.dto.*;
import com.ada.proj.entity.*;
import com.ada.proj.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String customId = authentication.getName(); // JWT에 저장된 ID (UserDetailsService에서 반환한 ID)
        return userRepository.findByCustomId(customId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public CommentResponse createComment(CommentCreateRequest request) {
        User user = getCurrentUser();
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(user);
        comment.setPost(post);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParent(parent);
        }

        return new CommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(String postId) { // ← 타입도 String이면 더 정확
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post)
                .stream()
                .map(CommentResponse::new)
                .toList();
    }

    @Transactional
    public CommentResponse updateComment(Long id, CommentUpdateRequest request) {
        User user = getCurrentUser();
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getSeq().equals(user.getSeq())) {
            throw new RuntimeException("You cannot edit others' comments");
        }

        comment.setContent(request.getContent());
        return new CommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long id) {
        User user = getCurrentUser();
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getSeq().equals(user.getSeq())) {
            throw new RuntimeException("You cannot delete others' comments");
        }

        commentRepository.delete(comment);
    }
}