package cn.har01d.survey.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
