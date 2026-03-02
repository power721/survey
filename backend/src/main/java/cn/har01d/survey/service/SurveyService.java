package cn.har01d.survey.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.har01d.survey.dto.CreatorDto;
import cn.har01d.survey.dto.survey.AnswerDto;
import cn.har01d.survey.dto.survey.AnswerRequest;
import cn.har01d.survey.dto.survey.OptionDto;
import cn.har01d.survey.dto.survey.OptionRequest;
import cn.har01d.survey.dto.survey.QuestionDto;
import cn.har01d.survey.dto.survey.QuestionRequest;
import cn.har01d.survey.dto.survey.QuestionStatsDto;
import cn.har01d.survey.dto.survey.SurveyCreateRequest;
import cn.har01d.survey.dto.survey.SurveyDto;
import cn.har01d.survey.dto.survey.SurveyListDto;
import cn.har01d.survey.dto.survey.SurveyResponseDto;
import cn.har01d.survey.dto.survey.SurveySectionDto;
import cn.har01d.survey.dto.survey.SurveySectionRequest;
import cn.har01d.survey.dto.survey.SurveyStatsDto;
import cn.har01d.survey.dto.survey.SurveySubmitRequest;
import cn.har01d.survey.entity.Answer;
import cn.har01d.survey.entity.Question;
import cn.har01d.survey.entity.QuestionOption;
import cn.har01d.survey.entity.Survey;
import cn.har01d.survey.entity.SurveyResponse;
import cn.har01d.survey.entity.SurveySection;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.AnswerRepository;
import cn.har01d.survey.repository.QuestionOptionRepository;
import cn.har01d.survey.repository.QuestionRepository;
import cn.har01d.survey.repository.SurveyRepository;
import cn.har01d.survey.repository.SurveyResponseRepository;
import cn.har01d.survey.repository.SurveySectionRepository;
import cn.har01d.survey.util.GravatarUtil;
import cn.har01d.survey.util.HtmlSanitizer;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final SurveyResponseRepository responseRepository;
    private final AnswerRepository answerRepository;
    private final SurveySectionRepository sectionRepository;
    private final AuthService authService;
    private final FileService fileService;

    public SurveyService(SurveyRepository surveyRepository, QuestionRepository questionRepository,
                         QuestionOptionRepository optionRepository, SurveyResponseRepository responseRepository,
                         AnswerRepository answerRepository, SurveySectionRepository sectionRepository,
                         AuthService authService, FileService fileService) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
        this.sectionRepository = sectionRepository;
        this.authService = authService;
        this.fileService = fileService;
    }

    @Transactional
    public SurveyDto createSurvey(SurveyCreateRequest request) {
        User user = authService.getCurrentUser();
        if (user == null) {
            throw new BusinessException("auth.not.authenticated", HttpStatus.UNAUTHORIZED);
        }

        Instant endTime = request.getEndTime() != null ? request.getEndTime() : Instant.now().plus(Duration.ofDays(30));
        if (request.getStartTime() != null && !endTime.isAfter(request.getStartTime())) {
            throw new BusinessException("error.endTimeBeforeStartTime");
        }
        boolean hasSections = request.getSections() != null && !request.getSections().isEmpty();
        boolean hasQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();
        if (!hasSections && !hasQuestions) {
            throw new BusinessException("survey.atLeastOneQuestion");
        }
        if (hasSections) {
            boolean anyQuestion = request.getSections().stream()
                    .anyMatch(s -> s.getQuestions() != null && !s.getQuestions().isEmpty());
            if (!anyQuestion) {
                throw new BusinessException("survey.atLeastOneQuestion");
            }
        }

        Survey survey = Survey.builder()
                .shareId(generateShareId())
                .title(request.getTitle())
                .description(HtmlSanitizer.sanitize(request.getDescription()))
                .logoUrl(request.getLogoUrl())
                .backgroundUrl(request.getBackgroundUrl())
                .user(user)
                .status(Survey.SurveyStatus.DRAFT)
                .accessLevel(Survey.AccessLevel.valueOf(request.getAccessLevel()))
                .anonymous(request.isAnonymous())
                .template(request.isTemplate())
                .allowUpdate(request.isAllowUpdate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .questions(new ArrayList<>())
                .sections(new ArrayList<>())
                .build();

        if (hasSections) {
            for (int si = 0; si < request.getSections().size(); si++) {
                SurveySectionRequest sr = request.getSections().get(si);
                SurveySection section = SurveySection.builder()
                        .survey(survey)
                        .title(sr.getTitle())
                        .sortOrder(sr.getSortOrder() > 0 ? sr.getSortOrder() : si)
                        .questions(new ArrayList<>())
                        .build();
                if (sr.getQuestions() != null) {
                    for (int i = 0; i < sr.getQuestions().size(); i++) {
                        section.getQuestions().add(buildQuestion(survey, section, sr.getQuestions().get(i), i));
                    }
                }
                survey.getSections().add(section);
            }
        } else {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                survey.getQuestions().add(buildQuestion(survey, null, request.getQuestions().get(i), i));
            }
        }

        survey = surveyRepository.save(survey);
        return toDto(survey);
    }

    @Transactional
    public SurveyDto updateSurvey(Long id, SurveyCreateRequest request) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));

        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }

        survey.setTitle(request.getTitle());
        survey.setDescription(HtmlSanitizer.sanitize(request.getDescription()));
        survey.setLogoUrl(request.getLogoUrl());
        survey.setBackgroundUrl(request.getBackgroundUrl());
        survey.setAccessLevel(Survey.AccessLevel.valueOf(request.getAccessLevel()));
        survey.setAnonymous(request.isAnonymous());
        survey.setTemplate(request.isTemplate());
        survey.setAllowUpdate(request.isAllowUpdate());
        Instant endTime = request.getEndTime() != null ? request.getEndTime() : Instant.now().plus(Duration.ofDays(30));
        if (request.getStartTime() != null && !endTime.isAfter(request.getStartTime())) {
            throw new BusinessException("error.endTimeBeforeStartTime");
        }
        boolean hasSections = request.getSections() != null && !request.getSections().isEmpty();
        boolean hasQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();
        if (!hasSections && !hasQuestions) {
            throw new BusinessException("survey.atLeastOneQuestion");
        }
        if (hasSections) {
            boolean anyQuestion = request.getSections().stream()
                    .anyMatch(s -> s.getQuestions() != null && !s.getQuestions().isEmpty());
            if (!anyQuestion) {
                throw new BusinessException("survey.atLeastOneQuestion");
            }
        }
        survey.setStartTime(request.getStartTime());
        survey.setEndTime(endTime);

        if (hasSections) {
            // Clear flat questions when switching to sections
            Set<Long> allFlatIds = survey.getQuestions().stream()
                    .filter(q -> q.getId() != null).map(Question::getId).collect(Collectors.toSet());
            for (Long qid : allFlatIds) answerRepository.deleteByQuestionId(qid);
            survey.getQuestions().clear();

            // Build map of existing sections
            Map<Long, SurveySection> existingSectionMap = survey.getSections().stream()
                    .filter(s -> s.getId() != null)
                    .collect(Collectors.toMap(SurveySection::getId, s -> s));
            Set<Long> requestSectionIds = request.getSections().stream()
                    .filter(s -> s.getId() != null).map(SurveySectionRequest::getId).collect(Collectors.toSet());

            // Delete removed sections (cascade removes their questions)
            survey.getSections().removeIf(s -> s.getId() != null && !requestSectionIds.contains(s.getId()));

            for (int si = 0; si < request.getSections().size(); si++) {
                SurveySectionRequest sr = request.getSections().get(si);
                SurveySection section;
                if (sr.getId() != null && existingSectionMap.containsKey(sr.getId())) {
                    section = existingSectionMap.get(sr.getId());
                    section.setTitle(sr.getTitle());
                    section.setSortOrder(sr.getSortOrder() > 0 ? sr.getSortOrder() : si);
                } else {
                    section = SurveySection.builder()
                            .survey(survey).title(sr.getTitle())
                            .sortOrder(sr.getSortOrder() > 0 ? sr.getSortOrder() : si)
                            .questions(new ArrayList<>()).build();
                    survey.getSections().add(section);
                }
                // Rebuild questions in this section
                Map<Long, Question> existingQMap = section.getQuestions().stream()
                        .filter(q -> q.getId() != null).collect(Collectors.toMap(Question::getId, q -> q));
                Set<Long> reqQIds = sr.getQuestions() == null ? new HashSet<>() : sr.getQuestions().stream()
                        .filter(q -> q.getId() != null).map(QuestionRequest::getId).collect(Collectors.toSet());
                for (Long qid : existingQMap.keySet()) {
                    if (!reqQIds.contains(qid)) answerRepository.deleteByQuestionId(qid);
                }
                section.getQuestions().removeIf(q -> q.getId() != null && !reqQIds.contains(q.getId()));
                if (sr.getQuestions() != null) {
                    for (int i = 0; i < sr.getQuestions().size(); i++) {
                        QuestionRequest qr = sr.getQuestions().get(i);
                        if (qr.getId() != null && existingQMap.containsKey(qr.getId())) {
                            updateQuestion(existingQMap.get(qr.getId()), qr, i);
                        } else {
                            section.getQuestions().add(buildQuestion(survey, section, qr, i));
                        }
                    }
                }
            }
        } else {
            // Clear sections when using flat questions
            survey.getSections().clear();

            Map<Long, Question> existingQuestionMap = survey.getQuestions().stream()
                    .filter(q -> q.getId() != null)
                    .collect(Collectors.toMap(Question::getId, q -> q));
            Set<Long> requestQuestionIds = request.getQuestions().stream()
                    .filter(qr -> qr.getId() != null).map(QuestionRequest::getId).collect(Collectors.toSet());
            for (Long existingId : existingQuestionMap.keySet()) {
                if (!requestQuestionIds.contains(existingId)) answerRepository.deleteByQuestionId(existingId);
            }
            survey.getQuestions().removeIf(q -> q.getId() != null && !requestQuestionIds.contains(q.getId()));
            for (int i = 0; i < request.getQuestions().size(); i++) {
                QuestionRequest qr = request.getQuestions().get(i);
                if (qr.getId() != null && existingQuestionMap.containsKey(qr.getId())) {
                    updateQuestion(existingQuestionMap.get(qr.getId()), qr, i);
                } else {
                    survey.getQuestions().add(buildQuestion(survey, null, qr, i));
                }
            }
        }

        survey = surveyRepository.save(survey);
        return toDto(survey);
    }

    @Transactional(readOnly = true)
    public SurveyDto getSurveyById(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));
        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }
        return toDto(survey);
    }

    @Transactional(readOnly = true)
    public SurveyDto getSurveyByShareId(String shareId) {
        Survey survey = surveyRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));

        if (survey.getStatus() != Survey.SurveyStatus.PUBLISHED) {
            throw new BusinessException("survey.not.published");
        }
        if (survey.getStartTime() != null && survey.getStartTime().isAfter(Instant.now())) {
            SurveyDto dto = new SurveyDto();
            dto.setId(survey.getId());
            dto.setShareId(survey.getShareId());
            dto.setTitle(survey.getTitle());
            dto.setStatus(survey.getStatus().name());
            dto.setStartTime(survey.getStartTime());
            dto.setEndTime(survey.getEndTime());
            dto.setCreator(new CreatorDto(survey.getUser().getUsername(), survey.getUser().getNickname(), resolveAvatar(survey.getUser())));
            dto.setCreatedAt(survey.getCreatedAt());
            dto.setQuestions(List.of());
            dto.setSections(List.of());
            return dto;
        }
        if (survey.getEndTime() != null && survey.getEndTime().isBefore(Instant.now())) {
            throw new BusinessException("survey.closed");
        }
        return toDto(survey);
    }

    @Transactional(readOnly = true)
    public Page<SurveyListDto> getMySurveys(String keyword, Pageable pageable) {
        User user = authService.getCurrentUser();
        if (keyword != null && !keyword.isBlank()) {
            return surveyRepository.findByUserAndTitleContaining(user, keyword, pageable).map(this::toListDto);
        }
        return surveyRepository.findByUser(user, pageable).map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public Page<SurveyListDto> getPublicSurveys(String username, Pageable pageable) {
        if (username != null && !username.isBlank()) {
            return surveyRepository.findByStatusAndAccessLevelAndUserUsername(
                    Survey.SurveyStatus.PUBLISHED, Survey.AccessLevel.PUBLIC, username, pageable).map(this::toListDto);
        }
        return surveyRepository.findByStatusAndAccessLevel(Survey.SurveyStatus.PUBLISHED, Survey.AccessLevel.PUBLIC, pageable)
                .map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public Page<SurveyListDto> getTemplates(Pageable pageable) {
        return surveyRepository.findByTemplateTrue(pageable).map(this::toListDto);
    }

    @Transactional
    public SurveyDto publishSurvey(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));
        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }
        survey.setStatus(Survey.SurveyStatus.PUBLISHED);
        return toDto(surveyRepository.save(survey));
    }

    @Transactional
    public SurveyDto closeSurvey(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));
        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }
        survey.setStatus(Survey.SurveyStatus.CLOSED);
        return toDto(surveyRepository.save(survey));
    }

    @Transactional
    public void deleteSurvey(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));
        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }
        deleteFileAnswers(survey.getId());
        answerRepository.deleteBySurveyId(survey.getId());
        responseRepository.deleteBySurveyId(survey.getId());
        surveyRepository.delete(survey);
    }

    private void deleteFileAnswers(Long surveyId) {
        List<SurveyResponse> responses = responseRepository.findBySurveyId(surveyId);
        for (SurveyResponse response : responses) {
            deleteFileAttachments(response);
        }
    }

    private void deleteFileAttachments(SurveyResponse response) {
        for (Answer answer : response.getAnswers()) {
            if (answer.getTextValue() != null && answer.getTextValue().startsWith("/api/files/")) {
                String fileName = answer.getTextValue().substring("/api/files/".length());
                try {
                    fileService.delete(fileName);
                } catch (Exception e) {
                    // ignore deletion errors
                }
            }
        }
    }

    @Transactional
    public SurveyResponseDto submitSurvey(String shareId, SurveySubmitRequest request, HttpServletRequest httpRequest) {
        Survey survey = surveyRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));

        if (survey.getStatus() != Survey.SurveyStatus.PUBLISHED) {
            throw new BusinessException("survey.not.published");
        }
        if (survey.getStartTime() != null && survey.getStartTime().isAfter(Instant.now())) {
            throw new BusinessException("survey.not.started");
        }
        if (survey.getEndTime() != null && survey.getEndTime().isBefore(Instant.now())) {
            throw new BusinessException("survey.closed");
        }

        User user = authService.getCurrentUser();
        if (!survey.isAnonymous() && user == null) {
            throw new BusinessException("auth.not.authenticated", HttpStatus.UNAUTHORIZED);
        }
        String ip = getClientIp(httpRequest);

        // Duplicate submission check
        if (user != null) {
            if (!survey.isAllowUpdate() && responseRepository.existsBySurveyIdAndUserId(survey.getId(), user.getId())) {
                throw new BusinessException("survey.already.submitted");
            }
        } else {
            // Anonymous survey: check by IP
            if (responseRepository.existsBySurveyIdAndIp(survey.getId(), ip)) {
                throw new BusinessException("survey.already.submitted");
            }
        }

        boolean isUpdate = false;
        SurveyResponse response = null;

        if (survey.isAllowUpdate() && user != null) {
            response = responseRepository.findFirstBySurveyIdAndUserIdOrderByCreatedAtDesc(survey.getId(), user.getId())
                    .orElse(null);
            if (response != null) {
                isUpdate = true;
                deleteFileAttachments(response);
                response.getAnswers().clear();
                response.setIp(ip);
                response.setUserAgent(httpRequest.getHeader("User-Agent"));
            }
        }

        if (response == null) {
            response = SurveyResponse.builder()
                    .survey(survey)
                    .user(user)
                    .ip(ip)
                    .userAgent(httpRequest.getHeader("User-Agent"))
                    .answers(new ArrayList<>())
                    .build();
        }

        Map<Long, Question> questionMap = questionRepository.findBySurveyIdOrderBySortOrderAsc(survey.getId())
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        for (AnswerRequest ar : request.getAnswers()) {
            Question question = questionMap.get(ar.getQuestionId());
            if (question == null) continue;

            Answer answer = Answer.builder()
                    .response(response)
                    .question(question)
                    .build();

            switch (question.getType()) {
                case SINGLE_CHOICE:
                    if (ar.getSelectedOptionId() != null) {
                        QuestionOption opt = optionRepository.findById(ar.getSelectedOptionId()).orElse(null);
                        answer.setSelectedOption(opt);
                    }
                    break;
                case MULTIPLE_CHOICE:
                    if (ar.getSelectedOptionIds() != null && !ar.getSelectedOptionIds().isEmpty()) {
                        answer.setSelectedOptionIds(ar.getSelectedOptionIds().stream()
                                .map(String::valueOf).collect(Collectors.joining(",")));
                    }
                    break;
                default:
                    answer.setTextValue(ar.getTextValue());
                    break;
            }
            response.getAnswers().add(answer);
        }

        if (!isUpdate) {
            survey.setResponseCount(survey.getResponseCount() + 1);
        }
        surveyRepository.save(survey);
        response = responseRepository.save(response);
        return toResponseDto(response, survey.isAnonymous());
    }

    @Transactional(readOnly = true)
    public SurveyResponseDto getMyResponse(String shareId) {
        Survey survey = surveyRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));

        User user = authService.getCurrentUser();
        if (user == null) {
            return null;
        }

        return responseRepository.findFirstBySurveyIdAndUserIdOrderByCreatedAtDesc(survey.getId(), user.getId())
                .map(r -> toResponseDto(r, false))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<SurveyResponseDto> getResponses(Long surveyId, Pageable pageable) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));
        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }
        boolean anonymous = survey.isAnonymous();
        return responseRepository.findBySurveyId(surveyId, pageable).map(r -> toResponseDto(r, anonymous));
    }

    @Transactional(readOnly = true)
    public SurveyStatsDto getStatistics(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("survey.not.found"));
        User user = authService.getCurrentUser();
        if (!survey.getUser().getId().equals(user.getId())) {
            throw new BusinessException("survey.access.denied", HttpStatus.FORBIDDEN);
        }

        SurveyStatsDto stats = new SurveyStatsDto();
        stats.setSurveyId(survey.getId());
        stats.setTitle(survey.getTitle());
        stats.setTotalResponses(survey.getResponseCount());

        List<QuestionStatsDto> questionStats = new ArrayList<>();
        for (Question question : survey.getQuestions()) {
            QuestionStatsDto qs = new QuestionStatsDto();
            qs.setQuestionId(question.getId());
            qs.setQuestionTitle(question.getTitle());
            qs.setQuestionType(question.getType().name());

            if (question.getType() == Question.QuestionType.SINGLE_CHOICE) {
                List<Object[]> counts = answerRepository.countByQuestionGroupByOption(question.getId());
                Map<Long, Long> countMap = new HashMap<>();
                long total = 0;
                for (Object[] row : counts) {
                    Long optId = (Long) row[0];
                    Long count = (Long) row[1];
                    countMap.put(optId, count);
                    total += count;
                }

                List<QuestionStatsDto.OptionStatsDto> optionStats = new ArrayList<>();
                for (QuestionOption opt : question.getOptions()) {
                    QuestionStatsDto.OptionStatsDto os = new QuestionStatsDto.OptionStatsDto();
                    os.setOptionId(opt.getId());
                    os.setContent(opt.getContent());
                    long count = countMap.getOrDefault(opt.getId(), 0L);
                    os.setCount(count);
                    os.setPercentage(total > 0 ? (double) count / total * 100 : 0);
                    optionStats.add(os);
                }
                qs.setOptionStats(optionStats);
            } else if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
                Map<Long, Long> countMap = new HashMap<>();
                long total = 0;
                List<String> allSelections = answerRepository.findTextValuesByQuestionId(question.getId());
                // For multiple choice, we stored IDs in selectedOptionIds field
                List<Answer> answers = answerRepository.findAll().stream()
                        .filter(a -> a.getQuestion().getId().equals(question.getId()) && a.getSelectedOptionIds() != null)
                        .toList();
                for (Answer a : answers) {
                    String[] ids = a.getSelectedOptionIds().split(",");
                    for (String idStr : ids) {
                        Long optId = Long.parseLong(idStr.trim());
                        countMap.merge(optId, 1L, Long::sum);
                        total++;
                    }
                }

                List<QuestionStatsDto.OptionStatsDto> optionStats = new ArrayList<>();
                for (QuestionOption opt : question.getOptions()) {
                    QuestionStatsDto.OptionStatsDto os = new QuestionStatsDto.OptionStatsDto();
                    os.setOptionId(opt.getId());
                    os.setContent(opt.getContent());
                    long count = countMap.getOrDefault(opt.getId(), 0L);
                    os.setCount(count);
                    os.setPercentage(total > 0 ? (double) count / total * 100 : 0);
                    optionStats.add(os);
                }
                qs.setOptionStats(optionStats);
            } else {
                List<String> textValues = answerRepository.findTextValuesByQuestionId(question.getId());
                qs.setTextAnswers(textValues);
            }
            questionStats.add(qs);
        }
        stats.setQuestionStats(questionStats);
        return stats;
    }

    private String generateShareId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private SurveyListDto toListDto(Survey survey) {
        SurveyListDto dto = new SurveyListDto();
        dto.setId(survey.getId());
        dto.setShareId(survey.getShareId());
        dto.setTitle(survey.getTitle());
        dto.setLogoUrl(survey.getLogoUrl());
        dto.setStatus(survey.getStatus().name());
        dto.setAccessLevel(survey.getAccessLevel().name());
        dto.setAnonymous(survey.isAnonymous());
        dto.setTemplate(survey.isTemplate());
        dto.setAllowUpdate(survey.isAllowUpdate());
        dto.setStartTime(survey.getStartTime());
        dto.setEndTime(survey.getEndTime());
        dto.setResponseCount(survey.getResponseCount());
        dto.setCreator(new CreatorDto(survey.getUser().getUsername(), survey.getUser().getNickname(), resolveAvatar(survey.getUser())));
        dto.setCreatedAt(survey.getCreatedAt());
        dto.setUpdatedAt(survey.getUpdatedAt());
        return dto;
    }

    private SurveyDto toDto(Survey survey) {
        SurveyDto dto = new SurveyDto();
        dto.setId(survey.getId());
        dto.setShareId(survey.getShareId());
        dto.setTitle(survey.getTitle());
        dto.setDescription(survey.getDescription());
        dto.setLogoUrl(survey.getLogoUrl());
        dto.setBackgroundUrl(survey.getBackgroundUrl());
        dto.setStatus(survey.getStatus().name());
        dto.setAccessLevel(survey.getAccessLevel().name());
        dto.setAnonymous(survey.isAnonymous());
        dto.setTemplate(survey.isTemplate());
        dto.setAllowUpdate(survey.isAllowUpdate());
        dto.setStartTime(survey.getStartTime());
        dto.setEndTime(survey.getEndTime());
        dto.setResponseCount(survey.getResponseCount());
        dto.setCreator(new CreatorDto(survey.getUser().getUsername(), survey.getUser().getNickname(), resolveAvatar(survey.getUser())));
        dto.setCreatedAt(survey.getCreatedAt());
        dto.setUpdatedAt(survey.getUpdatedAt());

        if (survey.getSections() != null && !survey.getSections().isEmpty()) {
            dto.setSections(survey.getSections().stream().map(this::toSectionDto).toList());
            dto.setQuestions(List.of());
        } else if (survey.getQuestions() != null) {
            dto.setQuestions(survey.getQuestions().stream().map(this::toQuestionDto).toList());
            dto.setSections(List.of());
        }
        return dto;
    }

    private SurveySectionDto toSectionDto(SurveySection section) {
        SurveySectionDto dto = new SurveySectionDto();
        dto.setId(section.getId());
        dto.setTitle(section.getTitle());
        dto.setSortOrder(section.getSortOrder());
        if (section.getQuestions() != null) {
            dto.setQuestions(section.getQuestions().stream().map(this::toQuestionDto).toList());
        }
        return dto;
    }

    private Question buildQuestion(Survey survey, SurveySection section, QuestionRequest qr, int index) {
        Question question = Question.builder()
                .survey(survey)
                .section(section)
                .type(Question.QuestionType.valueOf(qr.getType()))
                .title(qr.getTitle())
                .description(qr.getDescription())
                .required(qr.isRequired())
                .sortOrder(qr.getSortOrder() > 0 ? qr.getSortOrder() : index)
                .options(new ArrayList<>())
                .build();
        if (qr.getOptions() != null) {
            for (int j = 0; j < qr.getOptions().size(); j++) {
                OptionRequest or = qr.getOptions().get(j);
                question.getOptions().add(QuestionOption.builder()
                        .question(question)
                        .content(or.getContent())
                        .sortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : j)
                        .build());
            }
        }
        return question;
    }

    private void updateQuestion(Question question, QuestionRequest qr, int index) {
        question.setType(Question.QuestionType.valueOf(qr.getType()));
        question.setTitle(qr.getTitle());
        question.setDescription(qr.getDescription());
        question.setRequired(qr.isRequired());
        question.setSortOrder(qr.getSortOrder() > 0 ? qr.getSortOrder() : index);
        Map<Long, QuestionOption> existingOptionMap = question.getOptions().stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(QuestionOption::getId, o -> o));
        Set<Long> requestOptionIds = new HashSet<>();
        if (qr.getOptions() != null) {
            for (OptionRequest or : qr.getOptions()) {
                if (or.getId() != null) requestOptionIds.add(or.getId());
            }
        }
        question.getOptions().removeIf(o -> o.getId() != null && !requestOptionIds.contains(o.getId()));
        if (qr.getOptions() != null) {
            for (int j = 0; j < qr.getOptions().size(); j++) {
                OptionRequest or = qr.getOptions().get(j);
                if (or.getId() != null && existingOptionMap.containsKey(or.getId())) {
                    QuestionOption option = existingOptionMap.get(or.getId());
                    option.setContent(or.getContent());
                    option.setSortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : j);
                } else {
                    question.getOptions().add(QuestionOption.builder()
                            .question(question)
                            .content(or.getContent())
                            .sortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : j)
                            .build());
                }
            }
        }
    }

    private QuestionDto toQuestionDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setType(question.getType().name());
        dto.setTitle(question.getTitle());
        dto.setDescription(question.getDescription());
        dto.setRequired(question.isRequired());
        dto.setSortOrder(question.getSortOrder());
        if (question.getOptions() != null) {
            dto.setOptions(question.getOptions().stream().map(this::toOptionDto).toList());
        }
        return dto;
    }

    private OptionDto toOptionDto(QuestionOption option) {
        OptionDto dto = new OptionDto();
        dto.setId(option.getId());
        dto.setContent(option.getContent());
        dto.setSortOrder(option.getSortOrder());
        return dto;
    }

    private SurveyResponseDto toResponseDtoWithAnswers(SurveyResponse response, boolean anonymous) {
        SurveyResponseDto dto = new SurveyResponseDto();
        dto.setId(response.getId());
        dto.setIp(response.getIp());
        dto.setCreatedAt(response.getCreatedAt());
        if (!anonymous && response.getUser() != null) {
            dto.setUsername(response.getUser().getUsername());
            dto.setNickname(response.getUser().getNickname());
        }
        dto.setAnswers(answerRepository.findByResponseId(response.getId()).stream().map(this::toAnswerDto).toList());
        return dto;
    }

    private SurveyResponseDto toResponseDto(SurveyResponse response, boolean anonymous) {
        SurveyResponseDto dto = new SurveyResponseDto();
        dto.setId(response.getId());
        dto.setIp(response.getIp());
        dto.setCreatedAt(response.getCreatedAt());
        if (!anonymous && response.getUser() != null) {
            dto.setUsername(response.getUser().getUsername());
            dto.setNickname(response.getUser().getNickname());
        }
        List<Answer> answers = (response.getAnswers() != null && !response.getAnswers().isEmpty())
                ? response.getAnswers()
                : answerRepository.findByResponseId(response.getId());
        dto.setAnswers(answers.stream().map(this::toAnswerDto).toList());
        return dto;
    }

    private AnswerDto toAnswerDto(Answer answer) {
        AnswerDto dto = new AnswerDto();
        dto.setId(answer.getId());
        dto.setQuestionId(answer.getQuestion().getId());
        dto.setQuestionTitle(answer.getQuestion().getTitle());
        dto.setTextValue(answer.getTextValue());
        if (answer.getSelectedOption() != null) {
            dto.setSelectedOptionId(answer.getSelectedOption().getId());
            dto.setSelectedOptionContent(answer.getSelectedOption().getContent());
        }
        if (answer.getSelectedOptionIds() != null) {
            List<Long> optionIds = Arrays.stream(answer.getSelectedOptionIds().split(","))
                    .map(s -> Long.parseLong(s.trim())).toList();
            dto.setSelectedOptionIds(optionIds);
            List<String> contents = optionIds.stream()
                    .map(id -> optionRepository.findById(id).map(QuestionOption::getContent).orElse(String.valueOf(id)))
                    .toList();
            dto.setSelectedOptionContents(contents);
        }
        return dto;
    }

    private String resolveAvatar(User user) {
        if (user.getAvatar() != null && !user.getAvatar().isBlank()) {
            return user.getAvatar();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return GravatarUtil.getAvatarUrl(user.getEmail());
        }
        return GravatarUtil.getAvatarUrl(user.getUsername());
    }
}
