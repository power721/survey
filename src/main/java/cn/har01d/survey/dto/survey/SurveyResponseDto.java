package cn.har01d.survey.dto.survey;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SurveyResponseDto {
    private Long id;
    private String ip;
    private String username;
    private String nickname;
    private List<AnswerDto> answers;
    private Instant createdAt;
}
