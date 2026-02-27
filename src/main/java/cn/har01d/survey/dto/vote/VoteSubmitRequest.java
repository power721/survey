package cn.har01d.survey.dto.vote;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VoteSubmitRequest {
    private List<Long> optionIds;
    private Map<Long, Integer> votes;
    private String deviceId;
}
