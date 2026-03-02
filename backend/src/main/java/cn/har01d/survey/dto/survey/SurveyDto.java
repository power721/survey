package cn.har01d.survey.dto.survey;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class SurveyDto {
    private Long id;
    private String shareId;
    private String title;
    private String description;
    private String status;
    private String accessLevel;
    private boolean anonymous;
    private boolean template;
    private boolean allowUpdate;
    private String logoUrl;
    private Instant startTime;
    private Instant endTime;
    private int responseCount;
    private List<QuestionDto> questions;
    private String creatorName;
    private Instant createdAt;
    private Instant updatedAt;
}
