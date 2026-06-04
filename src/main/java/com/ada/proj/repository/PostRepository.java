package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.dto.AdminPostSummaryResponse;
import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.entity.Post;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.NoticeCategory;
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
              and (
                :mediaFilter is null or :mediaFilter = 'ALL'
                or (:mediaFilter = 'PHOTO' and p.images is not null and p.images <> '')
                or (:mediaFilter = 'VIDEO' and p.videos is not null and p.videos <> '')
                or (:mediaFilter = 'TEXT' and (p.images is null or p.images = '') and (p.videos is null or p.videos = ''))
              )
              and (
                :excludeCategory is null
                or p.communityCategory is null
                or p.communityCategory <> :excludeCategory
              )
            """)
    Page<PostSummaryResponse> searchSummaries(
            @Param("boardType") PostBoardType boardType,
            @Param("category") CommunityCategory category,
            @Param("techSubTag") TechSubTag techSubTag,
            @Param("techTag") String techTag,
            @Param("query") String query,
            @Param("includeLegacyCommunity") boolean includeLegacyCommunity,
            @Param("mediaFilter") String mediaFilter,
            @Param("excludeCategory") CommunityCategory excludeCategory,
            Pageable pageable);

    // 공지사항 목록: 핀된 글은 pinnedAt 최신순 최상단, 그 이후 일반 글 writedAt 내림차순
    @Query(value = """
            select new com.ada.proj.dto.NoticeSummaryResponse(
                p.postUuid, p.seq, p.title, p.writer, u.profileImage,
                p.writedAt, p.views, p.noticeCategory, p.isPinned, p.pinnedAt
            )
            from Post p left join com.ada.proj.entity.User u on u.uuid = p.writerUuid
            where p.boardType = com.ada.proj.enums.PostBoardType.NOTICE
              and (:noticeCategory is null or p.noticeCategory = :noticeCategory)
              and (
                :query is null
                or lower(p.title) like lower(concat('%', :query, '%'))
                or p.content like concat('%', :query, '%')
              )
            order by
              case when p.isPinned = true then 0 else 1 end asc,
              case when p.isPinned = true then p.pinnedAt end desc,
              p.writedAt desc
            """,
            countQuery = """
            select count(p)
            from Post p
            where p.boardType = com.ada.proj.enums.PostBoardType.NOTICE
              and (:noticeCategory is null or p.noticeCategory = :noticeCategory)
              and (
                :query is null
                or lower(p.title) like lower(concat('%', :query, '%'))
                or p.content like concat('%', :query, '%')
              )
            """)
    Page<NoticeSummaryResponse> findNoticeSummaries(
            @Param("noticeCategory") NoticeCategory noticeCategory,
            @Param("query") String query,
            Pageable pageable);

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

    @Modifying
    @Query("update Post p set p.comments = p.comments + 1 where p.postUuid = :uuid")
    int increaseComments(@Param("uuid") String uuid);

    @Modifying
    @Query("""
           update Post p
              set p.comments = case when p.comments > 0 then p.comments - 1 else 0 end
            where p.postUuid = :uuid
           """)
    int decreaseComments(@Param("uuid") String uuid);

    java.util.Optional<Post> findBySeq(Long seq);

    @Query("select p.seq from Post p where p.postUuid = :uuid")
    Long findSeqByUuid(@Param("uuid") String uuid);

    long countByWriterUuid(String writerUuid);

    List<Post> findByWriterUuidOrderByWritedAtDesc(String writerUuid);

    @Query("""
            select new com.ada.proj.dto.AdminPostSummaryResponse(
                p.postUuid, p.writerUuid, p.seq, p.title, p.writer,
                p.writedAt, p.likes, p.views, p.comments,
                p.boardType, p.communityCategory, p.thumbnailImage
            )
            from Post p
            where (:writerUuid is null or p.writerUuid = :writerUuid)
              and (:query is null or lower(p.title) like lower(concat('%', :query, '%')))
            """)
    Page<AdminPostSummaryResponse> findAdminPostSummaries(
            @Param("writerUuid") String writerUuid,
            @Param("query") String query,
            Pageable pageable);

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

    // 개발 태그 인기 집계 (devTags가 있는 커뮤니티 글 기준)
    @Query(value = """
            select dev_tag, count(*) as cnt
            from (
                select trim(substring_index(substring_index(p.dev_tags, ',', n.n), ',', -1)) as dev_tag
                from posts p
                cross join (
                    select 1 as n union all select 2 union all select 3 union all
                    select 4 union all select 5 union all select 6 union all
                    select 7 union all select 8 union all select 9 union all select 10
                ) n
                where p.dev_tags is not null
                  and p.dev_tags <> ''
                  and p.board_type = 'COMMUNITY'
                  and length(p.dev_tags) - length(replace(p.dev_tags, ',', '')) >= n.n - 1
            ) tags
            where dev_tag <> ''
            group by dev_tag
            order by cnt desc
            limit :limit
            """,
            nativeQuery = true)
    List<Object[]> findTopDevTags(@Param("limit") int limit);

    // 사용자 활동 통계
    @Query("select count(p) from Post p where p.writerUuid = :uuid and p.boardType = com.ada.proj.enums.PostBoardType.COMMUNITY")
    long countCommunityPostsByWriter(@Param("uuid") String uuid);

    @Query("select coalesce(sum(p.likes), 0) from Post p where p.writerUuid = :uuid")
    long sumLikesByWriter(@Param("uuid") String uuid);
}
