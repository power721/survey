package cn.har01d.survey.dto.survey;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class SurveyResponseDto {
    private Long id;
    private String ip;
    private String username;
    private String nickname;
    private List<AnswerDto> answers;
    private Instant createdAt;
}
