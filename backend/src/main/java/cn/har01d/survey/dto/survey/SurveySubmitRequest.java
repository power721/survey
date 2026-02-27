package cn.har01d.survey.dto.survey;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import lombok.Data;

@Data
public class SurveySubmitRequest {
    @NotEmpty
    @Valid
    private List<AnswerRequest> answers;
}
