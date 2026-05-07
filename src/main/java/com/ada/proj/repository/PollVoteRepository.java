package com.ada.proj.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.Poll;
import com.ada.proj.entity.PollVote;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    @EntityGraph(attributePaths = {"option"})
    Optional<PollVote> findByPollAndVoterUuid(Poll poll, String voterUuid);

    @EntityGraph(attributePaths = {"option"})
    List<PollVote> findByPoll(Poll poll);

    @Query("select distinct v.voterUuid from PollVote v where v.poll.id = :pollId")
    List<String> findDistinctVoterUuidsByPollId(@Param("pollId") Long pollId);

    long countByPoll_Id(Long pollId);

    List<PollVote> findByPoll_IdIn(Collection<Long> pollIds);
}
