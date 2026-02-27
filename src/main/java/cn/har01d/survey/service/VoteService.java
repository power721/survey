package cn.har01d.survey.service;

import cn.har01d.survey.dto.vote.*;
import cn.har01d.survey.entity.*;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VoteService {

    private final VotePollRepository pollRepository;
    private final VoteOptionRepository optionRepository;
    private final VoteRecordRepository recordRepository;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final SimpMessagingTemplate messagingTemplate;

    public VoteService(VotePollRepository pollRepository, VoteOptionRepository optionRepository,
                       VoteRecordRepository recordRepository, AuthService authService,
                       RateLimitService rateLimitService, SimpMessagingTemplate messagingTemplate) {
        this.pollRepository = pollRepository;
        this.optionRepository = optionRepository;
        this.recordRepository = recordRepository;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public VotePollDto createPoll(VotePollCreateRequest request) {
        User user = authService.getCurrentUser();
        if (user == null) {
            throw new BusinessException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        VotePoll poll = VotePoll.builder()
                .shareId(generateShareId())
                .title(request.getTitle())
                .description(request.getDescription())
                .user(user)
                .voteType(VotePoll.VoteType.valueOf(request.getVoteType()))
                .frequency(VotePoll.VoteFrequency.valueOf(request.getFrequency()))
                .status(Survey.SurveyStatus.DRAFT)
                .accessLevel(Survey.AccessLevel.valueOf(request.getAccessLevel()))
                .anonymous(request.isAnonymous())
                .maxTotalVotes(request.getMaxTotalVotes())
                .endTime(request.getEndTime())
                .options(new ArrayList<>())
                .build();

        for (int i = 0; i < request.getOptions().size(); i++) {
            VoteOptionRequest or = request.getOptions().get(i);
            VoteOption option = VoteOption.builder()
                    .poll(poll)
                    .content(or.getContent())
                    .imageUrl(or.getImageUrl())
                    .maxVotes(or.getMaxVotes())
                    .sortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : i)
                    .build();
            poll.getOptions().add(option);
        }

        poll = pollRepository.save(poll);
        return toDto(poll, null, null);
    }

    @Transactional
    public VotePollDto updatePoll(Long id, VotePollCreateRequest request) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        poll.setTitle(request.getTitle());
        poll.setDescription(request.getDescription());
        poll.setVoteType(VotePoll.VoteType.valueOf(request.getVoteType()));
        poll.setFrequency(VotePoll.VoteFrequency.valueOf(request.getFrequency()));
        poll.setAccessLevel(Survey.AccessLevel.valueOf(request.getAccessLevel()));
        poll.setAnonymous(request.isAnonymous());
        poll.setMaxTotalVotes(request.getMaxTotalVotes());
        poll.setEndTime(request.getEndTime());

        poll.getOptions().clear();
        for (int i = 0; i < request.getOptions().size(); i++) {
            VoteOptionRequest or = request.getOptions().get(i);
            VoteOption option = VoteOption.builder()
                    .poll(poll)
                    .content(or.getContent())
                    .imageUrl(or.getImageUrl())
                    .maxVotes(or.getMaxVotes())
                    .sortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : i)
                    .build();
            poll.getOptions().add(option);
        }

        poll = pollRepository.save(poll);
        return toDto(poll, null, null);
    }

    @Transactional(readOnly = true)
    public VotePollDto getPollById(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
        return toDto(poll, null, null);
    }

    @Transactional(readOnly = true)
    public VotePollDto getPollByShareId(String shareId, HttpServletRequest request) {
        VotePoll poll = pollRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));

        if (poll.getStatus() != Survey.SurveyStatus.PUBLISHED) {
            throw new BusinessException("Vote poll is not published");
        }
        if (poll.getAccessLevel() == Survey.AccessLevel.PRIVATE) {
            User user = authService.getCurrentUser();
            if (user == null || !poll.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        }

        String ip = getClientIp(request);
        User user = authService.getCurrentUser();
        boolean hasVoted = checkHasVoted(poll, user, ip, null);

        return toDto(poll, hasVoted, null);
    }

    @Transactional(readOnly = true)
    public Page<VotePollDto> getMyPolls(Pageable pageable) {
        User user = authService.getCurrentUser();
        return pollRepository.findByUser(user, pageable).map(p -> toDto(p, null, null));
    }

    @Transactional(readOnly = true)
    public Page<VotePollDto> getPublicPolls(Pageable pageable) {
        return pollRepository.findByStatusAndAccessLevel(Survey.SurveyStatus.PUBLISHED, Survey.AccessLevel.PUBLIC, pageable)
                .map(p -> toDto(p, null, null));
    }

    @Transactional
    public VotePollDto publishPoll(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
        poll.setStatus(Survey.SurveyStatus.PUBLISHED);
        return toDto(pollRepository.save(poll), null, null);
    }

    @Transactional
    public VotePollDto closePoll(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
        poll.setStatus(Survey.SurveyStatus.CLOSED);
        return toDto(pollRepository.save(poll), null, null);
    }

    @Transactional
    public void deletePoll(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
        pollRepository.delete(poll);
    }

    @Transactional
    public VotePollDto submitVote(String shareId, VoteSubmitRequest request, HttpServletRequest httpRequest) {
        VotePoll poll = pollRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote poll not found"));

        if (poll.getStatus() != Survey.SurveyStatus.PUBLISHED) {
            throw new BusinessException("Voting is not open");
        }
        if (poll.getEndTime() != null && poll.getEndTime().isBefore(Instant.now())) {
            throw new BusinessException("Voting is closed");
        }

        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        User user = authService.getCurrentUser();
        String deviceId = request.getDeviceId();

        // Rate limiting
        String rateLimitKey = "vote:" + ip;
        if (rateLimitService.isRateLimited(rateLimitKey)) {
            throw new BusinessException("Too many requests, please try again later", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Check duplicate voting
        if (checkHasVoted(poll, user, ip, deviceId)) {
            throw new BusinessException("You have already voted");
        }

        // Check total votes
        if (poll.getMaxTotalVotes() != null && poll.getTotalVoteCount() >= poll.getMaxTotalVotes()) {
            throw new BusinessException("Maximum total votes reached");
        }

        // Validate vote type
        if (poll.getVoteType() == VotePoll.VoteType.SINGLE && request.getOptionIds().size() != 1) {
            throw new BusinessException("Single choice vote allows only one option");
        }

        for (Long optionId : request.getOptionIds()) {
            VoteOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vote option not found"));

            if (!option.getPoll().getId().equals(poll.getId())) {
                throw new BusinessException("Option does not belong to this poll");
            }

            if (option.getMaxVotes() != null && option.getVoteCount() >= option.getMaxVotes()) {
                throw new BusinessException("Maximum votes for option '" + option.getContent() + "' reached");
            }

            option.setVoteCount(option.getVoteCount() + 1);
            optionRepository.save(option);

            VoteRecord record = VoteRecord.builder()
                    .poll(poll)
                    .option(option)
                    .user(user)
                    .ip(ip)
                    .userAgent(ua)
                    .deviceId(deviceId)
                    .build();
            recordRepository.save(record);
        }

        poll.setTotalVoteCount(poll.getTotalVoteCount() + 1);
        poll = pollRepository.save(poll);

        // Mark as voted in Redis
        String identifier = user != null ? "user:" + user.getId() : "ip:" + ip;
        if (poll.getFrequency() == VotePoll.VoteFrequency.DAILY) {
            rateLimitService.markVotedDaily(String.valueOf(poll.getId()), identifier);
        } else {
            rateLimitService.markVoted(String.valueOf(poll.getId()), identifier);
        }

        VotePollDto result = toDto(poll, true, null);

        // WebSocket push real-time results
        messagingTemplate.convertAndSend("/topic/vote/" + poll.getShareId(), result);

        return result;
    }

    private boolean checkHasVoted(VotePoll poll, User user, String ip, String deviceId) {
        String identifier = user != null ? "user:" + user.getId() : "ip:" + ip;

        if (poll.getFrequency() == VotePoll.VoteFrequency.DAILY) {
            return rateLimitService.hasVotedDaily(String.valueOf(poll.getId()), identifier);
        } else {
            if (rateLimitService.hasVoted(String.valueOf(poll.getId()), identifier)) {
                return true;
            }
            // Fallback to database check
            if (user != null) {
                return recordRepository.existsByPollIdAndUserId(poll.getId(), user.getId());
            }
            if (deviceId != null && !deviceId.isBlank()) {
                return recordRepository.existsByPollIdAndDeviceId(poll.getId(), deviceId);
            }
            return recordRepository.existsByPollIdAndIp(poll.getId(), ip);
        }
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

    private VotePollDto toDto(VotePoll poll, Boolean hasVoted, User currentUser) {
        VotePollDto dto = new VotePollDto();
        dto.setId(poll.getId());
        dto.setShareId(poll.getShareId());
        dto.setTitle(poll.getTitle());
        dto.setDescription(poll.getDescription());
        dto.setVoteType(poll.getVoteType().name());
        dto.setFrequency(poll.getFrequency().name());
        dto.setStatus(poll.getStatus().name());
        dto.setAccessLevel(poll.getAccessLevel().name());
        dto.setAnonymous(poll.isAnonymous());
        dto.setMaxTotalVotes(poll.getMaxTotalVotes());
        dto.setEndTime(poll.getEndTime());
        dto.setTotalVoteCount(poll.getTotalVoteCount());
        dto.setCreatorName(poll.getUser().getNickname());
        dto.setHasVoted(hasVoted != null && hasVoted);
        dto.setCreatedAt(poll.getCreatedAt());
        dto.setUpdatedAt(poll.getUpdatedAt());

        int totalVotes = poll.getOptions().stream().mapToInt(VoteOption::getVoteCount).sum();
        List<VoteOptionDto> optionDtos = poll.getOptions().stream().map(opt -> {
            VoteOptionDto od = new VoteOptionDto();
            od.setId(opt.getId());
            od.setContent(opt.getContent());
            od.setImageUrl(opt.getImageUrl());
            od.setMaxVotes(opt.getMaxVotes());
            od.setVoteCount(opt.getVoteCount());
            od.setPercentage(totalVotes > 0 ? (double) opt.getVoteCount() / totalVotes * 100 : 0);
            od.setSortOrder(opt.getSortOrder());
            return od;
        }).toList();
        dto.setOptions(optionDtos);

        return dto;
    }
}
