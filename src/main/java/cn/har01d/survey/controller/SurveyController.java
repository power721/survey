package cn.har01d.survey.controller;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.survey.*;
import cn.har01d.survey.service.ExcelExportService;
import cn.har01d.survey.service.SurveyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;
    private final ExcelExportService excelExportService;

    public SurveyController(SurveyService surveyService, ExcelExportService excelExportService) {
        this.surveyService = surveyService;
        this.excelExportService = excelExportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SurveyDto>> createSurvey(@Valid @RequestBody SurveyCreateRequest request) {
        SurveyDto survey = surveyService.createSurvey(request);
        return ResponseEntity.ok(ApiResponse.ok("Survey created", survey));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SurveyDto>> updateSurvey(@PathVariable Long id,
                                                                @Valid @RequestBody SurveyCreateRequest request) {
        SurveyDto survey = surveyService.updateSurvey(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Survey updated", survey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SurveyDto>> getSurvey(@PathVariable Long id) {
        SurveyDto survey = surveyService.getSurveyById(id);
        return ResponseEntity.ok(ApiResponse.ok(survey));
    }

    @GetMapping("/s/{shareId}")
    public ResponseEntity<ApiResponse<SurveyDto>> getSurveyByShareId(@PathVariable String shareId) {
        SurveyDto survey = surveyService.getSurveyByShareId(shareId);
        return ResponseEntity.ok(ApiResponse.ok(survey));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<SurveyDto>>> getMySurveys(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SurveyDto> surveys = surveyService.getMySurveys(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(surveys));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<SurveyDto>>> getPublicSurveys(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SurveyDto> surveys = surveyService.getPublicSurveys(pageable);
        return ResponseEntity.ok(ApiResponse.ok(surveys));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<Page<SurveyDto>>> getTemplates(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SurveyDto> surveys = surveyService.getTemplates(pageable);
        return ResponseEntity.ok(ApiResponse.ok(surveys));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<SurveyDto>> publishSurvey(@PathVariable Long id) {
        SurveyDto survey = surveyService.publishSurvey(id);
        return ResponseEntity.ok(ApiResponse.ok("Survey published", survey));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<SurveyDto>> closeSurvey(@PathVariable Long id) {
        SurveyDto survey = surveyService.closeSurvey(id);
        return ResponseEntity.ok(ApiResponse.ok("Survey closed", survey));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSurvey(@PathVariable Long id) {
        surveyService.deleteSurvey(id);
        return ResponseEntity.ok(ApiResponse.ok("Survey deleted", null));
    }

    @PostMapping("/s/{shareId}/submit")
    public ResponseEntity<ApiResponse<SurveyResponseDto>> submitSurvey(
            @PathVariable String shareId,
            @Valid @RequestBody SurveySubmitRequest request,
            HttpServletRequest httpRequest) {
        SurveyResponseDto response = surveyService.submitSurvey(shareId, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Survey submitted", response));
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<ApiResponse<Page<SurveyResponseDto>>> getResponses(
            @PathVariable Long id,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SurveyResponseDto> responses = surveyService.getResponses(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<SurveyStatsDto>> getStatistics(@PathVariable Long id) {
        SurveyStatsDto stats = surveyService.getStatistics(id);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/{id}/export")
    public void exportResponses(@PathVariable Long id, HttpServletResponse response) throws IOException {
        excelExportService.exportSurveyResponses(id, response);
    }
}
