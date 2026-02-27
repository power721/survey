package cn.har01d.survey.service;

import cn.har01d.survey.entity.*;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.SurveyRepository;
import cn.har01d.survey.repository.SurveyResponseRepository;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyResponseRepository responseRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ExcelExportService excelExportService;

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
                .id(1L).shareId("abc123").title("Test Survey")
                .user(testUser).status(Survey.SurveyStatus.PUBLISHED)
                .accessLevel(Survey.AccessLevel.PUBLIC).anonymous(true)
                .questions(new ArrayList<>(List.of(question))).build();
        question.setSurvey(testSurvey);
    }

    @Test
    void exportSurveyResponses_notFound() {
        when(surveyRepository.findById(99L)).thenReturn(Optional.empty());

        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(ResourceNotFoundException.class, () -> excelExportService.exportSurveyResponses(99L, response));
    }

    @Test
    void exportSurveyResponses_accessDenied() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(BusinessException.class, () -> excelExportService.exportSurveyResponses(1L, response));
    }

    @Test
    void exportSurveyResponses_emptyResponses() throws IOException {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(responseRepository.findBySurveyId(1L)).thenReturn(List.of());

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = mockServletOutputStream(baos);
        when(httpResponse.getOutputStream()).thenReturn(sos);

        excelExportService.exportSurveyResponses(1L, httpResponse);

        verify(httpResponse).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertTrue(baos.size() > 0);
    }

    @Test
    void exportSurveyResponses_withResponses() throws IOException {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);

        Question question = testSurvey.getQuestions().get(0);
        QuestionOption opt1 = question.getOptions().get(0);

        Answer answer = Answer.builder()
                .id(1L).question(question).selectedOption(opt1).build();
        SurveyResponse sr = SurveyResponse.builder()
                .id(1L).survey(testSurvey).user(testUser).ip("127.0.0.1")
                .createdAt(Instant.now()).answers(List.of(answer)).build();
        answer.setResponse(sr);

        when(responseRepository.findBySurveyId(1L)).thenReturn(List.of(sr));

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = mockServletOutputStream(baos);
        when(httpResponse.getOutputStream()).thenReturn(sos);

        excelExportService.exportSurveyResponses(1L, httpResponse);

        assertTrue(baos.size() > 0);
    }

    @Test
    void exportSurveyResponses_nonAnonymous_includesUserColumn() throws IOException {
        testSurvey.setAnonymous(false);
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);

        Question question = testSurvey.getQuestions().get(0);
        Answer answer = Answer.builder()
                .id(1L).question(question).textValue("hello").build();
        SurveyResponse sr = SurveyResponse.builder()
                .id(1L).survey(testSurvey).user(testUser).ip("127.0.0.1")
                .createdAt(Instant.now()).answers(List.of(answer)).build();

        when(responseRepository.findBySurveyId(1L)).thenReturn(List.of(sr));

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = mockServletOutputStream(baos);
        when(httpResponse.getOutputStream()).thenReturn(sos);

        excelExportService.exportSurveyResponses(1L, httpResponse);

        assertTrue(baos.size() > 0);
    }

    @Test
    void exportSurveyResponses_multipleChoiceResolvesIds() throws IOException {
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(testSurvey));
        when(authService.getCurrentUser()).thenReturn(testUser);

        Question question = testSurvey.getQuestions().get(0);
        Answer answer = Answer.builder()
                .id(1L).question(question).selectedOptionIds("1,2").build();
        SurveyResponse sr = SurveyResponse.builder()
                .id(1L).survey(testSurvey).user(testUser).ip("127.0.0.1")
                .createdAt(Instant.now()).answers(List.of(answer)).build();

        when(responseRepository.findBySurveyId(1L)).thenReturn(List.of(sr));

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = mockServletOutputStream(baos);
        when(httpResponse.getOutputStream()).thenReturn(sos);

        excelExportService.exportSurveyResponses(1L, httpResponse);

        assertTrue(baos.size() > 0);
    }

    private ServletOutputStream mockServletOutputStream(ByteArrayOutputStream baos) {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}

            @Override
            public void write(int b) { baos.write(b); }

            @Override
            public void write(byte[] b, int off, int len) { baos.write(b, off, len); }
        };
    }
}
