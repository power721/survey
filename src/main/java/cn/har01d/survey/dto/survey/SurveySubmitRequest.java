package cn.har01d.survey.dto.survey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SurveySubmitRequest {
    @NotEmpty
    @Valid
    private List<AnswerRequest> answers;
}
