package cn.har01d.survey.repository;

import cn.har01d.survey.entity.SurveyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    Page<SurveyResponse> findBySurveyId(Long surveyId, Pageable pageable);
    List<SurveyResponse> findBySurveyId(Long surveyId);
    long countBySurveyId(Long surveyId);
    void deleteBySurveyId(Long surveyId);
}
