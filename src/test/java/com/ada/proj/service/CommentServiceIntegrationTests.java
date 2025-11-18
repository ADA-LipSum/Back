package com.ada.proj.service;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.CommentCreateRequest;
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class CommentServiceIntegrationTests {

    @Autowired
    CommentService commentService;
    @Autowired
    PostRepository postRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CommentRepository commentRepository;

    User user;
    Post post;

    @BeforeEach
    void setUp() {
        // 사용자 준비
        user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .adminId("admin_" + System.nanoTime())
                .customId("custom_" + System.nanoTime())
                .password("$2a$10$dummyhashdummyhashdummyhashdummyha")
                .userRealname("실명")
                .userNickname("닉네임")
                .role(Role.STUDENT)
                .useNickname(true)
                .build();
        user = userRepository.save(user);

        // 인증 컨텍스트 설정
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user.getUuid(),
                "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
            )
        );

        // 게시글 준비
        post = Post.builder()
                .writerUuid(user.getUuid())
                .title("테스트 제목")
                .writer(user.isUseNickname() ? user.getUserNickname() : user.getUserRealname())
                .build();
        post = postRepository.save(post);
    }

    @Test
    @DisplayName("루트 댓글 + 대댓글 작성 시 Post.comments 카운트 증가 및 children 포함")
    void createRootAndReply_updatesCountAndChildren() {
        // 루트 댓글 작성
        CommentCreateRequest rootReq = new CommentCreateRequest();
        rootReq.setPostId(post.getPostUuid());
        rootReq.setContent("첫 댓글");
        CommentResponse rootResp = commentService.createComment(rootReq);

        Post afterRoot = postRepository.findById(post.getPostUuid()).orElseThrow();
        assertThat(afterRoot.getComments()).isEqualTo(1);

        // 대댓글 작성
        CommentCreateRequest replyReq = new CommentCreateRequest();
        replyReq.setPostId(post.getPostUuid());
        replyReq.setParentId(rootResp.getCommentId());
        replyReq.setContent("대댓글");
        CommentResponse replyResp = commentService.createComment(replyReq);

        Post afterReply = postRepository.findById(post.getPostUuid()).orElseThrow();
        assertThat(afterReply.getComments()).isEqualTo(2);

        // 조회 시 children 포함 확인
        List<CommentResponse> roots = commentService.getCommentsByPost(post.getPostUuid());
        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getChildren()).hasSize(1);
        assertThat(roots.get(0).getChildren().get(0).getCommentId()).isEqualTo(replyResp.getCommentId());
    }

    @Test
    @DisplayName("댓글/대댓글 삭제 시 카운트 감소")
    void deleteComments_updatesCount() {
        // two comments: root + child
        CommentCreateRequest rootReq = new CommentCreateRequest();
        rootReq.setPostId(post.getPostUuid());
        rootReq.setContent("첫 댓글");
        CommentResponse rootResp = commentService.createComment(rootReq);

        CommentCreateRequest replyReq = new CommentCreateRequest();
        replyReq.setPostId(post.getPostUuid());
        replyReq.setParentId(rootResp.getCommentId());
        replyReq.setContent("대댓글");
        CommentResponse replyResp = commentService.createComment(replyReq);

        Post afterCreate = postRepository.findById(post.getPostUuid()).orElseThrow();
        assertThat(afterCreate.getComments()).isEqualTo(2);

        // 대댓글 삭제
        commentService.deleteComment(replyResp.getCommentId());
        Post afterDeleteReply = postRepository.findById(post.getPostUuid()).orElseThrow();
        assertThat(afterDeleteReply.getComments()).isEqualTo(1);

        // 루트 댓글 삭제 (자식 이미 삭제된 상태)
        commentService.deleteComment(rootResp.getCommentId());
        Post afterDeleteRoot = postRepository.findById(post.getPostUuid()).orElseThrow();
        assertThat(afterDeleteRoot.getComments()).isZero();
    }
}
