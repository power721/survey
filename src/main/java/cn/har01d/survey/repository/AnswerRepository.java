package cn.har01d.survey.repository;

import cn.har01d.survey.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByResponseId(Long responseId);

    @Modifying
    @Query("DELETE FROM Answer a WHERE a.response.survey.id = :surveyId")
    void deleteBySurveyId(@Param("surveyId") Long surveyId);

    @Modifying
    @Query("DELETE FROM Answer a WHERE a.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT a.selectedOption.id, COUNT(a) FROM Answer a WHERE a.question.id = :questionId AND a.selectedOption IS NOT NULL GROUP BY a.selectedOption.id")
    List<Object[]> countByQuestionGroupByOption(@Param("questionId") Long questionId);

    @Query("SELECT a.textValue FROM Answer a WHERE a.question.id = :questionId AND a.textValue IS NOT NULL")
    List<String> findTextValuesByQuestionId(@Param("questionId") Long questionId);
}
