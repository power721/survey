package cn.har01d.survey.dto.vote;

import java.util.List;

import lombok.Data;

@Data
public class VoteOptionDto {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private int voteCount;
    private double percentage;
    private int sortOrder;
    private List<VoterDto> voters;
}
