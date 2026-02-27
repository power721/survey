package cn.har01d.survey.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vote_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private VotePoll poll;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 1000)
    private String imageUrl;

    private int voteCount = 0;

    private int sortOrder = 0;
}
