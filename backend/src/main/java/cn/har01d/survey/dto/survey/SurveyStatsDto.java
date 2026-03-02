package cn.har01d.survey.dto.survey;

import java.util.List;

import lombok.Data;

@Data
public class SurveyStatsDto {
    private Long surveyId;
    private String shareId;
    private String title;
    private int totalResponses;
    private List<QuestionStatsDto> questionStats;
}
