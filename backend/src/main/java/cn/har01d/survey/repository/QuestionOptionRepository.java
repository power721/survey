package cn.har01d.survey.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.QuestionOption;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
}
