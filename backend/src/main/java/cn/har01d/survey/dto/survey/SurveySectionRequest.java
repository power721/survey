package cn.har01d.survey.dto.survey;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SurveySectionRequest {
    private Long id;

    @Size(max = 200)
    private String title;

    private int sortOrder = 0;

    @Valid
    private List<QuestionRequest> questions;
}
