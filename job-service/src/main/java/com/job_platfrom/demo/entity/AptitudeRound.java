package com.job_platfrom.demo.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "aptitude_rounds")
@Getter
@Setter
public class AptitudeRound extends JobRound {

    @Column(name = "aptitude_questions", nullable = false)
    private Integer aptitudeQuestions;

    @ElementCollection
    @CollectionTable(name = "aptitude_round_topics", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "topic")
    private List<String> aptitudeTopics = new ArrayList<>();
}
