package com.job_platfrom.demo.service;

import com.job_platfrom.demo.dto.ApplicationResponse;
import com.job_platfrom.demo.dto.ApplicationRoundUpdateRequest;
import com.job_platfrom.demo.dto.ApplyRequest;
import com.job_platfrom.demo.dto.ResumeReviewUpdateRequest;
import java.util.List;
import java.util.Map;

public interface ApplicationService {

    ApplicationResponse apply(String candidateEmail, ApplyRequest request);

    List<ApplicationResponse> getMyApplications(String candidateEmail);

    List<ApplicationResponse> getAllApplications();

    List<ApplicationResponse> getApplicationsByJobId(Long jobId);

    ApplicationResponse getById(Long id);

    ApplicationResponse updateResumeReview(Long id, ResumeReviewUpdateRequest request);

    ApplicationResponse updateRound(Long id, ApplicationRoundUpdateRequest request);

    ApplicationResponse withdrawApplication(String candidateEmail, Long applicationId);

    Map<String, Long> getMyApplicationStats(String candidateEmail);

    Map<String, Long> getApplicationSummary(Long jobId);
}
