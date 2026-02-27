package cn.har01d.survey.dto.survey;

import lombok.Data;

@Data
public class OptionDto {
    private Long id;
    private String content;
    private int sortOrder;
}
