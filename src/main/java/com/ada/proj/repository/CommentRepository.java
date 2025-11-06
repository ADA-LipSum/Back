package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.Comment;
import com.ada.proj.entity.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
}
