package cn.har01d.survey.dto.survey;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QuestionStatsDto {
    private Long questionId;
    private String questionTitle;
    private String questionType;
    private List<OptionStatsDto> optionStats;
    private List<String> textAnswers;

    @Data
    public static class OptionStatsDto {
        private Long optionId;
        private String content;
        private long count;
        private double percentage;
    }
}
