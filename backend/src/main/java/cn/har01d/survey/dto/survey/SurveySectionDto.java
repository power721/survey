package cn.har01d.survey.dto.survey;

import java.util.List;

import lombok.Data;

@Data
public class SurveySectionDto {
    private Long id;
    private String title;
    private int sortOrder;
    private List<QuestionDto> questions;
}
