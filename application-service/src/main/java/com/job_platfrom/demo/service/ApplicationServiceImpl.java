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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationRoundRepository applicationRoundRepository;

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
                application.setStatus(ApplicationStatus.IN_PROGRESS);
                if (application.getCurrentRound() == null) {
                    application.setCurrentRound(RoundType.APTITUDE);
                }
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
            if (roundType == RoundType.APTITUDE) {
                application.setCurrentRound(RoundType.TECHNICAL);
                return;
            }
            if (roundType == RoundType.TECHNICAL) {
                application.setCurrentRound(RoundType.INTERVIEW);
                return;
            }
            application.setStatus(ApplicationStatus.SELECTED);
            application.setCurrentRound(null);
        }
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
