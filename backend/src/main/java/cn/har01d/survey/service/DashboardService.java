package cn.har01d.survey.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cn.har01d.survey.dto.DashboardStatsDto;
import cn.har01d.survey.repository.SurveyRepository;
import cn.har01d.survey.repository.SurveyResponseRepository;
import cn.har01d.survey.repository.UserRepository;
import cn.har01d.survey.repository.VotePollRepository;
import cn.har01d.survey.repository.VoteRecordRepository;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;
    private final VotePollRepository votePollRepository;
    private final VoteRecordRepository voteRecordRepository;

    public DashboardService(UserRepository userRepository,
                            SurveyRepository surveyRepository,
                            SurveyResponseRepository responseRepository,
                            VotePollRepository votePollRepository,
                            VoteRecordRepository voteRecordRepository) {
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.votePollRepository = votePollRepository;
        this.voteRecordRepository = voteRecordRepository;
    }

    public DashboardStatsDto getStats() {
        long totalUsers = userRepository.count();
        long totalSurveys = surveyRepository.count();
        long totalResponses = responseRepository.count();
        long totalVotes = votePollRepository.count();
        long totalVoteRecords = voteRecordRepository.count();

        List<DashboardStatsDto.TrendDataPoint> userTrend = getUserTrend(30);
        List<DashboardStatsDto.TrendDataPoint> surveyTrend = getSurveyTrend(30);
        List<DashboardStatsDto.TrendDataPoint> responseTrend = getResponseTrend(30);

        return DashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalSurveys(totalSurveys)
                .totalResponses(totalResponses)
                .totalVotes(totalVotes)
                .totalVoteRecords(totalVoteRecords)
                .userTrend(userTrend)
                .surveyTrend(surveyTrend)
                .responseTrend(responseTrend)
                .build();
    }

    private List<DashboardStatsDto.TrendDataPoint> getUserTrend(int days) {
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> results = userRepository.findCreatedAtGroupByDate(startTime);
        return buildTrendData(results, days);
    }

    private List<DashboardStatsDto.TrendDataPoint> getSurveyTrend(int days) {
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> results = surveyRepository.findCreatedAtGroupByDate(startTime);
        return buildTrendData(results, days);
    }

    private List<DashboardStatsDto.TrendDataPoint> getResponseTrend(int days) {
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> results = responseRepository.findCreatedAtGroupByDate(startTime);
        return buildTrendData(results, days);
    }

    private List<DashboardStatsDto.TrendDataPoint> buildTrendData(List<Object[]> results, int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> dataMap = results.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> ((Number) row[1]).longValue()
                ));

        List<DashboardStatsDto.TrendDataPoint> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(formatter);
            long count = dataMap.getOrDefault(dateStr, 0L);
            trendData.add(DashboardStatsDto.TrendDataPoint.builder()
                    .date(dateStr)
                    .count(count)
                    .build());
        }
        return trendData;
    }
}
