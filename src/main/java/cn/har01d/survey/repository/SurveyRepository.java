package cn.har01d.survey.repository;

import cn.har01d.survey.entity.Survey;
import cn.har01d.survey.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    Optional<Survey> findByShareId(String shareId);
    Page<Survey> findByUser(User user, Pageable pageable);
    Page<Survey> findByStatus(Survey.SurveyStatus status, Pageable pageable);
    Page<Survey> findByStatusAndAccessLevel(Survey.SurveyStatus status, Survey.AccessLevel accessLevel, Pageable pageable);
    Page<Survey> findByTemplateTrue(Pageable pageable);
    Page<Survey> findByUserAndTitleContaining(User user, String title, Pageable pageable);
}
