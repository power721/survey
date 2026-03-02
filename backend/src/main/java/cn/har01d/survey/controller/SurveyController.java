package cn.har01d.survey.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.survey.SurveyCreateRequest;
import cn.har01d.survey.dto.survey.SurveyDto;
import cn.har01d.survey.dto.survey.SurveyResponseDto;
import cn.har01d.survey.dto.survey.SurveyStatsDto;
import cn.har01d.survey.dto.survey.SurveySubmitRequest;
import cn.har01d.survey.service.ExcelExportService;
import cn.har01d.survey.service.SurveyService;

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
            @RequestParam(required = false) String username,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SurveyDto> surveys = surveyService.getPublicSurveys(username, pageable);
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

    @GetMapping("/s/{shareId}/my-response")
    public ResponseEntity<ApiResponse<SurveyResponseDto>> getMyResponse(@PathVariable String shareId) {
        SurveyResponseDto response = surveyService.getMyResponse(shareId);
        return ResponseEntity.ok(ApiResponse.ok(response));
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
