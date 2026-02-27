package cn.har01d.survey.dto.survey;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDto {
    private Long id;
    private String type;
    private String title;
    private String description;
    private boolean required;
    private int sortOrder;
    private List<OptionDto> options;
}
