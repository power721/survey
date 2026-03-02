package cn.har01d.survey.dto.survey;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class QuestionRequest {
    private Long id;

    @NotBlank
    private String type;

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 1000)
    private String description;

    private boolean required = false;
    private int sortOrder = 0;

    private Long conditionQuestionId;
    private List<Long> conditionOptionIds;

    private List<OptionRequest> options;
}
