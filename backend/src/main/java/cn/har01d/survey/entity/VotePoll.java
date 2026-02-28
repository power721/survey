package cn.har01d.survey.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private boolean showVoters = false;

    private Integer maxTotalVotes;

    private Integer maxOptions;

    private Integer maxVotesPerOption;

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
        SINGLE, MULTIPLE, SCORED
    }

    public enum VoteFrequency {
        ONCE, DAILY
    }
}
