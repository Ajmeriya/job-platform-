package com.job_platfrom.demo.repository;

import com.job_platfrom.demo.entity.AptitudeRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AptitudeRoundRepository extends JpaRepository<AptitudeRound, Long> {

    List<AptitudeRound> findByJobId(Long jobId);

    List<AptitudeRound> findByJobIdOrderBySequenceOrderAsc(Long jobId);

    void deleteByJobId(Long jobId);
}
