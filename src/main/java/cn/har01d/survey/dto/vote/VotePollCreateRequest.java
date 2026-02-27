package cn.har01d.survey.dto.vote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class VotePollCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    private String voteType = "SINGLE";
    private String frequency = "ONCE";
    private String accessLevel = "PUBLIC";
    private boolean anonymous = true;
    private Integer maxTotalVotes;
    private Integer maxOptions;
    private Integer maxVotesPerOption;
    private Instant endTime;

    @NotEmpty
    @Valid
    private List<VoteOptionRequest> options;
}
