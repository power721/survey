package cn.har01d.survey.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySurveyIdOrderBySortOrderAsc(Long surveyId);
}
