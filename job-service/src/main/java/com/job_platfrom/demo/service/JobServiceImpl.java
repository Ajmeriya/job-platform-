package com.job_platfrom.demo.service;

import com.job_platfrom.demo.Enum.JobStatus;
import com.job_platfrom.demo.dto.CreateJobRequest;
import com.job_platfrom.demo.dto.UpdateJobRequest;
import com.job_platfrom.demo.dto.UpdateJobStatusRequest;
import com.job_platfrom.demo.entity.Job;
import com.job_platfrom.demo.repository.JobRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

	private final JobRepository jobRepository;
	private final EntityManager entityManager;

	@Override
	public Job createJob(CreateJobRequest request, Long createdBy) {
		validateCreateJobRequest(request, createdBy);

		Job job = Job.builder()
			.role(request.getRole().trim())
			.companyName(request.getCompanyName().trim())
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

	@Override
	@Transactional
	public Job updateJob(Long jobId, UpdateJobRequest request, Long createdBy) {
		validateJobId(jobId);
		validateCreatedBy(createdBy);
		if (request == null) {
			throw new IllegalArgumentException("UpdateJobRequest cannot be null");
		}

		Job existing = jobRepository.findById(jobId)
			.orElseThrow(() -> new IllegalArgumentException("Job not found for id: " + jobId));

		if (!createdBy.equals(existing.getCreatedBy())) {
			throw new IllegalArgumentException("Not allowed to update this job");
		}

		if (request.getRole() != null) {
			if (request.getRole().isBlank()) {
				throw new IllegalArgumentException("role cannot be blank");
			}
			existing.setRole(request.getRole().trim());
		}
		if (request.getCompanyName() != null) {
			if (request.getCompanyName().isBlank()) {
				throw new IllegalArgumentException("companyName cannot be blank");
			}
			existing.setCompanyName(request.getCompanyName().trim());
		}
		if (request.getDescription() != null) {
			if (request.getDescription().isBlank()) {
				throw new IllegalArgumentException("description cannot be blank");
			}
			existing.setDescription(request.getDescription().trim());
		}
		if (request.getLocation() != null) {
			if (request.getLocation().isBlank()) {
				throw new IllegalArgumentException("location cannot be blank");
			}
			existing.setLocation(request.getLocation().trim());
		}
		if (request.getExperienceRequired() != null) {
			if (request.getExperienceRequired() < 0) {
				throw new IllegalArgumentException("experienceRequired must be zero or greater");
			}
			existing.setExperienceRequired(request.getExperienceRequired());
		}
		if (request.getSkillsRequired() != null) {
			existing.setSkillsRequired(request.getSkillsRequired());
		}
		if (request.getStatus() != null) {
			existing.setStatus(request.getStatus());
		}

		return jobRepository.save(existing);
	}

	@Override
	@Transactional
	public Job updateJobStatus(Long jobId, UpdateJobStatusRequest request, Long createdBy) {
		validateJobId(jobId);
		validateCreatedBy(createdBy);
		if (request == null || request.getStatus() == null) {
			throw new IllegalArgumentException("status is required");
		}

		Job existing = jobRepository.findById(jobId)
			.orElseThrow(() -> new IllegalArgumentException("Job not found for id: " + jobId));

		if (!createdBy.equals(existing.getCreatedBy())) {
			throw new IllegalArgumentException("Not allowed to update this job");
		}

		existing.setStatus(request.getStatus());
		return jobRepository.save(existing);
	}

	@Override
	@Transactional
	public void deleteJob(Long jobId, Long createdBy) {
		validateJobId(jobId);
		validateCreatedBy(createdBy);

		Job existing = jobRepository.findById(jobId)
			.orElseThrow(() -> new IllegalArgumentException("Job not found for id: " + jobId));

		if (!createdBy.equals(existing.getCreatedBy())) {
			throw new IllegalArgumentException("Not allowed to delete this job");
		}

		clearRoundsForJob(jobId);
		jobRepository.delete(existing);
	}

	private void validateCreateJobRequest(CreateJobRequest request, Long createdBy) {
		if (request == null) {
			throw new IllegalArgumentException("CreateJobRequest cannot be null");
		}
		if (createdBy == null || createdBy <= 0) {
			throw new IllegalArgumentException("createdBy must be a positive value");
		}
		if (request.getRole() == null || request.getRole().isBlank()) {
			throw new IllegalArgumentException("role is required");
		}
		if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
			throw new IllegalArgumentException("companyName is required");
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

	private void validateJobId(Long jobId) {
		if (jobId == null || jobId <= 0) {
			throw new IllegalArgumentException("jobId must be a positive value");
		}
	}

	private void validateCreatedBy(Long createdBy) {
		if (createdBy == null || createdBy <= 0) {
			throw new IllegalArgumentException("createdBy must be a positive value");
		}
	}

	private void clearRoundsForJob(Long jobId) {
		entityManager.createNativeQuery(
			"DELETE FROM aptitude_round_topics WHERE id IN (" +
			"SELECT id FROM aptitude_rounds WHERE job_id = :jobId)"
		).setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery(
			"DELETE FROM technical_round_topics WHERE id IN (" +
			"SELECT id FROM technical_rounds WHERE job_id = :jobId)"
		).setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery(
			"DELETE FROM interview_round_skills WHERE id IN (" +
			"SELECT id FROM interview_rounds WHERE job_id = :jobId)"
		).setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery("DELETE FROM aptitude_rounds WHERE job_id = :jobId")
			.setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery("DELETE FROM technical_rounds WHERE job_id = :jobId")
			.setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery("DELETE FROM interview_rounds WHERE job_id = :jobId")
			.setParameter("jobId", jobId).executeUpdate();
	}
}
