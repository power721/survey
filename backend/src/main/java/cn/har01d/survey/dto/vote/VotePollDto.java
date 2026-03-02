package cn.har01d.survey.dto.vote;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class VotePollDto {
    private Long id;
    private String shareId;
    private String title;
    private String description;
    private String voteType;
    private String frequency;
    private String status;
    private String accessLevel;
    private boolean anonymous;
    private boolean showVoters;
    private Integer maxTotalVotes;
    private Integer maxOptions;
    private Integer maxVotesPerOption;
    private Instant startTime;
    private Instant endTime;
    private int totalVoteCount;
    private List<VoteOptionDto> options;
    private String creatorName;
    private boolean hasVoted;
    private Instant createdAt;
    private Instant updatedAt;
}
