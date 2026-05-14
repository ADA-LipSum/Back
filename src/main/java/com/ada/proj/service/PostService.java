package com.ada.proj.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.PostBookmark;
import com.ada.proj.entity.PostLike;
import com.ada.proj.entity.User;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;
import com.ada.proj.repository.PostBookmarkRepository;
import com.ada.proj.repository.PostLikeRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*]\\(([^\\s)]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern HTML_IMAGE = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final String POST_SEQ_LOCK = "ada_posts_seq";

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final UserBanService userBanService;
    private final PollService pollService;
    private final JdbcTemplate jdbcTemplate;

    private Post getPostByUuidOrThrow(@NonNull String uuid) {
        return postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));
    }

    private Post getPostBySeqOrThrow(@NonNull Long seq) {
        return postRepository.findBySeq(seq)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + seq));
    }

    @Transactional(readOnly = true)
    public String findUuidBySeq(@NonNull Long seq) {
        return getPostBySeqOrThrow(seq).getPostUuid();
    }

    private boolean hasAdminRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga != null && "ROLE_ADMIN".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private void ensureWriterOrAdmin(Post post, Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        if (hasAdminRole(auth)) {
            return;
        }
        if (!Objects.equals(post.getWriterUuid(), auth.getName())) {
            throw new AccessDeniedException("작성자 또는 관리자만 수정/삭제할 수 있습니다.");
        }
    }

    @Transactional
    public Long create(@NonNull PostCreateRequest req) {
        return createInternal(req, false, null);
    }

    @Transactional
    public Long createCommunity(@NonNull PostCreateRequest req) {
        return createInternal(req, true, PostBoardType.COMMUNITY);
    }

    @Transactional
    public Long createBlog(@NonNull PostCreateRequest req) {
        return createInternal(req, true, PostBoardType.BLOG);
    }

    @Transactional
    public Long createNotice(@NonNull PostCreateRequest req) {
        return createInternal(req, true, PostBoardType.NOTICE);
    }

    private Long createInternal(PostCreateRequest req, boolean strict, PostBoardType forcedBoardType) {
        String writerUuid = resolveWriterUuid(req.getWriterUuid());
        User writerUser = null;
        String writerName = null;
        if (writerUuid != null && !writerUuid.isBlank()) {
            writerUser = userRepository.findByUuid(writerUuid).orElse(null);
            if (writerUser != null) {
                userBanService.checkUserBanned(writerUser);
                writerName = displayName(writerUser);
            }
        }

        ResolvedPostMeta meta = resolveCreateMeta(req, strict, forcedBoardType);
        Long seq = allocatePostSeq();

        Post post = Post.builder()
                .seq(seq)
                .writerUuid(writerUuid)
                .title(req.getTitle())
                .content(req.getContent())
                .images(req.getImages())
                .videos(req.getVideos())
                .writer(writerName)
                .boardType(meta.boardType())
                .communityCategory(meta.communityCategory())
                .techSubTag(meta.techSubTag())
                .isDev(meta.isDev())
                .devTags(meta.techTagsCsv())
                .thumbnailImage(meta.thumbnailImage())
                .build();

        Post saved = postRepository.saveAndFlush(post);
        if (meta.shouldCreatePoll()) {
            pollService.createPoll(saved, req.getPoll());
        }

        return saved.getSeq();
    }

    private Long allocatePostSeq() {
        Integer locked = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 10)", Integer.class, POST_SEQ_LOCK);
        if (locked == null || locked != 1) {
            throw new IllegalStateException("Could not acquire post seq lock");
        }

        boolean releaseRegistered = false;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        releasePostSeqLock();
                    }
                });
                releaseRegistered = true;
            }

            Long nextSeq = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(seq), 0) + 1 FROM posts",
                    Long.class
            );
            if (nextSeq == null || nextSeq <= 0) {
                throw new IllegalStateException("Could not allocate post seq");
            }
            return nextSeq;
        } finally {
            if (!releaseRegistered) {
                releasePostSeqLock();
            }
        }
    }

    private void releasePostSeqLock() {
        try {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, POST_SEQ_LOCK);
        } catch (RuntimeException ex) {
            log.warn("Failed to release post seq lock: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));
        Page<PostSummaryResponse> result = postRepository.findSummaryPage(pageable)
                .map(this::completeSummary);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> search(
            PostBoardType boardType,
            CommunityCategory category,
            TechSubTag techSubTag,
            String techTag,
            String query,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));
        boolean includeLegacyCommunity = boardType == PostBoardType.COMMUNITY;
        Page<PostSummaryResponse> result = postRepository.searchSummaries(
                        boardType,
                        category,
                        techSubTag,
                        blankToNull(techTag),
                        blankToNull(query),
                        includeLegacyCommunity,
                        pageable)
                .map(this::completeSummary);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> listNotices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));
        Page<NoticeSummaryResponse> result = postRepository.findNoticeSummaries(pageable);
        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent()
        );
    }

    @Transactional
    public PostDetailResponse detail(@NonNull Long seq) {
        return detail(seq, (String) null);
    }

    @Transactional
    public PostDetailResponse detail(@NonNull Long seq, String requesterUuid) {
        Post post = getPostBySeqOrThrow(seq);
        postRepository.increaseViews(post.getPostUuid());

        User user = userRepository.findByUuid(post.getWriterUuid()).orElse(null);
        boolean isLiked = requesterUuid != null
                && postLikeRepository.existsByUserUuidAndPostUuid(requesterUuid, post.getPostUuid());
        boolean isBookmarked = requesterUuid != null
                && postBookmarkRepository.existsByUserUuidAndPostUuid(requesterUuid, post.getPostUuid());

        return PostDetailResponse.builder()
                .postUuid(post.getPostUuid())
                .seq(post.getSeq())
                .writerUuid(post.getWriterUuid())
                .writerCustomId(user != null ? user.getCustomId() : null)
                .writer(post.getWriter())
                .writerProfileImage(user != null ? user.getProfileImage() : null)
                .title(post.getTitle())
                .content(post.getContent())
                .images(post.getImages())
                .videos(post.getVideos())
                .thumbnailImage(post.getThumbnailImage())
                .writedAt(post.getWritedAt())
                .updatedAt(post.getUpdatedAt())
                .likes(post.getLikes())
                .views(post.getViews())
                .comments(post.getComments())
                .isDev(post.getIsDev())
                .devTags(post.getDevTags())
                .techTags(splitTags(post.getDevTags()))
                .boardType(resolveBoardType(post.getBoardType()))
                .communityCategory(post.getCommunityCategory())
                .techSubTag(post.getTechSubTag())
                .poll(pollService.findResponseByPostUuid(post.getPostUuid()))
                .isLiked(isLiked)
                .isBookmarked(isBookmarked)
                .build();
    }

    @Transactional
    public void update(@NonNull Long seq, @NonNull PostUpdateRequest req, Authentication auth) {
        updateInternal(seq, req, auth, false, null);
    }

    @Transactional
    public void updateCommunity(@NonNull Long seq, @NonNull PostUpdateRequest req, Authentication auth) {
        updateInternal(seq, req, auth, true, PostBoardType.COMMUNITY);
    }

    @Transactional
    public void updateBlog(@NonNull Long seq, @NonNull PostUpdateRequest req, Authentication auth) {
        updateInternal(seq, req, auth, true, PostBoardType.BLOG);
    }

    @Transactional
    public void updateNotice(@NonNull Long seq, @NonNull PostUpdateRequest req, Authentication auth) {
        updateInternal(seq, req, auth, true, PostBoardType.NOTICE);
    }

    private void updateInternal(
            Long seq,
            PostUpdateRequest req,
            Authentication auth,
            boolean strict,
            PostBoardType forcedBoardType
    ) {
        Post post = getPostBySeqOrThrow(seq);
        ensureWriterOrAdmin(post, auth);

        if (req.getTitle() != null) {
            post.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            post.setContent(req.getContent());
        }
        if (req.getImages() != null) {
            post.setImages(req.getImages());
        }
        if (req.getVideos() != null) {
            post.setVideos(req.getVideos());
        }

        ResolvedPostMeta meta = resolveUpdateMeta(post, req, strict, forcedBoardType);
        post.setBoardType(meta.boardType());
        post.setCommunityCategory(meta.communityCategory());
        post.setTechSubTag(meta.techSubTag());
        post.setIsDev(meta.isDev());
        post.setDevTags(meta.techTagsCsv());
        post.setThumbnailImage(meta.thumbnailImage());

        if (meta.isPollPost()) {
            boolean hasPoll = pollService.findResponseByPostUuid(post.getPostUuid()) != null;
            if (req.getPoll() != null && hasPoll) {
                pollService.updatePoll(post, req.getPoll());
            } else if (req.getPoll() != null) {
                pollService.createPoll(post, req.getPoll());
            } else if (strict && !hasPoll) {
                throw new IllegalArgumentException("투표 게시글에는 투표 정보가 필요합니다.");
            }
        } else {
            if (req.getPoll() != null) {
                throw new IllegalArgumentException("투표 정보는 커뮤니티 기술/투표 게시글에서만 사용할 수 있습니다.");
            }
            pollService.deleteByPost(post);
        }
    }

    @Transactional
    public void delete(@NonNull Long seq, Authentication auth) {
        Post post = getPostBySeqOrThrow(seq);
        ensureWriterOrAdmin(post, auth);
        pollService.deleteByPost(post);
        postRepository.deleteById(post.getPostUuid());
    }

    @Transactional
    public boolean toggleLike(@NonNull String userUuid, @NonNull Long seq) {
        Post post = getPostBySeqOrThrow(seq);
        String postUuid = post.getPostUuid();

        boolean alreadyLiked = postLikeRepository.existsByUserUuidAndPostUuid(userUuid, postUuid);

        if (alreadyLiked) {
            postLikeRepository.deleteByUserUuidAndPostUuid(userUuid, postUuid);
            postRepository.decreaseLikes(postUuid);
            return false;
        }

        PostLike like = PostLike.builder()
                .userUuid(userUuid)
                .postUuid(postUuid)
                .build();
        postLikeRepository.save(Objects.requireNonNull(like));
        postRepository.increaseLikes(postUuid);
        return true;
    }

    @Transactional
    public boolean toggleBookmark(@NonNull String userUuid, @NonNull Long seq) {
        Post post = getPostBySeqOrThrow(seq);
        String postUuid = post.getPostUuid();

        if (postBookmarkRepository.existsByUserUuidAndPostUuid(userUuid, postUuid)) {
            postBookmarkRepository.deleteByUserUuidAndPostUuid(userUuid, postUuid);
            return false;
        }

        postBookmarkRepository.save(PostBookmark.builder()
                .userUuid(userUuid)
                .postUuid(postUuid)
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getMyBookmarks(
            @NonNull String userUuid,
            @NonNull PostBoardType boardType,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));
        Page<PostSummaryResponse> result = postRepository
                .findBookmarkedSummaries(userUuid, boardType, pageable)
                .map(this::completeSummary);
        return toPageResponse(result);
    }

    @Transactional
    public void deleteLikeById(@NonNull Long likeId, @NonNull String userUuid) {
        PostLike like = postLikeRepository.findById(likeId)
                .orElseThrow(() -> new IllegalArgumentException("Like not found: " + likeId));

        if (!like.getUserUuid().equals(userUuid)) {
            throw new AccessDeniedException("본인의 좋아요만 취소할 수 있습니다.");
        }

        String postUuid = Objects.requireNonNull(like.getPostUuid(), "postUuid is required");
        if (!postRepository.existsById(postUuid)) {
            throw new EntityNotFoundException("Post not found: " + postUuid);
        }

        postLikeRepository.deleteById(likeId);
        postRepository.decreaseLikes(postUuid);
    }

    private ResolvedPostMeta resolveCreateMeta(PostCreateRequest req, boolean strict, PostBoardType forcedBoardType) {
        PostBoardType boardType = forcedBoardType != null
                ? forcedBoardType
                : req.getBoardType() != null ? req.getBoardType() : PostBoardType.COMMUNITY;

        CommunityCategory category = req.getCommunityCategory();
        TechSubTag techSubTag = req.getTechSubTag();
        String techTagsCsv = normalizeTags(req.getTechTags(), req.getDevTags());
        String thumbnail = firstNonBlank(req.getThumbnailImage(), null);

        if (boardType == PostBoardType.BLOG && thumbnail == null) {
            thumbnail = firstNonBlank(extractFirstImage(req.getContent()), firstCsvValue(req.getImages()));
        }

        return normalizeMeta(
                boardType,
                category,
                techSubTag,
                techTagsCsv,
                thumbnail,
                Boolean.TRUE.equals(req.getIsDev()),
                req.getPoll() != null,
                strict,
                true
        );
    }

    private ResolvedPostMeta resolveUpdateMeta(Post post, PostUpdateRequest req, boolean strict, PostBoardType forcedBoardType) {
        PostBoardType boardType = forcedBoardType != null
                ? forcedBoardType
                : req.getBoardType() != null ? req.getBoardType() : resolveBoardType(post.getBoardType());
        CommunityCategory category = req.getCommunityCategory() != null ? req.getCommunityCategory() : post.getCommunityCategory();
        TechSubTag techSubTag = req.getTechSubTag() != null ? req.getTechSubTag() : post.getTechSubTag();
        String techTagsCsv = req.getTechTags() != null || req.getDevTags() != null
                ? normalizeTags(req.getTechTags(), req.getDevTags())
                : post.getDevTags();

        String contentForThumbnail = req.getContent() != null ? req.getContent() : post.getContent();
        String imagesForThumbnail = req.getImages() != null ? req.getImages() : post.getImages();
        String thumbnail = req.getThumbnailImage() != null
                ? blankToNull(req.getThumbnailImage())
                : post.getThumbnailImage();
        if (boardType == PostBoardType.BLOG && (thumbnail == null || req.getContent() != null)) {
            thumbnail = firstNonBlank(extractFirstImage(contentForThumbnail), firstCsvValue(imagesForThumbnail), thumbnail);
        }

        boolean isDev = req.getIsDev() != null ? req.getIsDev() : Boolean.TRUE.equals(post.getIsDev());

        return normalizeMeta(
                boardType,
                category,
                techSubTag,
                techTagsCsv,
                thumbnail,
                isDev,
                req.getPoll() != null,
                strict,
                false
        );
    }

    private ResolvedPostMeta normalizeMeta(
            PostBoardType boardType,
            CommunityCategory category,
            TechSubTag techSubTag,
            String techTagsCsv,
            String thumbnail,
            boolean isDev,
            boolean pollProvided,
            boolean strict,
            boolean requirePollOnPollPost
    ) {
        if (boardType == PostBoardType.COMMUNITY) {
            CommunityCategory resolvedCategory = category != null
                    ? category
                    : isDev ? CommunityCategory.TECH : CommunityCategory.CHAT;

            if (resolvedCategory == CommunityCategory.TECH) {
                TechSubTag resolvedSubTag = techSubTag;
                if (resolvedSubTag == null) {
                    if (strict) {
                        throw new IllegalArgumentException("기술 태그 선택 시 하위 태그를 반드시 1개 선택해야 합니다.");
                    }
                    resolvedSubTag = TechSubTag.CHAT;
                }
                if (resolvedSubTag == TechSubTag.POLL && strict && !pollProvided && requirePollOnPollPost) {
                    throw new IllegalArgumentException("투표 게시글에는 투표 정보가 필요합니다.");
                }
                return new ResolvedPostMeta(
                        boardType,
                        resolvedCategory,
                        resolvedSubTag,
                        techTagsCsv,
                        thumbnail,
                        true,
                        resolvedSubTag == TechSubTag.POLL,
                        resolvedSubTag == TechSubTag.POLL && pollProvided
                );
            }

            if (pollProvided) {
                throw new IllegalArgumentException("투표 정보는 커뮤니티 기술/투표 게시글에서만 사용할 수 있습니다.");
            }
            return new ResolvedPostMeta(boardType, resolvedCategory, null, null, thumbnail, false, false, false);
        }

        if (pollProvided) {
            throw new IllegalArgumentException("투표 정보는 커뮤니티 기술/투표 게시글에서만 사용할 수 있습니다.");
        }
        if (boardType == PostBoardType.BLOG) {
            boolean hasTechTags = techTagsCsv != null && !techTagsCsv.isBlank();
            return new ResolvedPostMeta(boardType, null, null, techTagsCsv, thumbnail, hasTechTags, false, false);
        }
        return new ResolvedPostMeta(PostBoardType.NOTICE, null, null, null, null, false, false, false);
    }

    private String resolveWriterUuid(String requestWriterUuid) {
        if (requestWriterUuid != null && !requestWriterUuid.isBlank()) {
            return requestWriterUuid;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    private PostSummaryResponse completeSummary(PostSummaryResponse dto) {
        PostBoardType boardType = resolveBoardType(dto.getBoardType());
        dto.setBoardType(boardType);
        dto.setTechTags(splitTags(dto.getDevTags()));
        dto.setTag(readableTag(boardType, dto.getCommunityCategory(), dto.getTechSubTag(), dto.getDevTags()));
        dto.setPostId(dto.getSeq());
        return dto;
    }

    private String readableTag(PostBoardType boardType, CommunityCategory category, TechSubTag techSubTag, String tags) {
        if (boardType == PostBoardType.BLOG) {
            return "블로그";
        }
        if (boardType == PostBoardType.NOTICE) {
            return "공지사항";
        }
        if (category == CommunityCategory.TECH) {
            String sub = switch (techSubTag == null ? TechSubTag.CHAT : techSubTag) {
                case QUESTION -> "질문";
                case CHAT -> "잡담";
                case TIP -> "팁";
                case POLL -> "투표";
            };
            return tags != null && !tags.isBlank() ? "기술/" + sub + "(" + tags + ")" : "기술/" + sub;
        }
        if (category == CommunityCategory.MEME) {
            return "밈";
        }
        if (category == CommunityCategory.PROJECT_SHOWCASE) {
            return "프로젝트 자랑";
        }
        return "잡담";
    }

    private PostBoardType resolveBoardType(PostBoardType boardType) {
        return boardType == null ? PostBoardType.COMMUNITY : boardType;
    }

    private PageResponse<PostSummaryResponse> toPageResponse(Page<PostSummaryResponse> result) {
        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent()
        );
    }

    private String normalizeTags(List<String> tags, String csv) {
        Set<String> values = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(values::add);
        } else if (csv != null) {
            Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(values::add);
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    private List<String> splitTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String extractFirstImage(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher markdown = MARKDOWN_IMAGE.matcher(content);
        if (markdown.find()) {
            return markdown.group(1);
        }
        Matcher html = HTML_IMAGE.matcher(content);
        if (html.find()) {
            return html.group(1);
        }
        return null;
    }

    private String firstCsvValue(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String displayName(User user) {
        return user.isUseNickname() ? user.getUserNickname() : user.getUserRealname();
    }

    private record ResolvedPostMeta(
            PostBoardType boardType,
            CommunityCategory communityCategory,
            TechSubTag techSubTag,
            String techTagsCsv,
            String thumbnailImage,
            boolean isDev,
            boolean isPollPost,
            boolean shouldCreatePoll
    ) {
    }
}
