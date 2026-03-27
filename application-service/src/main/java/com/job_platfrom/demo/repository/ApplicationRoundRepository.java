package com.job_platfrom.demo.repository;

import com.job_platfrom.demo.Enum.RoundType;
import com.job_platfrom.demo.entity.ApplicationRound;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRoundRepository extends JpaRepository<ApplicationRound, Long> {

    Optional<ApplicationRound> findByApplicationIdAndRoundType(Long applicationId, RoundType roundType);

    List<ApplicationRound> findByApplicationIdOrderByIdAsc(Long applicationId);
}
