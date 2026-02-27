package cn.har01d.survey.dto.vote;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VoteSubmitRequest {
    @NotEmpty
    private List<Long> optionIds;
    private String deviceId;
}
