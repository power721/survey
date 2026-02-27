package cn.har01d.survey.dto.survey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OptionRequest {
    private Long id;

    @NotBlank
    @Size(max = 500)
    private String content;

    private int sortOrder = 0;
}
