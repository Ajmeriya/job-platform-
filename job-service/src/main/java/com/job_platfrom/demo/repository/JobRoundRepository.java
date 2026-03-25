package com.job_platfrom.demo.repository;

import com.job_platfrom.demo.entity.JobRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRoundRepository extends JpaRepository<JobRound,Long> {

    List<JobRound> findByJobId(Long jobId);

    List<JobRound> findByJobIdOrderBySequenceOrderAsc(Long jobId);

    void deleteByJobId(Long jobId);
}
