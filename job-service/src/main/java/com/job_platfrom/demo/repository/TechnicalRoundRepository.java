package com.job_platfrom.demo.repository;

import com.job_platfrom.demo.entity.TechnicalRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicalRoundRepository extends JpaRepository<TechnicalRound, Long> {

    List<TechnicalRound> findByJobId(Long jobId);

    List<TechnicalRound> findByJobIdOrderBySequenceOrderAsc(Long jobId);

    void deleteByJobId(Long jobId);
}
