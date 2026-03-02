package cn.har01d.survey.dto.vote;

import java.time.Instant;

import lombok.Data;

@Data
public class VoteRecordDto {
    private Long id;
    private Long pollId;
    private String pollShareId;
    private String pollTitle;
    private String optionTitle;
    private int voteCount;
    private String username;
    private String nickname;
    private String ip;
    private Instant createdAt;
}
