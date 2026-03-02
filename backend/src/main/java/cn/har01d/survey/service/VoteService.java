package cn.har01d.survey.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.har01d.survey.dto.vote.VoteOptionDto;
import cn.har01d.survey.dto.vote.VoteOptionRequest;
import cn.har01d.survey.dto.vote.VotePollCreateRequest;
import cn.har01d.survey.dto.vote.VotePollDto;
import cn.har01d.survey.dto.vote.VoteRecordDto;
import cn.har01d.survey.dto.vote.VoteSubmitRequest;
import cn.har01d.survey.dto.vote.VoterDto;
import cn.har01d.survey.entity.Survey;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.entity.VoteOption;
import cn.har01d.survey.entity.VotePoll;
import cn.har01d.survey.entity.VoteRecord;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.exception.ResourceNotFoundException;
import cn.har01d.survey.repository.VoteOptionRepository;
import cn.har01d.survey.repository.VotePollRepository;
import cn.har01d.survey.repository.VoteRecordRepository;
import cn.har01d.survey.util.GravatarUtil;
import cn.har01d.survey.util.HtmlSanitizer;

@Service
public class VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteService.class);

    private final VotePollRepository pollRepository;
    private final VoteOptionRepository optionRepository;
    private final VoteRecordRepository recordRepository;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Duration DEFAULT_DEADLINE = Duration.ofDays(7);

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
            throw new BusinessException("auth.not.authenticated", HttpStatus.UNAUTHORIZED);
        }

        Instant endTime = request.getEndTime() != null ? request.getEndTime() : Instant.now().plus(DEFAULT_DEADLINE);
        if (request.getStartTime() != null && !endTime.isAfter(request.getStartTime())) {
            throw new BusinessException("error.endTimeBeforeStartTime");
        }

        VotePoll poll = VotePoll.builder()
                .shareId(generateShareId())
                .title(request.getTitle())
                .description(HtmlSanitizer.sanitize(request.getDescription()))
                .logoUrl(request.getLogoUrl())
                .backgroundUrl(request.getBackgroundUrl())
                .user(user)
                .voteType(VotePoll.VoteType.valueOf(request.getVoteType()))
                .frequency(VotePoll.VoteFrequency.valueOf(request.getFrequency()))
                .status(Survey.SurveyStatus.DRAFT)
                .accessLevel(Survey.AccessLevel.valueOf(request.getAccessLevel()))
                .anonymous(request.isAnonymous())
                .showVoters(request.isShowVoters())
                .maxTotalVotes(request.getMaxTotalVotes())
                .maxOptions(request.getMaxOptions())
                .maxVotesPerOption(request.getMaxVotesPerOption())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .options(new ArrayList<>())
                .build();

        for (int i = 0; i < request.getOptions().size(); i++) {
            VoteOptionRequest or = request.getOptions().get(i);
            VoteOption option = VoteOption.builder()
                    .poll(poll)
                    .title(or.getTitle())
                    .content(or.getContent())
                    .imageUrl(or.getImageUrl())
                    .sortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : i)
                    .build();
            poll.getOptions().add(option);
        }

        poll = pollRepository.save(poll);
        return toDto(poll, null);
    }

    @Transactional
    public VotePollDto updatePoll(Long id, VotePollCreateRequest request) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("vote.access.denied", HttpStatus.FORBIDDEN);
        }

        poll.setTitle(request.getTitle());
        poll.setDescription(HtmlSanitizer.sanitize(request.getDescription()));
        poll.setLogoUrl(request.getLogoUrl());
        poll.setBackgroundUrl(request.getBackgroundUrl());
        poll.setVoteType(VotePoll.VoteType.valueOf(request.getVoteType()));
        poll.setFrequency(VotePoll.VoteFrequency.valueOf(request.getFrequency()));
        poll.setAccessLevel(Survey.AccessLevel.valueOf(request.getAccessLevel()));
        poll.setAnonymous(request.isAnonymous());
        poll.setShowVoters(request.isShowVoters());
        poll.setMaxTotalVotes(request.getMaxTotalVotes());
        poll.setMaxOptions(request.getMaxOptions());
        poll.setMaxVotesPerOption(request.getMaxVotesPerOption());
        Instant endTime = request.getEndTime() != null ? request.getEndTime() : Instant.now().plus(DEFAULT_DEADLINE);
        if (request.getStartTime() != null && !endTime.isAfter(request.getStartTime())) {
            throw new BusinessException("error.endTimeBeforeStartTime");
        }
        poll.setStartTime(request.getStartTime());
        poll.setEndTime(endTime);

        // Build map of existing options by ID
        Map<Long, VoteOption> existingOptionMap = poll.getOptions().stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(VoteOption::getId, o -> o));

        // Collect IDs of options in the request
        Set<Long> requestOptionIds = new HashSet<>();
        for (VoteOptionRequest or : request.getOptions()) {
            if (or.getId() != null) {
                requestOptionIds.add(or.getId());
            }
        }

        // Delete vote records for removed options to avoid FK constraint violation
        for (VoteOption existingOption : poll.getOptions()) {
            if (existingOption.getId() != null && !requestOptionIds.contains(existingOption.getId())) {
                recordRepository.deleteByOptionId(existingOption.getId());
            }
        }

        // Remove deleted options
        poll.getOptions().removeIf(o -> o.getId() != null && !requestOptionIds.contains(o.getId()));

        // Update existing and add new options
        for (int i = 0; i < request.getOptions().size(); i++) {
            VoteOptionRequest or = request.getOptions().get(i);
            if (or.getId() != null && existingOptionMap.containsKey(or.getId())) {
                // Update existing option (preserve voteCount)
                VoteOption option = existingOptionMap.get(or.getId());
                option.setTitle(or.getTitle());
                option.setContent(or.getContent());
                option.setImageUrl(or.getImageUrl());
                option.setSortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : i);
            } else {
                // New option
                VoteOption option = VoteOption.builder()
                        .poll(poll)
                        .title(or.getTitle())
                        .content(or.getContent())
                        .imageUrl(or.getImageUrl())
                        .sortOrder(or.getSortOrder() > 0 ? or.getSortOrder() : i)
                        .build();
                poll.getOptions().add(option);
            }
        }

        poll = pollRepository.save(poll);
        return toDto(poll, null);
    }

    @Transactional(readOnly = true)
    public VotePollDto getPollById(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("vote.access.denied", HttpStatus.FORBIDDEN);
        }
        return toDto(poll, null);
    }

    @Transactional(readOnly = true)
    public VotePollDto getPollByShareId(String shareId, HttpServletRequest request) {
        VotePoll poll = pollRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));

        if (poll.getStatus() != Survey.SurveyStatus.PUBLISHED) {
            throw new BusinessException("vote.not.published");
        }

        if (poll.getStartTime() != null && poll.getStartTime().isAfter(Instant.now())) {
            VotePollDto dto = new VotePollDto();
            dto.setId(poll.getId());
            dto.setShareId(poll.getShareId());
            dto.setTitle(poll.getTitle());
            dto.setStatus(poll.getStatus().name());
            dto.setStartTime(poll.getStartTime());
            dto.setEndTime(poll.getEndTime());
            dto.setCreatorName(poll.getUser().getNickname());
            dto.setCreatedAt(poll.getCreatedAt());
            dto.setOptions(List.of());
            return dto;
        }

        String ip = getClientIp(request);
        User user = authService.getCurrentUser();
        boolean hasVoted = (poll.isAnonymous() || user != null) && checkHasVoted(poll, user, ip, null);

        return toDto(poll, hasVoted);
    }

    @Transactional(readOnly = true)
    public Page<VotePollDto> getMyPolls(Pageable pageable) {
        User user = authService.getCurrentUser();
        return pollRepository.findByUser(user, pageable).map(p -> toDto(p, null));
    }

    @Transactional(readOnly = true)
    public Page<VoteRecordDto> getVoteRecords(Long pollId, Pageable pageable) {
        VotePoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("vote.access.denied", HttpStatus.FORBIDDEN);
        }
        boolean anonymous = poll.isAnonymous();

        if (poll.getVoteType() == VotePoll.VoteType.SCORED) {
            return getAggregatedVoteRecords(poll, anonymous, pageable);
        }

        return recordRepository.findByPollId(pollId, pageable).map(r -> toRecordDto(r, anonymous));
    }

    private Page<VoteRecordDto> getAggregatedVoteRecords(VotePoll poll, boolean anonymous, Pageable pageable) {
        List<VoteRecord> allRecords = recordRepository.findByPollId(poll.getId());

        // Group by (userId or ip) + optionId
        Map<String, List<VoteRecord>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(r -> {
                    String key = r.getUser() != null ? "u:" + r.getUser().getId() : "ip:" + r.getIp();
                    return key + "|" + r.getOption().getId();
                }));

        List<VoteRecordDto> aggregated = grouped.values().stream()
                .map(records -> {
                    VoteRecord first = records.get(0);
                    VoteRecordDto dto = new VoteRecordDto();
                    dto.setId(first.getId());
                    dto.setOptionTitle(first.getOption().getTitle());
                    dto.setVoteCount(records.size());
                    dto.setCreatedAt(first.getCreatedAt());
                    dto.setIp(first.getIp());
                    if (!anonymous && first.getUser() != null) {
                        dto.setUsername(first.getUser().getUsername());
                        dto.setNickname(first.getUser().getNickname());
                    }
                    return dto;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), aggregated.size());
        List<VoteRecordDto> pageContent = start >= aggregated.size() ? List.of() : aggregated.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, aggregated.size());
    }

    private VoteRecordDto toRecordDto(VoteRecord r, boolean anonymous) {
        VoteRecordDto dto = new VoteRecordDto();
        dto.setId(r.getId());
        dto.setOptionTitle(r.getOption().getTitle());
        dto.setVoteCount(1);
        dto.setCreatedAt(r.getCreatedAt());
        dto.setIp(r.getIp());
        if (!anonymous && r.getUser() != null) {
            dto.setUsername(r.getUser().getUsername());
            dto.setNickname(r.getUser().getNickname());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<VotePollDto> getPublicPolls(Pageable pageable) {
        return pollRepository.findByStatusAndAccessLevel(Survey.SurveyStatus.PUBLISHED, Survey.AccessLevel.PUBLIC, pageable)
                .map(p -> toDto(p, null));
    }

    @Transactional
    public VotePollDto publishPoll(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("vote.access.denied", HttpStatus.FORBIDDEN);
        }
        poll.setStatus(Survey.SurveyStatus.PUBLISHED);
        return toDto(pollRepository.save(poll), null);
    }

    @Transactional
    public VotePollDto closePoll(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("vote.access.denied", HttpStatus.FORBIDDEN);
        }
        poll.setStatus(Survey.SurveyStatus.CLOSED);
        return toDto(pollRepository.save(poll), null);
    }

    @Transactional
    public void deletePoll(Long id) {
        VotePoll poll = pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));
        User user = authService.getCurrentUser();
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new BusinessException("vote.access.denied", HttpStatus.FORBIDDEN);
        }
        recordRepository.deleteByPollId(poll.getId());
        pollRepository.delete(poll);
    }

    @Transactional
    public VotePollDto submitVote(String shareId, VoteSubmitRequest request, HttpServletRequest httpRequest) {
        VotePoll poll = pollRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));

        if (poll.getStatus() != Survey.SurveyStatus.PUBLISHED) {
            throw new BusinessException("vote.not.published");
        }
        if (poll.getStartTime() != null && poll.getStartTime().isAfter(Instant.now())) {
            throw new BusinessException("vote.not.started");
        }
        if (poll.getEndTime() != null && poll.getEndTime().isBefore(Instant.now())) {
            throw new BusinessException("vote.closed");
        }

        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        User user = authService.getCurrentUser();

        if (!poll.isAnonymous() && user == null) {
            throw new BusinessException("auth.not.authenticated", HttpStatus.UNAUTHORIZED);
        }
        String deviceId = request.getDeviceId();

        // Rate limiting
        String rateLimitKey = "vote:" + ip;
        if (rateLimitService.isRateLimited(rateLimitKey)) {
            throw new BusinessException("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Check duplicate voting
        if (checkHasVoted(poll, user, ip, deviceId)) {
            throw new BusinessException("vote.already.voted");
        }

        // Build per-option vote counts
        Map<Long, Integer> voteMap;
        if (poll.getVoteType() == VotePoll.VoteType.SCORED) {
            voteMap = request.getVotes();
            if (voteMap == null || voteMap.isEmpty()) {
                throw new BusinessException("No votes provided");
            }
        } else {
            if (request.getOptionIds() == null || request.getOptionIds().isEmpty()) {
                throw new BusinessException("No options selected");
            }
            voteMap = new HashMap<>();
            for (Long optionId : request.getOptionIds()) {
                voteMap.put(optionId, 1);
            }
        }

        int totalVotesInRequest = voteMap.values().stream().mapToInt(Integer::intValue).sum();

        // Validate vote type constraints
        if (poll.getVoteType() == VotePoll.VoteType.SINGLE && voteMap.size() != 1) {
            throw new BusinessException("Single choice vote allows only one option");
        }
        if (poll.getVoteType() == VotePoll.VoteType.MULTIPLE && poll.getMaxOptions() != null
                && voteMap.size() > poll.getMaxOptions()) {
            throw new BusinessException("You can select at most " + poll.getMaxOptions() + " options");
        }

        // Validate maxVotesPerOption
        if (poll.getMaxVotesPerOption() != null) {
            for (Map.Entry<Long, Integer> entry : voteMap.entrySet()) {
                if (entry.getValue() > poll.getMaxVotesPerOption()) {
                    throw new BusinessException("Maximum " + poll.getMaxVotesPerOption() + " votes per option");
                }
                if (entry.getValue() < 0) {
                    throw new BusinessException("Vote count cannot be negative");
                }
            }
        }

        // Check per-user total votes
        if (poll.getMaxTotalVotes() != null) {
            long userTotalVotes;
            if (user != null) {
                userTotalVotes = recordRepository.countByPollIdAndUserId(poll.getId(), user.getId());
            } else if (deviceId != null && !deviceId.isBlank()) {
                userTotalVotes = recordRepository.countByPollIdAndDeviceId(poll.getId(), deviceId);
            } else {
                userTotalVotes = recordRepository.countByPollIdAndIp(poll.getId(), ip);
            }
            if (userTotalVotes + totalVotesInRequest > poll.getMaxTotalVotes()) {
                throw new BusinessException("vote.max.reached");
            }
        }

        // Process votes
        for (Map.Entry<Long, Integer> entry : voteMap.entrySet()) {
            Long optionId = entry.getKey();
            int count = entry.getValue();
            if (count <= 0) continue;

            VoteOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new ResourceNotFoundException("vote.not.found"));

            if (!option.getPoll().getId().equals(poll.getId())) {
                throw new BusinessException("Option does not belong to this poll");
            }

            option.setVoteCount(option.getVoteCount() + count);
            optionRepository.save(option);

            for (int i = 0; i < count; i++) {
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
        }

        poll.setTotalVoteCount(poll.getTotalVoteCount() + totalVotesInRequest);
        poll = pollRepository.save(poll);

        // Mark as voted in Redis
        String identifier = user != null ? "user:" + user.getId() : "ip:" + ip;
        if (poll.getFrequency() == VotePoll.VoteFrequency.DAILY) {
            rateLimitService.markVotedDaily(String.valueOf(poll.getId()), identifier);
        } else {
            rateLimitService.markVoted(String.valueOf(poll.getId()), identifier, poll.getEndTime());
        }

        VotePollDto result = toDto(poll, true);

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

    private VotePollDto toDto(VotePoll poll, Boolean hasVoted) {
        VotePollDto dto = new VotePollDto();
        dto.setId(poll.getId());
        dto.setShareId(poll.getShareId());
        dto.setTitle(poll.getTitle());
        dto.setDescription(poll.getDescription());
        dto.setLogoUrl(poll.getLogoUrl());
        dto.setBackgroundUrl(poll.getBackgroundUrl());
        dto.setVoteType(poll.getVoteType().name());
        dto.setFrequency(poll.getFrequency().name());
        dto.setStatus(poll.getStatus().name());
        dto.setAccessLevel(poll.getAccessLevel().name());
        dto.setAnonymous(poll.isAnonymous());
        dto.setShowVoters(poll.isShowVoters());
        dto.setMaxTotalVotes(poll.getMaxTotalVotes());
        dto.setMaxOptions(poll.getMaxOptions());
        dto.setMaxVotesPerOption(poll.getMaxVotesPerOption());
        dto.setStartTime(poll.getStartTime());
        dto.setEndTime(poll.getEndTime());
        dto.setTotalVoteCount(poll.getTotalVoteCount());
        dto.setCreatorName(poll.getUser().getNickname());
        dto.setHasVoted(hasVoted != null && hasVoted);
        dto.setCreatedAt(poll.getCreatedAt());
        dto.setUpdatedAt(poll.getUpdatedAt());

        boolean showVoters = poll.isShowVoters() && !poll.isAnonymous() && poll.getVoteType() != VotePoll.VoteType.SCORED;
        Map<Long, List<VoterDto>> votersByOption = new HashMap<>();
        if (showVoters) {
            List<VoteRecord> records = recordRepository.findByPollId(poll.getId());
            for (VoteRecord record : records) {
                if (record.getUser() != null) {
                    User voter = record.getUser();
                    String avatar = voter.getAvatar() != null && !voter.getAvatar().isBlank()
                            ? voter.getAvatar()
                            : GravatarUtil.getAvatarUrl(voter.getEmail());
                    votersByOption
                            .computeIfAbsent(record.getOption().getId(), k -> new ArrayList<>())
                            .add(new VoterDto(voter.getNickname(), avatar));
                }
            }
        }

        int totalVotes = poll.getOptions().stream().mapToInt(VoteOption::getVoteCount).sum();
        List<VoteOptionDto> optionDtos = poll.getOptions().stream().map(opt -> {
            VoteOptionDto od = new VoteOptionDto();
            od.setId(opt.getId());
            od.setTitle(opt.getTitle());
            od.setContent(opt.getContent());
            od.setImageUrl(opt.getImageUrl());
            od.setVoteCount(opt.getVoteCount());
            od.setPercentage(totalVotes > 0 ? (double) opt.getVoteCount() / totalVotes * 100 : 0);
            od.setSortOrder(opt.getSortOrder());
            if (showVoters) {
                od.setVoters(votersByOption.getOrDefault(opt.getId(), List.of()));
            }
            return od;
        }).toList();
        dto.setOptions(optionDtos);

        return dto;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void closeExpiredPolls() {
        List<VotePoll> expiredPolls = pollRepository.findByStatusAndEndTimeBefore(
                Survey.SurveyStatus.PUBLISHED, Instant.now());
        for (VotePoll poll : expiredPolls) {
            poll.setStatus(Survey.SurveyStatus.CLOSED);
            pollRepository.save(poll);
            log.info("Auto-closed expired poll: id={}, title={}", poll.getId(), poll.getTitle());
        }
        if (!expiredPolls.isEmpty()) {
            log.info("Auto-closed {} expired poll(s)", expiredPolls.size());
        }
    }
}
