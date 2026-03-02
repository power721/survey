package cn.har01d.survey.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cn.har01d.survey.entity.SurveyResponse;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    Page<SurveyResponse> findBySurveyId(Long surveyId, Pageable pageable);

    List<SurveyResponse> findBySurveyId(Long surveyId);

    long countBySurveyId(Long surveyId);

    void deleteBySurveyId(Long surveyId);

    @EntityGraph(attributePaths = {"answers", "answers.question", "answers.selectedOption"})
    Optional<SurveyResponse> findFirstBySurveyIdAndUserIdOrderByCreatedAtDesc(Long surveyId, Long userId);

    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);

    boolean existsBySurveyIdAndIp(Long surveyId, String ip);

    @Query("SELECT CAST(r.createdAt AS date) as date, COUNT(r) as count " +
            "FROM SurveyResponse r WHERE r.createdAt >= :startTime GROUP BY CAST(r.createdAt AS date)")
    List<Object[]> findCreatedAtGroupByDate(@Param("startTime") Instant startTime);
}
