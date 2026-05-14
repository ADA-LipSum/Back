package com.ada.proj.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.entity.Post;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    Page<Post> findAllByOrderByWritedAtDesc(Pageable pageable);

    @Query("""
            select new com.ada.proj.dto.PostSummaryResponse(
                p.postUuid, p.seq, p.title, p.writer, u.profileImage,
                p.writedAt, p.likes, p.views, p.comments,
                p.isDev, p.devTags, null,
                p.boardType, p.communityCategory, p.techSubTag, p.thumbnailImage
            )
            from Post p left join com.ada.proj.entity.User u on u.uuid = p.writerUuid
            """)
    Page<PostSummaryResponse> findSummaryPage(Pageable pageable);

    @Query("""
            select new com.ada.proj.dto.PostSummaryResponse(
                p.postUuid, p.seq, p.title, p.writer, u.profileImage,
                p.writedAt, p.likes, p.views, p.comments,
                p.isDev, p.devTags, null,
                p.boardType, p.communityCategory, p.techSubTag, p.thumbnailImage
            )
            from Post p left join com.ada.proj.entity.User u on u.uuid = p.writerUuid
            where (
                :boardType is null
                or p.boardType = :boardType
                or (:includeLegacyCommunity = true and p.boardType is null)
            )
              and (:category is null or p.communityCategory = :category)
              and (:techSubTag is null or p.techSubTag = :techSubTag)
              and (:techTag is null or lower(coalesce(p.devTags, '')) like lower(concat('%', :techTag, '%')))
              and (
                :query is null
                or lower(p.title) like lower(concat('%', :query, '%'))
                or p.content like concat('%', :query, '%')
              )
            """)
    Page<PostSummaryResponse> searchSummaries(
            @Param("boardType") PostBoardType boardType,
            @Param("category") CommunityCategory category,
            @Param("techSubTag") TechSubTag techSubTag,
            @Param("techTag") String techTag,
            @Param("query") String query,
            @Param("includeLegacyCommunity") boolean includeLegacyCommunity,
            Pageable pageable);

    @Query("""
            select new com.ada.proj.dto.NoticeSummaryResponse(
                p.postUuid, p.title, p.writer, u.profileImage, p.writedAt
            )
            from Post p left join com.ada.proj.entity.User u on u.uuid = p.writerUuid
            where p.boardType = com.ada.proj.enums.PostBoardType.NOTICE
            order by p.writedAt desc
            """)
    Page<NoticeSummaryResponse> findNoticeSummaries(Pageable pageable);

    @Modifying
    @Query("update Post p set p.views = p.views + 1 where p.postUuid = :uuid")
    int increaseViews(@Param("uuid") String uuid);

    @Modifying
    @Query("update Post p set p.likes = p.likes + 1 where p.postUuid = :uuid")
    int increaseLikes(@Param("uuid") String uuid);

    @Modifying
    @Query("""
           update Post p
              set p.likes = case when p.likes > 0 then p.likes - 1 else 0 end
            where p.postUuid = :uuid
           """)
    int decreaseLikes(@Param("uuid") String uuid);

    java.util.Optional<Post> findBySeq(Long seq);

    @Query("select p.seq from Post p where p.postUuid = :uuid")
    Long findSeqByUuid(@Param("uuid") String uuid);

    long countByWriterUuid(String writerUuid);

    @Query("""
            select new com.ada.proj.dto.PostSummaryResponse(
                p.postUuid, p.seq, p.title, p.writer, u.profileImage,
                p.writedAt, p.likes, p.views, p.comments,
                p.isDev, p.devTags, null,
                p.boardType, p.communityCategory, p.techSubTag, p.thumbnailImage
            )
            from Post p
            join com.ada.proj.entity.PostBookmark pb on pb.postUuid = p.postUuid
            left join com.ada.proj.entity.User u on u.uuid = p.writerUuid
            where pb.userUuid = :userUuid
              and p.boardType = :boardType
            """)
    Page<PostSummaryResponse> findBookmarkedSummaries(
            @Param("userUuid") String userUuid,
            @Param("boardType") PostBoardType boardType,
            Pageable pageable);
}
