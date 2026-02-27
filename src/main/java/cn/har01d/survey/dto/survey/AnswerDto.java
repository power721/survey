package cn.har01d.survey.dto.survey;

import java.util.List;

import lombok.Data;

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
