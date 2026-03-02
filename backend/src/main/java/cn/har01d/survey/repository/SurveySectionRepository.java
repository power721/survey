package cn.har01d.survey.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.SurveySection;

public interface SurveySectionRepository extends JpaRepository<SurveySection, Long> {
}
