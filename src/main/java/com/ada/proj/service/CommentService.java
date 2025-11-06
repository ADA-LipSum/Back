package com.ada.proj.service;

import java.util.List;

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

import lombok.RequiredArgsConstructor;

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
    public List<CommentResponse> getCommentsByPost(String postId) {
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
            throw new AccessDeniedException("You cannot edit others' comments");
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
            throw new AccessDeniedException("You cannot delete others' comments");
        }
        commentRepository.delete(comment);
    }
}
