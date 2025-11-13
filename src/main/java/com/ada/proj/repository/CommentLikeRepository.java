package com.ada.proj.repository;

import com.ada.proj.entity.Comment;
import com.ada.proj.entity.CommentLike;
import com.ada.proj.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    long countByComment(Comment comment);
}