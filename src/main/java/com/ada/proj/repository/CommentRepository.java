package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.Comment;
import com.ada.proj.entity.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"children", "author"})
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByPostOrderByCreatedAtAsc(Post post, Pageable pageable);

    List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);

    long countByPost(Post post);

    boolean existsByCommentUuid(String commentUuid);

    long countByAuthor_Uuid(String uuid);

    @Modifying
    @Query("UPDATE Comment c SET c.likes = c.likes + 1 WHERE c.id = :id")
    void incrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likes = CASE WHEN c.likes > 0 THEN c.likes - 1 ELSE 0 END WHERE c.id = :id")
    void decrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.fixed = false WHERE c.post = :post AND c.fixed = true")
    void unpinAllByPost(@Param("post") Post post);

    List<Comment> findByPost(Post post);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.postUuid = :postUuid")
    void deleteByPostUuid(@Param("postUuid") String postUuid);
}
