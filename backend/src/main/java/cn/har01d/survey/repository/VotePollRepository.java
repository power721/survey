package cn.har01d.survey.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.Survey;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.entity.VotePoll;

public interface VotePollRepository extends JpaRepository<VotePoll, Long> {
    Optional<VotePoll> findByShareId(String shareId);

    Page<VotePoll> findByUser(User user, Pageable pageable);

    Page<VotePoll> findByStatusAndAccessLevel(Survey.SurveyStatus status, Survey.AccessLevel accessLevel, Pageable pageable);

    Page<VotePoll> findByStatusAndAccessLevelAndUserUsername(Survey.SurveyStatus status, Survey.AccessLevel accessLevel, String username, Pageable pageable);

    List<VotePoll> findByStatusAndEndTimeBefore(Survey.SurveyStatus status, Instant time);
}
