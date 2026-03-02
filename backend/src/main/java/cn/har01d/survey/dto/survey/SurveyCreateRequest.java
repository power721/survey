package cn.har01d.survey.dto.survey;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SurveyCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    private String accessLevel = "PUBLIC";
    private boolean anonymous = true;
    private boolean template = false;
    private boolean allowUpdate = false;
    @Size(max = 500)
    private String logoUrl;
    @Size(max = 500)
    private String backgroundUrl;
    private Instant startTime;
    private Instant endTime;

    @Valid
    private List<QuestionRequest> questions;

    @Valid
    private List<SurveySectionRequest> sections;
}
