package cn.har01d.survey.repository;

import cn.har01d.survey.entity.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    List<VoteOption> findByPollIdOrderBySortOrderAsc(Long pollId);
}
