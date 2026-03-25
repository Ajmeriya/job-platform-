package com.job_platfrom.demo.service;

import com.job_platfrom.demo.Enum.JobStatus;
import com.job_platfrom.demo.dto.CreateJobRequest;
import com.job_platfrom.demo.entity.Job;
import com.job_platfrom.demo.repository.JobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

	private final JobRepository jobRepository;

	@Override
	public Job createJob(CreateJobRequest request, Long createdBy) {
		validateCreateJobRequest(request, createdBy);

		Job job = Job.builder()
			.title(request.getTitle().trim())
			.description(request.getDescription().trim())
			.skillsRequired(request.getSkillsRequired() == null ? new ArrayList<>() : request.getSkillsRequired())
			.location(request.getLocation().trim())
			.experienceRequired(request.getExperienceRequired())
			.createdBy(createdBy)
			.status(JobStatus.DRAFT)
			.build();

		return jobRepository.save(job);
	}

	@Override
	public List<Job> getAllJobs() {
		return jobRepository.findAll();
	}

	@Override
	public Optional<Job> getJobById(Long jobId) {
		if (jobId == null || jobId <= 0) {
			throw new IllegalArgumentException("jobId must be a positive value");
		}
		return jobRepository.findById(jobId);
	}

	private void validateCreateJobRequest(CreateJobRequest request, Long createdBy) {
		if (request == null) {
			throw new IllegalArgumentException("CreateJobRequest cannot be null");
		}
		if (createdBy == null || createdBy <= 0) {
			throw new IllegalArgumentException("createdBy must be a positive value");
		}
		if (request.getTitle() == null || request.getTitle().isBlank()) {
			throw new IllegalArgumentException("title is required");
		}
		if (request.getDescription() == null || request.getDescription().isBlank()) {
			throw new IllegalArgumentException("description is required");
		}
		if (request.getLocation() == null || request.getLocation().isBlank()) {
			throw new IllegalArgumentException("location is required");
		}
		if (request.getExperienceRequired() == null || request.getExperienceRequired() < 0) {
			throw new IllegalArgumentException("experienceRequired must be zero or greater");
		}
	}
}
