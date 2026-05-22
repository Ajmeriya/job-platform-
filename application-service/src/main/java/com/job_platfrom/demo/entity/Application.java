package com.job_platfrom.demo.entity;

import com.job_platfrom.demo.Enum.ApplicationStatus;
import com.job_platfrom.demo.Enum.ResumeStatus;
import com.job_platfrom.demo.Enum.RoundType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_application_candidate_job", columnNames = {"candidate_email", "job_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "candidate_email", nullable = false)
    private String candidateEmail;

    @Column(name = "resume_url", nullable = false, columnDefinition = "TEXT")
    private String resumeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_round")
    private RoundType currentRound;

    @Column(name = "resume_score")
    private Double resumeScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "resume_status", nullable = false)
    @Builder.Default
    private ResumeStatus resumeStatus = ResumeStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationRound> rounds = new ArrayList<>();
}
