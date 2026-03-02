package cn.har01d.survey.dto.survey;

import java.util.List;

import lombok.Data;

@Data
public class QuestionDto {
    private Long id;
    private String type;
    private String title;
    private String description;
    private boolean required;
    private int sortOrder;
    private Long conditionQuestionId;
    private Long conditionOptionId;
    private List<OptionDto> options;
}
