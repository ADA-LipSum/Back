package com.ada.proj.repository;

import com.ada.proj.entity.Comment;
import com.ada.proj.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
}