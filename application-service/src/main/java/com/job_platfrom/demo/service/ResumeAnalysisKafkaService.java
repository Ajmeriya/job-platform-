package com.job_platfrom.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_platfrom.demo.Enum.ApplicationStatus;
import com.job_platfrom.demo.Enum.ResumeStatus;
import com.job_platfrom.demo.Enum.RoundType;
import com.job_platfrom.demo.dto.ResumeAnalysisRequestEvent;
import com.job_platfrom.demo.dto.ResumeAnalysisResultEvent;
import com.job_platfrom.demo.entity.Application;
import com.job_platfrom.demo.repository.ApplicationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.resume-analysis-request-topic}")
    private String requestTopic;

    @Value("${app.kafka.resume-analysis-result-topic}")
    private String resultTopic;

    public void queueAnalysis(Application application) {
        if (application == null || application.getId() == null) {
            return;
        }

        try {
            ResumeAnalysisRequestEvent event = ResumeAnalysisRequestEvent.builder()
                .applicationId(application.getId())
                .jobId(application.getJobId())
                .candidateEmail(application.getCandidateEmail())
                .resumeUrl(application.getResumeUrl())
                .requestId(UUID.randomUUID().toString())
                .build();

            kafkaTemplate.send(requestTopic, String.valueOf(application.getId()), objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("Unable to queue resume analysis for application {}: {}", application.getId(), ex.getMessage());
        }
    }

    @KafkaListener(topics = "${app.kafka.resume-analysis-result-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleResult(String payload) {
        try {
            ResumeAnalysisResultEvent event = objectMapper.readValue(payload, ResumeAnalysisResultEvent.class);
            if (event.getApplicationId() == null) {
                return;
            }

            log.info(
                "Received resume analysis result: applicationId={}, similarity={}, matchPercentage={}, verdict={}",
                event.getApplicationId(),
                event.getSimilarity(),
                event.getMatchPercentage(),
                event.getVerdict()
            );

            applicationRepository.findById(event.getApplicationId()).ifPresent(application -> {
                if (event.getMatchPercentage() != null) {
                    application.setResumeScore(event.getMatchPercentage());
                }

                if (event.getSimilarity() != null) {
                    if (event.getSimilarity() >= 0.75d) {
                        application.setResumeStatus(ResumeStatus.SHORTLISTED);
                        if (application.getStatus() == ApplicationStatus.APPLIED) {
                            application.setStatus(ApplicationStatus.IN_PROGRESS);
                            application.setCurrentRound(RoundType.APTITUDE);
                        }
                    } else {
                        application.setResumeStatus(ResumeStatus.REJECTED);
                        if (application.getStatus() == ApplicationStatus.APPLIED) {
                            application.setStatus(ApplicationStatus.REJECTED);
                            application.setCurrentRound(null);
                        }
                    }
                }

                applicationRepository.save(application);
                log.info(
                    "Updated application after resume analysis: applicationId={}, status={}, resumeStatus={}, score={}",
                    application.getId(),
                    application.getStatus(),
                    application.getResumeStatus(),
                    application.getResumeScore()
                );
            });
        } catch (Exception ex) {
            log.warn("Failed to process resume analysis result: {}", ex.getMessage(), ex);
        }
    }
}