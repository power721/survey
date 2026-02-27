package cn.har01d.survey.dto.vote;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class VoteSubmitRequest {
    private List<Long> optionIds;
    private Map<Long, Integer> votes;
    private String deviceId;
}
