package com.ada.proj.service;

import com.ada.proj.dto.AdminPostSummaryResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.entity.Comment;
import com.ada.proj.entity.Post;
import com.ada.proj.repository.CommentLikeRepository;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostBookmarkRepository;
import com.ada.proj.repository.PostLikeRepository;
import com.ada.proj.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminPostSummaryResponse> getAdminPosts(String writerUuid, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));
        Page<AdminPostSummaryResponse> result = postRepository.findAdminPostSummaries(
                writerUuid != null && writerUuid.isBlank() ? null : writerUuid,
                query != null && query.isBlank() ? null : query,
                pageable);
        return new PageResponse<>(result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.getContent());
    }

    @Transactional
    public int deletePostsByUser(String writerUuid) {
        List<Post> posts = postRepository.findByWriterUuidOrderByWritedAtDesc(writerUuid);
        for (Post post : posts) {
            deletePostCascade(post);
        }
        return posts.size();
    }

    @Transactional
    public void forceDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("댓글을 찾을 수 없습니다: " + commentId));
        commentLikeRepository.deleteByComment(comment);
        commentRepository.delete(comment);
    }

    private void deletePostCascade(Post post) {
        String postUuid = post.getPostUuid();
        commentLikeRepository.deleteByPostUuid(postUuid);
        commentRepository.deleteByPostUuid(postUuid);
        postLikeRepository.deleteByPostUuid(postUuid);
        postBookmarkRepository.deleteByPostUuid(postUuid);
        postRepository.delete(post);
    }
}
