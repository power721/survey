package cn.har01d.survey.repository;

import cn.har01d.survey.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySurveyIdOrderBySortOrderAsc(Long surveyId);
}
