package com.job_platfrom.demo.controller;

import com.job_platfrom.demo.dto.JobResponse;
import com.job_platfrom.demo.service.ApplicationJobService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationJobController {

    private final ApplicationJobService applicationJobService;

    @GetMapping("/jobs")
    public ResponseEntity<?> getAllJobsFromJobService(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        try {
            List<JobResponse> jobs = applicationJobService.fetchAllJobs(authorizationHeader);
            return ResponseEntity.ok(jobs);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String message = body == null || body.isBlank() ? "Failed to fetch jobs from job-service" : body;
            return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode().value())).body(message);
        }
    }
}
