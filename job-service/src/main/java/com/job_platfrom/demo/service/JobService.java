package com.job_platfrom.demo.service;

import com.job_platfrom.demo.dto.CreateJobRequest;
import com.job_platfrom.demo.dto.UpdateJobRequest;
import com.job_platfrom.demo.dto.UpdateJobStatusRequest;
import com.job_platfrom.demo.entity.Job;
import java.util.List;
import java.util.Optional;

public interface JobService {

    Job createJob(CreateJobRequest request, Long createdBy);

    List<Job> getAllJobs();

    Optional<Job> getJobById(Long jobId);

    Job updateJob(Long jobId, UpdateJobRequest request, Long createdBy);

    Job updateJobStatus(Long jobId, UpdateJobStatusRequest request, Long createdBy);

    void deleteJob(Long jobId, Long createdBy);
}
