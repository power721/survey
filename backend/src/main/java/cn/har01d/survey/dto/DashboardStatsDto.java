package cn.har01d.survey.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalUsers;
    private long totalSurveys;
    private long totalResponses;
    private long totalVotes;
    private long totalVoteRecords;
    private List<TrendDataPoint> userTrend;
    private List<TrendDataPoint> surveyTrend;
    private List<TrendDataPoint> responseTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String date;
        private long count;
    }
}
