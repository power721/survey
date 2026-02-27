package cn.har01d.survey.repository;

import cn.har01d.survey.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
}
