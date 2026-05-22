package com.job_platfrom.demo.repository;

import com.job_platfrom.demo.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {

    List<InterviewRound> findByJobId(Long jobId);

    List<InterviewRound> findByJobIdOrderBySequenceOrderAsc(Long jobId);

    void deleteByJobId(Long jobId);
}
