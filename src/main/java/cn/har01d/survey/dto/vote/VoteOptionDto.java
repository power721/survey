package cn.har01d.survey.dto.vote;

import lombok.Data;

@Data
public class VoteOptionDto {
    private Long id;
    private String content;
    private String imageUrl;
    private Integer maxVotes;
    private int voteCount;
    private double percentage;
    private int sortOrder;
}
