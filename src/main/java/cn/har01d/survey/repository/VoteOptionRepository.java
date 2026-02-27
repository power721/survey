package cn.har01d.survey.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.VoteOption;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    List<VoteOption> findByPollIdOrderBySortOrderAsc(Long pollId);
}
