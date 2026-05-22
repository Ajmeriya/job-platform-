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
@Table(name = "technical_rounds")
@Getter
@Setter
public class TechnicalRound extends JobRound {

    @Column(name = "dsa_questions")
    private Integer dsaQuestions;

    @Column(name = "sql_questions")
    private Integer sqlQuestions;

    @ElementCollection
    @CollectionTable(name = "technical_round_topics", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "topic")
    private List<String> technicalTopics = new ArrayList<>();
}
