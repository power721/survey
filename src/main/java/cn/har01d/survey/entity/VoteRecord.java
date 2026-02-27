package cn.har01d.survey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "vote_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private VotePoll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private VoteOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 45)
    private String ip;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 200)
    private String deviceId;

    @CreationTimestamp
    private Instant createdAt;
}
