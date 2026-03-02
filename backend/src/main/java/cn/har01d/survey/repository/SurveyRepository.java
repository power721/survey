package cn.har01d.survey.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cn.har01d.survey.entity.Survey;
import cn.har01d.survey.entity.User;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    Optional<Survey> findByShareId(String shareId);

    Page<Survey> findByUser(User user, Pageable pageable);

    Page<Survey> findByStatus(Survey.SurveyStatus status, Pageable pageable);

    Page<Survey> findByStatusAndAccessLevel(Survey.SurveyStatus status, Survey.AccessLevel accessLevel, Pageable pageable);

    Page<Survey> findByStatusAndAccessLevelAndUserUsername(Survey.SurveyStatus status, Survey.AccessLevel accessLevel, String username, Pageable pageable);

    Page<Survey> findByTemplateTrue(Pageable pageable);

    Page<Survey> findByUserAndTitleContaining(User user, String title, Pageable pageable);

    @Query("SELECT CAST(s.createdAt AS date) as date, COUNT(s) as count " +
            "FROM Survey s WHERE s.createdAt >= :startTime GROUP BY CAST(s.createdAt AS date)")
    List<Object[]> findCreatedAtGroupByDate(@Param("startTime") Instant startTime);
}
