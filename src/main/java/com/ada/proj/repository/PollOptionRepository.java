package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.PollOption;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    Optional<PollOption> findByIdAndPoll_Id(Long id, Long pollId);

    @Modifying
    @Query("update PollOption o set o.voteCount = o.voteCount + 1 where o.id = :id")
    void incrementVoteCount(@Param("id") Long id);

    @Modifying
    @Query("update PollOption o set o.voteCount = case when o.voteCount > 0 then o.voteCount - 1 else 0 end where o.id = :id")
    void decrementVoteCount(@Param("id") Long id);
}
