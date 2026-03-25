package com.job_platfrom.demo.service;

import com.job_platfrom.demo.Enum.RoundType;
import com.job_platfrom.demo.dto.JobRoundRequest;
import com.job_platfrom.demo.entity.AptitudeRound;
import com.job_platfrom.demo.entity.InterviewRound;
import com.job_platfrom.demo.entity.Job;
import com.job_platfrom.demo.entity.JobRound;
import com.job_platfrom.demo.entity.TechnicalRound;
import com.job_platfrom.demo.repository.AptitudeRoundRepository;
import com.job_platfrom.demo.repository.InterviewRoundRepository;
import com.job_platfrom.demo.repository.JobRepository;
import com.job_platfrom.demo.repository.TechnicalRoundRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobRoundServiceImpl implements JobRoundService {

	private final JobRepository jobRepository;
	private final AptitudeRoundRepository aptitudeRoundRepository;
	private final TechnicalRoundRepository technicalRoundRepository;
	private final InterviewRoundRepository interviewRoundRepository;
	private final EntityManager entityManager;

	@Override
	@Transactional
	public List<JobRound> addRounds(Long jobId, List<JobRoundRequest> rounds) {
		if (jobId == null || jobId <= 0) {
			throw new IllegalArgumentException("jobId must be a positive value");
		}
		if (rounds == null || rounds.isEmpty()) {
			throw new IllegalArgumentException("rounds cannot be empty");
		}

		Job job = jobRepository.findById(jobId)
			.orElseThrow(() -> new IllegalArgumentException("Job not found for id: " + jobId));

		// Reconfigure mode: replace existing rounds for the same job.
		clearRoundsForJob(jobId);

		List<JobRoundRequest> sortedRequests = new ArrayList<>(rounds);
		sortedRequests.sort(Comparator.comparingInt(this::orderForRoundType));

		Set<RoundType> seenRoundTypes = new HashSet<>();
		List<JobRound> savedEntities = new ArrayList<>();
		int sequence = 1;

		for (JobRoundRequest request : sortedRequests) {
			validateRoundRequest(request, seenRoundTypes);
			JobRound entity = mapToEntity(job, request, sequence++);
			
			// Save to the appropriate repository based on type
			JobRound saved = switch (entity.getRoundType()) {
				case APTITUDE -> aptitudeRoundRepository.save((AptitudeRound) entity);
				case TECHNICAL -> technicalRoundRepository.save((TechnicalRound) entity);
				case INTERVIEW -> interviewRoundRepository.save((InterviewRound) entity);
			};
			savedEntities.add(saved);
		}

		return savedEntities;
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobRound> getRoundsByJobId(Long jobId) {
		if (jobId == null || jobId <= 0) {
			throw new IllegalArgumentException("jobId must be a positive value");
		}
		// Fetch from all three tables and combine results, sorted by sequence
		List<JobRound> allRounds = new ArrayList<>();
		allRounds.addAll(aptitudeRoundRepository.findByJobIdOrderBySequenceOrderAsc(jobId));
		allRounds.addAll(technicalRoundRepository.findByJobIdOrderBySequenceOrderAsc(jobId));
		allRounds.addAll(interviewRoundRepository.findByJobIdOrderBySequenceOrderAsc(jobId));
		
		// Ensure they're sorted by sequence order (should already be, but just in case)
		allRounds.sort(Comparator.comparingInt(JobRound::getSequenceOrder));
		return allRounds;
	}

	private void validateRoundRequest(JobRoundRequest request, Set<RoundType> seenRoundTypes) {
		if (request == null) {
			throw new IllegalArgumentException("round request cannot be null");
		}
		if (request.getRoundType() == null) {
			throw new IllegalArgumentException("roundType is required");
		}
		if (!seenRoundTypes.add(request.getRoundType())) {
			throw new IllegalArgumentException("Duplicate roundType is not allowed: " + request.getRoundType());
		}
		if (request.getDifficulty() == null) {
			throw new IllegalArgumentException("difficulty is required");
		}
		if (request.getTimeLimit() == null || request.getTimeLimit() <= 0) {
			throw new IllegalArgumentException("timeLimit must be greater than zero");
		}

		switch (request.getRoundType()) {
			case APTITUDE -> {
				if (request.getAptitudeQuestions() == null || request.getAptitudeQuestions() <= 0) {
					throw new IllegalArgumentException("aptitudeQuestions must be greater than zero for APTITUDE round");
				}
			}
			case TECHNICAL -> {
				int dsaQuestions = request.getDsaQuestions() == null ? 0 : request.getDsaQuestions();
				int sqlQuestions = request.getSqlQuestions() == null ? 0 : request.getSqlQuestions();

				if (dsaQuestions < 0 || sqlQuestions < 0) {
					throw new IllegalArgumentException("dsaQuestions and sqlQuestions cannot be negative");
				}
				if (dsaQuestions + sqlQuestions <= 0) {
					throw new IllegalArgumentException("At least one of dsaQuestions or sqlQuestions must be greater than zero for TECHNICAL round");
				}
			}
			case INTERVIEW -> {
				if (request.getInterviewSkills() == null || request.getInterviewSkills().isEmpty()) {
					throw new IllegalArgumentException("interviewSkills is required for INTERVIEW round");
				}
			}
			default -> throw new IllegalArgumentException("Unsupported round type: " + request.getRoundType());
		}
	}

	private JobRound mapToEntity(Job job, JobRoundRequest request, int sequenceOrder) {
		JobRound round = switch (request.getRoundType()) {
			case APTITUDE -> {
				AptitudeRound aptitudeRound = new AptitudeRound();
				aptitudeRound.setAptitudeQuestions(request.getAptitudeQuestions());
				aptitudeRound.setAptitudeTopics(request.getAptitudeTopics() == null ? new ArrayList<>() : request.getAptitudeTopics());
				yield aptitudeRound;
			}
			case TECHNICAL -> {
				TechnicalRound technicalRound = new TechnicalRound();
				technicalRound.setDsaQuestions(request.getDsaQuestions());
				technicalRound.setSqlQuestions(request.getSqlQuestions());
				technicalRound.setTechnicalTopics(request.getTechnicalTopics() == null ? new ArrayList<>() : request.getTechnicalTopics());
				yield technicalRound;
			}
			case INTERVIEW -> {
				InterviewRound interviewRound = new InterviewRound();
				interviewRound.setInterviewSkills(request.getInterviewSkills() == null ? new ArrayList<>() : request.getInterviewSkills());
				yield interviewRound;
			}
		};

		round.setJob(job);
		round.setRoundType(request.getRoundType());
		round.setDifficulty(request.getDifficulty());
		round.setTimeLimit(request.getTimeLimit());
		round.setSequenceOrder(sequenceOrder);
		return round;
	}

	private int orderForRoundType(JobRoundRequest request) {
		if (request == null || request.getRoundType() == null) {
			return Integer.MAX_VALUE;
		}
		return switch (request.getRoundType()) {
			case APTITUDE -> 1;
			case TECHNICAL -> 2;
			case INTERVIEW -> 3;
		};
	}

	private void clearRoundsForJob(Long jobId) {
		// Delete ElementCollections first
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

		// Delete from child tables
		entityManager.createNativeQuery("DELETE FROM aptitude_rounds WHERE job_id = :jobId")
			.setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery("DELETE FROM technical_rounds WHERE job_id = :jobId")
			.setParameter("jobId", jobId).executeUpdate();

		entityManager.createNativeQuery("DELETE FROM interview_rounds WHERE job_id = :jobId")
			.setParameter("jobId", jobId).executeUpdate();
	}
}
