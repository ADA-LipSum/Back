package com.ada.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PollCreateRequest;
import com.ada.proj.dto.PollOptionResponse;
import com.ada.proj.dto.PollResponse;
import com.ada.proj.dto.PollVoteRequest;
import com.ada.proj.dto.PollVoterResponse;
import com.ada.proj.entity.Poll;
import com.ada.proj.entity.PollOption;
import com.ada.proj.entity.PollVote;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.User;
import com.ada.proj.enums.NotificationType;
import com.ada.proj.repository.PollOptionRepository;
import com.ada.proj.repository.PollRepository;
import com.ada.proj.repository.PollVoteRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserBanService userBanService;

    @Transactional
    public PollResponse createPoll(Post post, PollCreateRequest request) {
        Objects.requireNonNull(post, "post");
        validateCreateRequest(request);
        if (pollRepository.existsByPost_PostUuid(post.getPostUuid())) {
            throw new IllegalStateException("이미 투표가 생성된 게시글입니다.");
        }

        Poll poll = Poll.builder()
                .post(post)
                .question(request.getQuestion().trim())
                .anonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .endsAt(request.getEndsAt())
                .build();

        for (String optionText : normalizedOptions(request.getOptions())) {
            poll.addOption(PollOption.builder()
                    .text(optionText)
                    .build());
        }

        Poll saved = pollRepository.saveAndFlush(poll);
        return toResponse(saved, null);
    }

    @Transactional
    public PollResponse updatePoll(Post post, PollCreateRequest request) {
        Objects.requireNonNull(post, "post");
        validateCreateRequest(request);

        Poll poll = pollRepository.findByPost(post)
                .orElseThrow(() -> new EntityNotFoundException("Poll not found for post: " + post.getPostUuid()));
        if (isEnded(poll)) {
            throw new IllegalStateException("종료된 투표는 수정할 수 없습니다.");
        }
        if (pollVoteRepository.countByPoll_Id(poll.getId()) > 0) {
            throw new IllegalStateException("참여자가 있는 투표는 선택지를 수정할 수 없습니다.");
        }

        poll.setQuestion(request.getQuestion().trim());
        poll.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));
        poll.setEndsAt(request.getEndsAt());
        poll.getOptions().clear();
        for (String optionText : normalizedOptions(request.getOptions())) {
            poll.addOption(PollOption.builder()
                    .text(optionText)
                    .build());
        }

        Poll saved = pollRepository.saveAndFlush(poll);
        return toResponse(saved, null);
    }

    @Transactional
    public void deleteByPost(Post post) {
        pollRepository.findByPost(post).ifPresent(pollRepository::delete);
    }

    @Transactional(readOnly = true)
    public PollResponse getByPostUuid(String postUuid, Authentication auth) {
        Poll poll = pollRepository.findByPost_PostUuid(postUuid)
                .orElseThrow(() -> new EntityNotFoundException("Poll not found for post: " + postUuid));
        return toResponse(poll, auth == null ? null : auth.getName());
    }

    @Transactional(readOnly = true)
    public PollResponse findResponseByPostUuid(String postUuid) {
        return pollRepository.findByPost_PostUuid(postUuid)
                .map(poll -> toResponse(poll, null))
                .orElse(null);
    }

    @Transactional
    public PollResponse vote(String postUuid, PollVoteRequest request, Authentication auth) {
        User voter = getCurrentUser(auth);
        userBanService.checkUserBanned(voter);

        Poll poll = pollRepository.findByPost_PostUuid(postUuid)
                .orElseThrow(() -> new EntityNotFoundException("Poll not found for post: " + postUuid));
        if (isEnded(poll)) {
            poll.setEnded(true);
            throw new IllegalStateException("이미 종료된 투표입니다.");
        }

        Long optionId = Objects.requireNonNull(request.getOptionId(), "optionId is required");
        PollOption selected = pollOptionRepository.findByIdAndPoll_Id(optionId, poll.getId())
                .orElseThrow(() -> new IllegalArgumentException("투표 선택지를 찾을 수 없습니다."));

        PollVote vote = pollVoteRepository.findByPollAndVoterUuid(poll, voter.getUuid()).orElse(null);
        if (vote == null) {
            vote = PollVote.builder()
                    .poll(poll)
                    .option(selected)
                    .voterUuid(voter.getUuid())
                    .voterName(displayName(voter))
                    .build();
            pollVoteRepository.save(vote);
            selected.setVoteCount(selected.getVoteCount() + 1);
        } else if (!Objects.equals(vote.getOption().getId(), selected.getId())) {
            PollOption previous = vote.getOption();
            previous.setVoteCount(Math.max(previous.getVoteCount() - 1, 0));
            selected.setVoteCount(selected.getVoteCount() + 1);
            vote.setOption(selected);
            vote.setVoterName(displayName(voter));
        }

        return toResponse(poll, voter.getUuid());
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void closeEndedPolls() {
        LocalDateTime now = LocalDateTime.now();
        for (Poll poll : pollRepository.findByEndsAtBeforeAndEndedNotifiedFalse(now)) {
            poll.setEnded(true);
            notifyPollEnded(poll);
            poll.setEndedNotified(true);
        }
    }

    private void notifyPollEnded(Poll poll) {
        Post post = poll.getPost();
        Set<String> recipients = new LinkedHashSet<>();
        if (post.getWriterUuid() != null && !post.getWriterUuid().isBlank()) {
            recipients.add(post.getWriterUuid());
        }
        recipients.addAll(pollVoteRepository.findDistinctVoterUuidsByPollId(poll.getId()));

        String title = "투표 종료";
        String message = "'" + post.getTitle() + "' 투표가 종료되었습니다.";
        for (String recipientUuid : recipients) {
            notificationService.create(
                    recipientUuid,
                    NotificationType.POLL_ENDED,
                    title,
                    message,
                    post.getPostUuid());
        }
    }

    private PollResponse toResponse(Poll poll, String currentUserUuid) {
        boolean ended = isEnded(poll);
        List<PollVote> votes = pollVoteRepository.findByPoll(poll);
        Map<Long, List<PollVoterResponse>> votersByOption = new LinkedHashMap<>();
        Long myOptionId = null;

        for (PollVote vote : votes) {
            Long optionId = vote.getOption().getId();
            if (Objects.equals(vote.getVoterUuid(), currentUserUuid)) {
                myOptionId = optionId;
            }
            if (!poll.isAnonymous()) {
                votersByOption.computeIfAbsent(optionId, ignored -> new ArrayList<>())
                        .add(PollVoterResponse.builder()
                                .voterUuid(vote.getVoterUuid())
                                .voterName(vote.getVoterName())
                                .build());
            }
        }

        List<PollOptionResponse> options = poll.getOptions().stream()
                .map(option -> PollOptionResponse.builder()
                        .id(option.getId())
                        .text(option.getText())
                        .voteCount(option.getVoteCount())
                        .voters(poll.isAnonymous()
                                ? List.of()
                                : votersByOption.getOrDefault(option.getId(), List.of()))
                        .build())
                .toList();

        return PollResponse.builder()
                .id(poll.getId())
                .postUuid(poll.getPost().getPostUuid())
                .question(poll.getQuestion())
                .anonymous(poll.isAnonymous())
                .endsAt(poll.getEndsAt())
                .ended(ended)
                .totalVotes(votes.size())
                .myOptionId(myOptionId)
                .options(options)
                .build();
    }

    private boolean isEnded(Poll poll) {
        return poll.isEnded() || !poll.getEndsAt().isAfter(LocalDateTime.now());
    }

    private void validateCreateRequest(PollCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("투표 정보가 필요합니다.");
        }
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new IllegalArgumentException("투표 질문을 입력해 주세요.");
        }
        if (request.getEndsAt() == null || !request.getEndsAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("투표 종료 시각은 현재 이후로 지정해 주세요.");
        }
        normalizedOptions(request.getOptions());
    }

    private List<String> normalizedOptions(List<String> options) {
        if (options == null) {
            throw new IllegalArgumentException("투표 선택지는 2개 이상 필요합니다.");
        }

        List<String> normalized = options.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (normalized.size() < 2 || normalized.size() > 10) {
            throw new IllegalArgumentException("투표 선택지는 2개 이상 10개 이하로 입력해 주세요.");
        }
        return normalized;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        return userRepository.findByUuid(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + auth.getName()));
    }

    private String displayName(User user) {
        return user.isUseNickname() ? user.getUserNickname() : user.getUserRealname();
    }
}
