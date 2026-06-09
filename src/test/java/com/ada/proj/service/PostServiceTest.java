package com.ada.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.entity.Post;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.repository.PostBookmarkRepository;
import com.ada.proj.repository.PostEmojiReactionRepository;
import com.ada.proj.repository.PostLikeRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

class PostServiceTest {

    private static final String POST_SEQ_LOCK = "ada_posts_seq";

    private final PostRepository postRepository = org.mockito.Mockito.mock(PostRepository.class);
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final PostLikeRepository postLikeRepository = org.mockito.Mockito.mock(PostLikeRepository.class);
    private final PostBookmarkRepository postBookmarkRepository = org.mockito.Mockito.mock(PostBookmarkRepository.class);
    private final PostEmojiReactionRepository postEmojiReactionRepository = org.mockito.Mockito.mock(PostEmojiReactionRepository.class);
    private final UserBanService userBanService = org.mockito.Mockito.mock(UserBanService.class);
    private final PollService pollService = org.mockito.Mockito.mock(PollService.class);
    private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
    private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);

    private final PostService postService = new PostService(
            postRepository,
            userRepository,
            postLikeRepository,
            postBookmarkRepository,
            postEmojiReactionRepository,
            userBanService,
            pollService,
            jdbcTemplate,
            redisTemplate
    );

    @Test
    void createCommunityAllocatesSeqBeforeSavingPost() {
        PostCreateRequest request = new PostCreateRequest();
        request.setWriterUuid("writer-uuid");
        request.setTitle("React question");
        request.setCommunityCategory(CommunityCategory.CHAT);

        when(userRepository.findByUuid("writer-uuid")).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForObject(eq("SELECT GET_LOCK(?, 10)"), eq(Integer.class), eq(POST_SEQ_LOCK)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT COALESCE(MAX(seq), 0) + 1 FROM posts"), eq(Long.class)))
                .thenReturn(42L);
        when(postRepository.saveAndFlush(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Long createdSeq = postService.createCommunity(request);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(postCaptor.capture());
        assertEquals(42L, createdSeq);
        assertEquals(42L, postCaptor.getValue().getSeq());
        verify(jdbcTemplate).queryForObject(eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class), eq(POST_SEQ_LOCK));
    }

    @Test
    void createCommunityFailsBeforeSavingWhenSeqLockCannotBeAcquired() {
        PostCreateRequest request = new PostCreateRequest();
        request.setWriterUuid("writer-uuid");
        request.setTitle("React question");
        request.setCommunityCategory(CommunityCategory.CHAT);

        when(userRepository.findByUuid("writer-uuid")).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForObject(eq("SELECT GET_LOCK(?, 10)"), eq(Integer.class), eq(POST_SEQ_LOCK)))
                .thenReturn(0);

        assertThrows(IllegalStateException.class, () -> postService.createCommunity(request));
        verify(postRepository, never()).saveAndFlush(any(Post.class));
    }
}
