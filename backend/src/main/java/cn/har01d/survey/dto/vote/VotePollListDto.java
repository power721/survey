package cn.har01d.survey.dto.vote;

import java.time.Instant;

import cn.har01d.survey.dto.CreatorDto;
import cn.har01d.survey.entity.VotePoll;
import lombok.Data;

@Data
public class VotePollListDto {
    private Long id;
    private String shareId;
    private String title;
    private String voteType;
    private String frequency;
    private String status;
    private String accessLevel;
    private boolean anonymous;
    private String logoUrl;
    private Instant startTime;
    private Instant endTime;
    private int totalVoteCount;
    private CreatorDto creator;
    private Instant createdAt;
    private Instant updatedAt;
}
