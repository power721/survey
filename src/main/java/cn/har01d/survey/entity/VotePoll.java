package cn.har01d.survey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vote_polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotePoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String shareId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteType voteType = VoteType.SINGLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteFrequency frequency = VoteFrequency.ONCE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Survey.SurveyStatus status = Survey.SurveyStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Survey.AccessLevel accessLevel = Survey.AccessLevel.PUBLIC;

    private boolean anonymous = true;

    private Integer maxTotalVotes;

    private Instant endTime;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<VoteOption> options = new ArrayList<>();

    private int totalVoteCount = 0;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum VoteType {
        SINGLE, MULTIPLE
    }

    public enum VoteFrequency {
        ONCE, DAILY
    }
}
