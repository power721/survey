package cn.har01d.survey.dto.survey;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AnswerRequest {
    @NotNull
    private Long questionId;
    private String textValue;
    private Long selectedOptionId;
    private List<Long> selectedOptionIds;
}
