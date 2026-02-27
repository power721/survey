package cn.har01d.survey.dto.vote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VoteOptionRequest {
    private Long id;

    @NotBlank
    @Size(max = 500)
    private String content;

    @Size(max = 1000)
    private String imageUrl;

    private Integer maxVotes;
    private int sortOrder = 0;
}
