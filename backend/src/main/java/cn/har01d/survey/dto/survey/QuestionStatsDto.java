package cn.har01d.survey.dto.survey;

import java.util.List;

import lombok.Data;

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
