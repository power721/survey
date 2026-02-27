package cn.har01d.survey.dto.survey;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AnswerRequest {
    @NotNull
    private Long questionId;
    private String textValue;
    private Long selectedOptionId;
    private List<Long> selectedOptionIds;
}
