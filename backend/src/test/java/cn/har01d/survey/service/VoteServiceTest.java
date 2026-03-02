package cn.har01d.survey.service;

import cn.har01d.survey.dto.vote.*;
import cn.har01d.survey.entity.*;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.*;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VotePollRepository pollRepository;

    @Mock
    private VoteOptionRepository optionRepository;

    @Mock
    private VoteRecordRepository recordRepository;

    @Mock
    private AuthService authService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private VoteService voteService;

    private User testUser;
    private User otherUser;
    private VotePoll testPoll;
    private VoteOption testOption;
    private VoteOption testOption2;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").nickname("TestNick").role(User.Role.USER).build();
        otherUser = User.builder().id(2L).username("other").nickname("Other").role(User.Role.USER).build();

        testOption = VoteOption.builder().id(1L).title("Option A").sortOrder(0).voteCount(0).build();
        testOption2 = VoteOption.builder().id(2L).title("Option B").sortOrder(1).voteCount(0).build();
        testPoll = VotePoll.builder()
                .id(1L).shareId("vote123").title("Test Poll").description("desc")
                .user(testUser).voteType(VotePoll.VoteType.SINGLE)
                .frequency(VotePoll.VoteFrequency.ONCE)
                .status(Survey.SurveyStatus.DRAFT)
                .accessLevel(Survey.AccessLevel.PUBLIC).anonymous(true)
                .totalVoteCount(0)
                .options(new ArrayList<>(List.of(testOption, testOption2))).build();
        testOption.setPoll(testPoll);
        testOption2.setPoll(testPoll);
    }

    // --- createPoll ---

    @Test
    void createPoll_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(pollRepository.save(any(VotePoll.class))).thenAnswer(inv -> {
            VotePoll p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        VoteOptionRequest vor = new VoteOptionRequest();
        vor.setTitle("Yes");

        VotePollCreateRequest request = new VotePollCreateRequest();
        request.setTitle("New Poll");
        request.setVoteType("SINGLE");
        request.setFrequency("ONCE");
        request.setAccessLevel("PUBLIC");
        request.setOptions(List.of(vor));

        VotePollDto dto = voteService.createPoll(request);

        assertNotNull(dto);
        assertEquals("New Poll", dto.getTitle());
        assertEquals(1, dto.getOptions().size());
        verify(pollRepository).save(any(VotePoll.class));
    }

    @Test
    void createPoll_notAuthenticated() {
        when(authService.getCurrentUser()).thenReturn(null);

        VotePollCreateRequest request = new VotePollCreateRequest();
        request.setTitle("Test");
        request.setOptions(List.of());

        assertThrows(BusinessException.class, () -> voteService.createPoll(request));
    }

    // --- getPollById ---

    @Test
    void getPollById_success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(testUser);

        VotePollDto dto = voteService.getPollById(1L);

        assertNotNull(dto);
        assertEquals("Test Poll", dto.getTitle());
    }

    @Test
    void getPollById_notFound() {
        when(pollRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> voteService.getPollById(99L));
    }

    @Test
    void getPollById_accessDenied() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> voteService.getPollById(1L));
    }

    // --- publishPoll ---

    @Test
    void publishPoll_success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(pollRepository.save(any(VotePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        VotePollDto dto = voteService.publishPoll(1L);

        assertEquals("PUBLISHED", dto.getStatus());
    }

    @Test
    void publishPoll_accessDenied() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> voteService.publishPoll(1L));
    }

    // --- closePoll ---

    @Test
    void closePoll_success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(pollRepository.save(any(VotePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        VotePollDto dto = voteService.closePoll(1L);

        assertEquals("CLOSED", dto.getStatus());
    }

    // --- deletePoll ---

    @Test
    void deletePoll_success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(testUser);

        voteService.deletePoll(1L);

        verify(pollRepository).delete(testPoll);
    }

    @Test
    void deletePoll_accessDenied() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> voteService.deletePoll(1L));
    }

    @Test
    void deletePoll_notFound() {
        when(pollRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> voteService.deletePoll(99L));
    }

    // --- getMyPolls ---

    @Test
    void getMyPolls_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        when(pollRepository.findByUser(testUser, pageable)).thenReturn(new PageImpl<>(List.of(testPoll)));

        Page<VotePollListDto> page = voteService.getMyPolls(pageable);

        assertEquals(1, page.getTotalElements());
    }

    // --- getPublicPolls ---

    @Test
    void getPublicPolls_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pollRepository.findByStatusAndAccessLevel(Survey.SurveyStatus.PUBLISHED, Survey.AccessLevel.PUBLIC, pageable))
                .thenReturn(new PageImpl<>(List.of(testPoll)));

        Page<VotePollListDto> page = voteService.getPublicPolls(null, pageable);

        assertEquals(1, page.getTotalElements());
    }

    // --- submitVote ---

    @Test
    void submitVote_notPublished() {
        testPoll.setStatus(Survey.SurveyStatus.DRAFT);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setOptionIds(List.of(1L));

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_expired() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setEndTime(Instant.now().minusSeconds(3600));
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setOptionIds(List.of(1L));

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_rateLimited() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited("vote:127.0.0.1")).thenReturn(true);

        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setOptionIds(List.of(1L));

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_alreadyVoted() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted("1", "user:1")).thenReturn(true);

        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setOptionIds(List.of(1L));

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_singleChoiceMultipleOptions() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SINGLE);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);

        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setOptionIds(List.of(1L, 2L));

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    // --- updatePoll ---

    @Test
    void updatePoll_success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(pollRepository.save(any(VotePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        VoteOptionRequest vor = new VoteOptionRequest();
        vor.setTitle("Updated Option");

        VotePollCreateRequest request = new VotePollCreateRequest();
        request.setTitle("Updated Poll");
        request.setVoteType("SINGLE");
        request.setFrequency("ONCE");
        request.setAccessLevel("PUBLIC");
        request.setOptions(List.of(vor));

        VotePollDto dto = voteService.updatePoll(1L, request);

        assertEquals("Updated Poll", dto.getTitle());
    }

    @Test
    void updatePoll_accessDenied() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(testPoll));
        when(authService.getCurrentUser()).thenReturn(otherUser);

        VotePollCreateRequest request = new VotePollCreateRequest();
        request.setTitle("Hacked");
        request.setOptions(List.of());

        assertThrows(BusinessException.class, () -> voteService.updatePoll(1L, request));
    }

    // --- SCORED vote type ---

    @Test
    void submitVote_scored_success() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SCORED);
        testPoll.setMaxVotesPerOption(5);
        testPoll.setMaxTotalVotes(10);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);
        when(recordRepository.countByPollIdAndUserId(1L, 1L)).thenReturn(0L);
        when(optionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(optionRepository.findById(2L)).thenReturn(Optional.of(testOption2));
        when(optionRepository.save(any(VoteOption.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.save(any(VoteRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pollRepository.save(any(VotePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        VoteSubmitRequest request = new VoteSubmitRequest();
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(1L, 3);
        votes.put(2L, 2);
        request.setVotes(votes);

        VotePollDto dto = voteService.submitVote("vote123", request, httpRequest);

        assertNotNull(dto);
        assertEquals(3, testOption.getVoteCount());
        assertEquals(2, testOption2.getVoteCount());
        assertEquals(5, testPoll.getTotalVoteCount());
    }

    @Test
    void submitVote_scored_exceedsMaxVotesPerOption() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SCORED);
        testPoll.setMaxVotesPerOption(3);
        testPoll.setMaxTotalVotes(10);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);

        VoteSubmitRequest request = new VoteSubmitRequest();
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(1L, 5); // exceeds maxVotesPerOption=3
        request.setVotes(votes);

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_scored_exceedsMaxTotalVotes() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SCORED);
        testPoll.setMaxVotesPerOption(10);
        testPoll.setMaxTotalVotes(5);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);
        when(recordRepository.countByPollIdAndUserId(1L, 1L)).thenReturn(0L);

        VoteSubmitRequest request = new VoteSubmitRequest();
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(1L, 3);
        votes.put(2L, 3); // total 6, exceeds maxTotalVotes=5
        request.setVotes(votes);

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_scored_emptyVotes() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SCORED);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);

        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setVotes(new HashMap<>());

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_scored_nullVotes() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SCORED);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);

        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setVotes(null);

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void submitVote_singleChoice_noOptionsSelected() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SINGLE);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);

        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setOptionIds(List.of());

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }

    @Test
    void createPoll_withMaxVotesPerOption() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(pollRepository.save(any(VotePoll.class))).thenAnswer(inv -> {
            VotePoll p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        VoteOptionRequest vor = new VoteOptionRequest();
        vor.setTitle("Option A");

        VotePollCreateRequest request = new VotePollCreateRequest();
        request.setTitle("Scored Poll");
        request.setVoteType("SCORED");
        request.setFrequency("ONCE");
        request.setAccessLevel("PUBLIC");
        request.setMaxVotesPerOption(5);
        request.setMaxTotalVotes(10);
        request.setOptions(List.of(vor));

        VotePollDto dto = voteService.createPoll(request);

        assertNotNull(dto);
        assertEquals("SCORED", dto.getVoteType());
        assertEquals(5, dto.getMaxVotesPerOption());
        assertEquals(10, dto.getMaxTotalVotes());
    }

    @Test
    void submitVote_scored_negativeVoteCount() {
        testPoll.setStatus(Survey.SurveyStatus.PUBLISHED);
        testPoll.setVoteType(VotePoll.VoteType.SCORED);
        testPoll.setMaxVotesPerOption(5);
        when(pollRepository.findByShareId("vote123")).thenReturn(Optional.of(testPoll));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isRateLimited(anyString())).thenReturn(false);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(rateLimitService.hasVoted(anyString(), anyString())).thenReturn(false);
        when(recordRepository.existsByPollIdAndUserId(1L, 1L)).thenReturn(false);

        VoteSubmitRequest request = new VoteSubmitRequest();
        Map<Long, Integer> votes = new HashMap<>();
        votes.put(1L, -1);
        request.setVotes(votes);

        assertThrows(BusinessException.class, () -> voteService.submitVote("vote123", request, httpRequest));
    }
}
