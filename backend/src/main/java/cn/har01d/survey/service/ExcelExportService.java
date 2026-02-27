package cn.har01d.survey.service;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.har01d.survey.entity.Answer;
import cn.har01d.survey.entity.Question;
import cn.har01d.survey.entity.QuestionOption;
import cn.har01d.survey.entity.Survey;
import cn.har01d.survey.entity.SurveyResponse;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.SurveyRepository;
import cn.har01d.survey.repository.SurveyResponseRepository;

@Service
public class ExcelExportService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;
    private final AuthService authService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public ExcelExportService(SurveyRepository surveyRepository, SurveyResponseRepository responseRepository,
                              AuthService authService) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public void exportSurveyResponses(Long surveyId, HttpServletResponse response) throws IOException {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found"));

        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        List<SurveyResponse> responses = responseRepository.findBySurveyId(surveyId);
        List<Question> questions = survey.getQuestions();

        // Build option ID to content map for resolving multiple choice answers
        Map<Long, String> optionContentMap = questions.stream()
                .flatMap(q -> q.getOptions().stream())
                .collect(Collectors.toMap(QuestionOption::getId, QuestionOption::getContent));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Responses");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            int colIdx = 0;
            Cell cell = headerRow.createCell(colIdx++);
            cell.setCellValue("#");
            cell.setCellStyle(headerStyle);

            cell = headerRow.createCell(colIdx++);
            cell.setCellValue("Submit Time");
            cell.setCellStyle(headerStyle);

            cell = headerRow.createCell(colIdx++);
            cell.setCellValue("IP");
            cell.setCellStyle(headerStyle);

            boolean anonymous = survey.isAnonymous();
            if (!anonymous) {
                cell = headerRow.createCell(colIdx++);
                cell.setCellValue("User");
                cell.setCellStyle(headerStyle);
            }

            for (Question question : questions) {
                cell = headerRow.createCell(colIdx++);
                cell.setCellValue(question.getTitle());
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (SurveyResponse sr : responses) {
                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(sr.getCreatedAt() != null ? FORMATTER.format(sr.getCreatedAt()) : "");
                row.createCell(2).setCellValue(sr.getIp() != null ? sr.getIp() : "");

                int dataColIdx = 3;
                if (!anonymous) {
                    String userName = "";
                    if (sr.getUser() != null) {
                        userName = sr.getUser().getNickname() != null ? sr.getUser().getNickname() : sr.getUser().getUsername();
                    }
                    row.createCell(dataColIdx++).setCellValue(userName);
                }

                Map<Long, Answer> answerMap = sr.getAnswers().stream()
                        .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

                colIdx = dataColIdx;
                for (Question question : questions) {
                    Answer answer = answerMap.get(question.getId());
                    String value = "";
                    if (answer != null) {
                        if (answer.getSelectedOption() != null) {
                            value = answer.getSelectedOption().getContent();
                        } else if (answer.getSelectedOptionIds() != null) {
                            value = java.util.Arrays.stream(answer.getSelectedOptionIds().split(","))
                                    .map(String::trim)
                                    .map(s -> {
                                        try {
                                            Long optId = Long.parseLong(s);
                                            return optionContentMap.getOrDefault(optId, s);
                                        } catch (NumberFormatException e) {
                                            return s;
                                        }
                                    })
                                    .collect(Collectors.joining(", "));
                        } else if (answer.getTextValue() != null) {
                            value = answer.getTextValue();
                        }
                    }
                    row.createCell(colIdx++).setCellValue(value);
                }
                rowIdx++;
            }

            // Auto-size columns
            for (int i = 0; i < colIdx; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=survey_" + surveyId + "_responses.xlsx");
            workbook.write(response.getOutputStream());
        }
    }
}
