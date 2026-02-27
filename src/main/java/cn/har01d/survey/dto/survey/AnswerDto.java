package cn.har01d.survey.dto.survey;

import lombok.Data;

import java.util.List;

@Data
public class AnswerDto {
    private Long id;
    private Long questionId;
    private String questionTitle;
    private String textValue;
    private Long selectedOptionId;
    private String selectedOptionContent;
    private List<Long> selectedOptionIds;
    private List<String> selectedOptionContents;
}
