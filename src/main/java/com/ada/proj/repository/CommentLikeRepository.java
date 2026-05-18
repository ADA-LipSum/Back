package com.ada.proj.repository;

import com.ada.proj.entity.Comment;
import com.ada.proj.entity.CommentLike;
import com.ada.proj.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    long countByComment(Comment comment);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM CommentLike cl WHERE cl.comment.post.postUuid = :postUuid")
    void deleteByPostUuid(@Param("postUuid") String postUuid);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM CommentLike cl WHERE cl.comment = :comment")
    void deleteByComment(@Param("comment") Comment comment);
}