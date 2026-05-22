package com.job_platfrom.demo.repository;

import com.job_platfrom.demo.Enum.ApplicationStatus;
import com.job_platfrom.demo.entity.Application;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByCandidateEmailAndJobId(String candidateEmail, Long jobId);

    List<Application> findByCandidateEmailOrderByCreatedAtDesc(String candidateEmail);

    List<Application> findAllByOrderByCreatedAtDesc();

    List<Application> findByJobIdOrderByCreatedAtDesc(Long jobId);

    long countByCandidateEmail(String candidateEmail);

    long countByCandidateEmailAndStatus(String candidateEmail, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    long countByJobId(Long jobId);

    long countByJobIdAndStatus(Long jobId, ApplicationStatus status);
}
