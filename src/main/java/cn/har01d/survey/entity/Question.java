package cn.har01d.survey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String description;

    private boolean required = false;

    private int sortOrder = 0;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();

    public enum QuestionType {
        SINGLE_CHOICE,
        MULTIPLE_CHOICE,
        TEXT,
        TEXTAREA,
        NUMBER,
        RATING,
        DATE,
        EMAIL,
        URL,
        PHONE,
        ID_CARD,
        FILE
    }
}
