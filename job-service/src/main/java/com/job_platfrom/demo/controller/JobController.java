package com.job_platfrom.demo.controller;

import com.job_platfrom.demo.dto.CreateJobRequest;
import com.job_platfrom.demo.dto.JobRoundRequest;
import com.job_platfrom.demo.entity.Job;
import com.job_platfrom.demo.entity.JobRound;
import com.job_platfrom.demo.service.JobRoundService;
import com.job_platfrom.demo.service.JobService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
