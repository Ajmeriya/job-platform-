package com.job_platfrom.demo.service;

import com.job_platfrom.demo.Enum.ApplicationStatus;
import com.job_platfrom.demo.Enum.ResumeStatus;
import com.job_platfrom.demo.Enum.RoundStatus;
import com.job_platfrom.demo.Enum.RoundType;
import com.job_platfrom.demo.dto.ApplicationResponse;
import com.job_platfrom.demo.dto.ApplicationRoundResponse;
import com.job_platfrom.demo.dto.ApplicationRoundUpdateRequest;
import com.job_platfrom.demo.dto.ApplyRequest;
import com.job_platfrom.demo.dto.ResumeReviewUpdateRequest;
import com.job_platfrom.demo.entity.Application;
import com.job_platfrom.demo.entity.ApplicationRound;
import com.job_platfrom.demo.repository.ApplicationRepository;
import com.job_platfrom.demo.repository.ApplicationRoundRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationRoundRepository applicationRoundRepository;
    private final ResumeAnalysisKafkaService resumeAnalysisKafkaService;

    @Override
    @Transactional
    public ApplicationResponse apply(String candidateEmail, ApplyRequest request) {
        if (candidateEmail == null || candidateEmail.isBlank()) {
            throw new IllegalArgumentException("Candidate email is required");
        }
        if (request == null || request.getJobId() == null) {
            throw new IllegalArgumentException("jobId is required");
        }
        if (request.getResumeUrl() == null || request.getResumeUrl().isBlank()) {
            throw new IllegalArgumentException("resumeUrl is required");
        }

        if (applicationRepository.existsByCandidateEmailAndJobId(candidateEmail, request.getJobId())) {
            throw new IllegalArgumentException("You have already applied for this job");
        }

        Application application = Application.builder()
            .jobId(request.getJobId())
            .candidateEmail(candidateEmail)
            .resumeUrl(request.getResumeUrl())
            .status(ApplicationStatus.APPLIED)
            .currentRound(RoundType.APTITUDE)
            .build();

        Application saved = applicationRepository.save(application);
        resumeAnalysisKafkaService.queueAnalysis(saved);
        return toResponse(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String candidateEmail) {
        if (candidateEmail == null || candidateEmail.isBlank()) {
            throw new IllegalArgumentException("Candidate email is required");
        }

        return applicationRepository.findByCandidateEmailOrderByCreatedAtDesc(candidateEmail)
            .stream()
            .map(this::toResponseWithRounds)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponseWithRounds)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJobId(Long jobId) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("jobId must be a positive value");
        }

        return applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId)
            .stream()
            .map(this::toResponseWithRounds)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Application id is required");
        }

        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Application not found for id: " + id));

        return toResponseWithRounds(application);
    }

    @Override
    @Transactional
    public ApplicationResponse updateResumeReview(Long id, ResumeReviewUpdateRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Application id is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Application not found for id: " + id));

        if (request.getResumeScore() != null) {
            if (request.getResumeScore() < 0 || request.getResumeScore() > 100) {
                throw new IllegalArgumentException("resumeScore must be between 0 and 100");
            }
            application.setResumeScore(request.getResumeScore());
        }

        if (request.getResumeStatus() != null) {
            application.setResumeStatus(request.getResumeStatus());

            if (request.getResumeStatus() == ResumeStatus.REJECTED) {
                application.setStatus(ApplicationStatus.REJECTED);
                application.setCurrentRound(null);
            }
            if (request.getResumeStatus() == ResumeStatus.SHORTLISTED) {
                startFirstRound(application);
            }
        }

        Application saved = applicationRepository.save(application);
        return toResponseWithRounds(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse updateRound(Long id, ApplicationRoundUpdateRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Application id is required");
        }
        if (request == null || request.getRoundType() == null) {
            throw new IllegalArgumentException("roundType is required");
        }

        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Application not found for id: " + id));

        ApplicationRound round = applicationRoundRepository
            .findByApplicationIdAndRoundType(id, request.getRoundType())
            .orElseGet(() -> ApplicationRound.builder()
                .application(application)
                .roundType(request.getRoundType())
                .build());

        if (request.getStatus() != null) {
            round.setStatus(request.getStatus());
        }
        if (request.getScore() != null) {
            round.setScore(request.getScore());
        }
        if (request.getFeedback() != null) {
            round.setFeedback(request.getFeedback());
        }
        if (request.getStartedAt() != null) {
            round.setStartedAt(request.getStartedAt());
        }
        if (request.getCompletedAt() != null) {
            round.setCompletedAt(request.getCompletedAt());
        }

        ApplicationRound savedRound = applicationRoundRepository.save(round);

        if (savedRound.getStatus() != null) {
            applyStatusTransition(application, savedRound.getRoundType(), savedRound.getStatus());
            applicationRepository.save(application);
        }

        return toResponseWithRounds(application);
    }

    @Override
    @Transactional
    public ApplicationResponse withdrawApplication(String candidateEmail, Long applicationId) {
        if (candidateEmail == null || candidateEmail.isBlank()) {
            throw new IllegalArgumentException("Candidate email is required");
        }
        if (applicationId == null) {
            throw new IllegalArgumentException("Application id is required");
        }

        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Application not found for id: " + applicationId));

        if (!candidateEmail.equalsIgnoreCase(application.getCandidateEmail())) {
            throw new IllegalArgumentException("Not allowed to modify this application");
        }

        if (application.getStatus() == ApplicationStatus.SELECTED || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Application cannot be withdrawn in current status: " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setResumeStatus(ResumeStatus.REJECTED);
        application.setCurrentRound(null);

        Application saved = applicationRepository.save(application);
        return toResponseWithRounds(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getMyApplicationStats(String candidateEmail) {
        if (candidateEmail == null || candidateEmail.isBlank()) {
            throw new IllegalArgumentException("Candidate email is required");
        }

        long total = applicationRepository.countByCandidateEmail(candidateEmail);
        long applied = applicationRepository.countByCandidateEmailAndStatus(candidateEmail, ApplicationStatus.APPLIED);
        long inProgress = applicationRepository.countByCandidateEmailAndStatus(candidateEmail, ApplicationStatus.IN_PROGRESS);
        long selected = applicationRepository.countByCandidateEmailAndStatus(candidateEmail, ApplicationStatus.SELECTED);
        long rejected = applicationRepository.countByCandidateEmailAndStatus(candidateEmail, ApplicationStatus.REJECTED);

        return Map.of(
            "total", total,
            "applied", applied,
            "inProgress", inProgress,
            "selected", selected,
            "rejected", rejected
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getApplicationSummary(Long jobId) {
        long total;
        long applied;
        long inProgress;
        long selected;
        long rejected;

        if (jobId != null && jobId > 0) {
            total = applicationRepository.countByJobId(jobId);
            applied = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.APPLIED);
            inProgress = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.IN_PROGRESS);
            selected = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.SELECTED);
            rejected = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.REJECTED);
        } else {
            total = applicationRepository.count();
            applied = applicationRepository.countByStatus(ApplicationStatus.APPLIED);
            inProgress = applicationRepository.countByStatus(ApplicationStatus.IN_PROGRESS);
            selected = applicationRepository.countByStatus(ApplicationStatus.SELECTED);
            rejected = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        }

        return Map.of(
            "total", total,
            "applied", applied,
            "inProgress", inProgress,
            "selected", selected,
            "rejected", rejected
        );
    }

    private void applyStatusTransition(Application application, RoundType roundType, RoundStatus roundStatus) {
        if (roundStatus == RoundStatus.FAILED) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setCurrentRound(roundType);
            return;
        }

        if (roundStatus == RoundStatus.PENDING) {
            application.setStatus(ApplicationStatus.IN_PROGRESS);
            application.setCurrentRound(roundType);
            return;
        }

        if (roundStatus == RoundStatus.PASSED) {
            application.setStatus(ApplicationStatus.IN_PROGRESS);
            switch (roundType) {
                case APTITUDE -> application.setCurrentRound(RoundType.TECHNICAL);
                case TECHNICAL -> application.setCurrentRound(RoundType.INTERVIEW);
                case INTERVIEW -> {
                    application.setStatus(ApplicationStatus.SELECTED);
                    application.setCurrentRound(null);
                }
            }
        }
    }

    private void startFirstRound(Application application) {
        application.setStatus(ApplicationStatus.IN_PROGRESS);
        application.setCurrentRound(RoundType.APTITUDE);
    }

    private ApplicationResponse toResponseWithRounds(Application application) {
        List<ApplicationRoundResponse> rounds = applicationRoundRepository
            .findByApplicationIdOrderByIdAsc(application.getId())
            .stream()
            .map(this::toRoundResponse)
            .toList();

        return toResponse(application, rounds);
    }

    private ApplicationRoundResponse toRoundResponse(ApplicationRound round) {
        return ApplicationRoundResponse.builder()
            .id(round.getId())
            .roundType(round.getRoundType())
            .status(round.getStatus())
            .score(round.getScore())
            .feedback(round.getFeedback())
            .startedAt(round.getStartedAt())
            .completedAt(round.getCompletedAt())
            .build();
    }

    private ApplicationResponse toResponse(Application application, List<ApplicationRoundResponse> rounds) {
        return ApplicationResponse.builder()
            .id(application.getId())
            .jobId(application.getJobId())
            .candidateEmail(application.getCandidateEmail())
            .resumeUrl(application.getResumeUrl())
            .status(application.getStatus())
            .currentRound(application.getCurrentRound())
            .resumeScore(application.getResumeScore())
            .resumeStatus(application.getResumeStatus())
            .createdAt(application.getCreatedAt())
            .updatedAt(application.getUpdatedAt())
            .rounds(rounds)
            .build();
    }
}
