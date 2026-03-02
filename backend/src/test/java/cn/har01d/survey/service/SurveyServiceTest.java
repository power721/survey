package cn.har01d.survey.service;

import cn.har01d.survey.dto.survey.*;
import cn.har01d.survey.entity.*;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository optionRepository;

    @Mock
    private SurveyResponseRepository responseRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AuthService authService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SurveyService surveyService;

    private User testUser;
    private User otherUser;
    private Survey testSurvey;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").nickname("TestNick").role(User.Role.USER).build();
        otherUser = User.builder().id(2L).username("other").nickname("Other").role(User.Role.USER).build();

        QuestionOption opt1 = QuestionOption.builder().id(1L).content("Option A").sortOrder(0).build();
        QuestionOption opt2 = QuestionOption.builder().id(2L).content("Option B").sortOrder(1).build();
        Question question = Question.builder()
                .id(1L).type(Question.QuestionType.SINGLE_CHOICE).title("Q1")
                .required(true).sortOrder(0).options(new ArrayList<>(List.of(opt1, opt2))).build();
        opt1.setQuestion(question);
        opt2.setQuestion(question);

        testSurvey = Survey.builder()
                .id(1L).shareId("abc123").title("Test Survey").description("desc")
                .user(testUser).status(Survey.SurveyStatus.DRAFT)
                .accessLevel(Survey.AccessLevel.PUBLIC).anonymous(true)
                .questions(new ArrayList<>(List.of(question)))
                .sections(new ArrayList<>()).build();
        question.setSurvey(testSurvey);
    }

    // --- createSurvey ---

    @Test
    void createSurvey_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(inv -> {
            Survey s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle("New Survey");
        request.setDescription("desc");
        request.setAccessLevel("PUBLIC");

        QuestionRequest qr = new QuestionRequest();
        qr.setType("TEXT");
        qr.setTitle("What?");
        request.setQuestions(List.of(qr));

        SurveyDto dto = surveyService.createSurvey(request);

        assertNotNull(dto);
        assertEquals("New Survey", dto.getTitle());
        assertEquals(1, dto.getQuestions().size());
        verify(surveyRepository).save(any(Survey.class));
    }

    @Test
    void createSurvey_notAuthenticated() {
        when(authService.getCurrentUser()).thenReturn(null);

        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle("Test");

        assertThrows(BusinessException.class, () -> surveyService.createSurvey(request));
    }

    @Test
    void createSurvey_withOptions() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(inv -> {
            Survey s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        OptionRequest or1 = new OptionRequest();
        or1.setContent("Yes");
        OptionRequest or2 = new OptionRequest();
        or2.setContent("No");

        QuestionRequest qr = new QuestionRequest();
        qr.setType("SINGLE_CHOICE");
        qr.setTitle("Choose");
        qr.setOptions(List.of(or1, or2));

        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle("Poll");
        request.setQuestions(List.of(qr));

        SurveyDto dto = surveyService.createSurvey(request);

        assertEquals(1, dto.getQuestions().size());
        assertEquals(2, dto.getQuestions().get(0).getOptions().size());
    }

    // --- getSurveyById ---

    @Test
    void getSurveyById_success() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);

        SurveyDto dto = surveyService.getSurveyById(1L);

        assertNotNull(dto);
        assertEquals("Test Survey", dto.getTitle());
    }

    @Test
    void getSurveyById_notFound() {
        when(surveyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> surveyService.getSurveyById(99L));
    }

    @Test
    void getSurveyById_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> surveyService.getSurveyById(1L));
    }

    // --- getSurveyByShareId ---

    @Test
    void getSurveyByShareId_success() {
        testSurvey.setStatus(Survey.SurveyStatus.PUBLISHED);
        when(surveyRepository.findByShareId("abc123")).thenReturn(Optional.of(testSurvey));

        SurveyDto dto = surveyService.getSurveyByShareId("abc123");

        assertNotNull(dto);
        assertEquals("Test Survey", dto.getTitle());
    }

    @Test
    void getSurveyByShareId_notPublished() {
        testSurvey.setStatus(Survey.SurveyStatus.DRAFT);
        when(surveyRepository.findByShareId("abc123")).thenReturn(Optional.of(testSurvey));

        assertThrows(BusinessException.class, () -> surveyService.getSurveyByShareId("abc123"));
    }

    @Test
    void getSurveyByShareId_expired() {
        testSurvey.setStatus(Survey.SurveyStatus.PUBLISHED);
        testSurvey.setEndTime(Instant.now().minusSeconds(3600));
        when(surveyRepository.findByShareId("abc123")).thenReturn(Optional.of(testSurvey));

        assertThrows(BusinessException.class, () -> surveyService.getSurveyByShareId("abc123"));
    }

    @Test
    void getSurveyByShareId_notFound() {
        when(surveyRepository.findByShareId("nonexist")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> surveyService.getSurveyByShareId("nonexist"));
    }

    // --- getMySurveys ---

    @Test
    void getMySurveys_noKeyword() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        when(surveyRepository.findByUser(testUser, pageable)).thenReturn(new PageImpl<>(List.of(testSurvey)));

        Page<SurveyListDto> page = surveyService.getMySurveys(null, pageable);

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getMySurveys_withKeyword() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        when(surveyRepository.findByUserAndTitleContaining(testUser, "Test", pageable))
                .thenReturn(new PageImpl<>(List.of(testSurvey)));

        Page<SurveyListDto> page = surveyService.getMySurveys("Test", pageable);

        assertEquals(1, page.getTotalElements());
    }

    // --- publishSurvey ---

    @Test
    void publishSurvey_success() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(inv -> inv.getArgument(0));

        SurveyDto dto = surveyService.publishSurvey(1L);

        assertEquals("PUBLISHED", dto.getStatus());
    }

    @Test
    void publishSurvey_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> surveyService.publishSurvey(1L));
    }

    // --- closeSurvey ---

    @Test
    void closeSurvey_success() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(inv -> inv.getArgument(0));

        SurveyDto dto = surveyService.closeSurvey(1L);

        assertEquals("CLOSED", dto.getStatus());
    }

    @Test
    void closeSurvey_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> surveyService.closeSurvey(1L));
    }

    // --- deleteSurvey ---

    @Test
    void deleteSurvey_success() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);

        surveyService.deleteSurvey(1L);

        verify(surveyRepository).delete(testSurvey);
    }

    @Test
    void deleteSurvey_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> surveyService.deleteSurvey(1L));
    }

    @Test
    void deleteSurvey_notFound() {
        when(surveyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> surveyService.deleteSurvey(99L));
    }

    // --- getResponses ---

    @Test
    void getResponses_success() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        when(responseRepository.findBySurveyId(1L, pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<SurveyResponseDto> page = surveyService.getResponses(1L, pageable);

        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void getResponses_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> surveyService.getResponses(1L, PageRequest.of(0, 10)));
    }

    // --- updateSurvey ---

    @Test
    void updateSurvey_success() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(inv -> inv.getArgument(0));

        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle("Updated Title");
        request.setAccessLevel("PRIVATE");

        QuestionRequest qr = new QuestionRequest();
        qr.setId(1L);
        qr.setType("SINGLE_CHOICE");
        qr.setTitle("Updated Q1");
        OptionRequest or1 = new OptionRequest();
        or1.setId(1L);
        or1.setContent("Updated A");
        qr.setOptions(List.of(or1));
        request.setQuestions(List.of(qr));

        SurveyDto dto = surveyService.updateSurvey(1L, request);

        assertEquals("Updated Title", dto.getTitle());
    }

    @Test
    void updateSurvey_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle("Hacked");

        assertThrows(BusinessException.class, () -> surveyService.updateSurvey(1L, request));
    }

    @Test
    void updateSurvey_removedQuestion_deletesAnswers() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(inv -> inv.getArgument(0));

        // Send a new question to replace the existing one — removes existing question id=1
        QuestionRequest qr = new QuestionRequest();
        qr.setType("TEXT");
        qr.setTitle("New Question");

        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle("Updated");
        request.setAccessLevel("PUBLIC");
        request.setQuestions(List.of(qr));

        surveyService.updateSurvey(1L, request);

        verify(answerRepository).deleteByQuestionId(1L);
    }
}
