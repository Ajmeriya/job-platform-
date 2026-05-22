package com.job_platfrom.demo.controller;

import com.job_platfrom.demo.client.JobServiceClient;
import com.job_platfrom.demo.dto.JobResponse;
import com.job_platfrom.demo.util.JwtUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobProxyController {

    private final JobServiceClient jobServiceClient;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getAllJobs(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            return ResponseEntity.ok(jobServiceClient.getAllJobs(authorizationHeader));
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyJobs(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            Long userId = requireUserId(authorizationHeader);
            List<JobResponse> upstreamJobs = jobServiceClient.getAllJobs(authorizationHeader);
            List<JobResponse> jobs = (upstreamJobs == null ? List.<JobResponse>of() : upstreamJobs)
                .stream()
                .filter(job -> job.getCreatedBy() != null && job.getCreatedBy().equals(userId))
                .toList();
            return ResponseEntity.ok(jobs);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveJobs(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            List<JobResponse> upstreamJobs = jobServiceClient.getAllJobs(authorizationHeader);
            List<JobResponse> jobs = (upstreamJobs == null ? List.<JobResponse>of() : upstreamJobs)
                .stream()
                .filter(job -> job.getStatus() != null && "ACTIVE".equalsIgnoreCase(job.getStatus()))
                .toList();
            return ResponseEntity.ok(jobs);
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobById(
        @PathVariable Long jobId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            return ResponseEntity.ok(jobServiceClient.getJobById(jobId, authorizationHeader));
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PostMapping
    public ResponseEntity<?> createJob(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody Object request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobServiceClient.createJob(request, requireUserId(authorizationHeader), authorizationHeader));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<?> updateJob(
        @PathVariable Long jobId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody Object request
    ) {
        try {
            return ResponseEntity.ok(jobServiceClient.updateJob(jobId, request, requireUserId(authorizationHeader), authorizationHeader));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PatchMapping("/{jobId}/status")
    public ResponseEntity<?> updateJobStatus(
        @PathVariable Long jobId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody Object request
    ) {
        try {
            return ResponseEntity.ok(jobServiceClient.updateJobStatus(jobId, request, requireUserId(authorizationHeader), authorizationHeader));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(
        @PathVariable Long jobId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            jobServiceClient.deleteJob(jobId, requireUserId(authorizationHeader), authorizationHeader);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PostMapping("/{jobId}/rounds")
    public ResponseEntity<?> addRounds(
        @PathVariable Long jobId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody List<Object> rounds
    ) {
        try {
            requireUserId(authorizationHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(jobServiceClient.addRounds(jobId, rounds, authorizationHeader));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/{jobId}/rounds")
    public ResponseEntity<?> getRoundsByJobId(
        @PathVariable Long jobId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            return ResponseEntity.ok(jobServiceClient.getRoundsByJobId(jobId, authorizationHeader));
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    private Long requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        return jwtUtil.extractUserId(authorizationHeader.substring(7));
    }
}