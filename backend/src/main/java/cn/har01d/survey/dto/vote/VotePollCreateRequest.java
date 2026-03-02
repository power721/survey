package cn.har01d.survey.dto.vote;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Data;

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
    private boolean showVoters = false;
    private Integer maxTotalVotes;
    private Integer maxOptions;
    private Integer minOptions;
    private Integer maxVotesPerOption;
    @Size(max = 500)
    private String logoUrl;
    @Size(max = 500)
    private String backgroundUrl;
    private Instant startTime;
    private Instant endTime;

    @NotEmpty
    @Valid
    private List<VoteOptionRequest> options;
}
