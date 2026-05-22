package com.job_platfrom.demo.controller;

import com.job_platfrom.demo.dto.CreateJobRequest;
import com.job_platfrom.demo.dto.JobRoundRequest;
import com.job_platfrom.demo.dto.UpdateJobRequest;
import com.job_platfrom.demo.dto.UpdateJobStatusRequest;
import com.job_platfrom.demo.entity.Job;
import com.job_platfrom.demo.entity.JobRound;
import com.job_platfrom.demo.service.JobRoundService;
import com.job_platfrom.demo.service.JobService;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobRoundService jobRoundService;

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody CreateJobRequest request, @RequestParam Long createdBy) {
        try {
            Job savedJob = jobService.createJob(request, createdBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobById(@PathVariable Long jobId) {
        try {
            return jobService.getJobById(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found for id: " + jobId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/public/{jobId}")
    public ResponseEntity<?> getPublicJobById(@PathVariable Long jobId) {
        try {
            return jobService.getJobById(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found for id: " + jobId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<?> updateJob(
        @PathVariable Long jobId,
        @RequestBody UpdateJobRequest request,
        @RequestParam Long createdBy
    ) {
        try {
            Job updatedJob = jobService.updateJob(jobId, request, createdBy);
            return ResponseEntity.ok(updatedJob);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Job not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PatchMapping("/{jobId}/status")
    public ResponseEntity<?> updateJobStatus(
        @PathVariable Long jobId,
        @RequestBody UpdateJobStatusRequest request,
        @RequestParam Long createdBy
    ) {
        try {
            Job updatedJob = jobService.updateJobStatus(jobId, request, createdBy);
            return ResponseEntity.ok(updatedJob);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Job not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            if (ex.getMessage() != null && ex.getMessage().startsWith("Not allowed")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{jobId}/status")
    public ResponseEntity<?> updateJobStatusPut(
        @PathVariable Long jobId,
        @RequestBody UpdateJobStatusRequest request,
        @RequestParam Long createdBy
    ) {
        return updateJobStatus(jobId, request, createdBy);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(@PathVariable Long jobId, @RequestParam Long createdBy) {
        try {
            jobService.deleteJob(jobId, createdBy);
            return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Job not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            if (ex.getMessage() != null && ex.getMessage().startsWith("Not allowed")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/{jobId}/rounds")
    public ResponseEntity<?> addRounds(@PathVariable Long jobId, @RequestBody List<JobRoundRequest> rounds) {
        try {
            List<JobRound> createdRounds = jobRoundService.addRounds(jobId, rounds);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRounds);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/{jobId}/rounds")
    public ResponseEntity<?> getRoundsByJobId(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(jobRoundService.getRoundsByJobId(jobId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
