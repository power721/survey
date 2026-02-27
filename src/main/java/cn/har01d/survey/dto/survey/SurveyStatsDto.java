package cn.har01d.survey.dto.survey;

import lombok.Data;

import java.util.List;

@Data
public class SurveyStatsDto {
    private Long surveyId;
    private String title;
    private int totalResponses;
    private List<QuestionStatsDto> questionStats;
}
