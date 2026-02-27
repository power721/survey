package cn.har01d.survey.repository;

import cn.har01d.survey.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    boolean existsByPollIdAndUserId(Long pollId, Long userId);
    boolean existsByPollIdAndIp(Long pollId, String ip);
    boolean existsByPollIdAndDeviceId(Long pollId, String deviceId);
    boolean existsByPollIdAndUserIdAndCreatedAtAfter(Long pollId, Long userId, Instant after);
    boolean existsByPollIdAndIpAndCreatedAtAfter(Long pollId, String ip, Instant after);
    List<VoteRecord> findByPollId(Long pollId);
    long countByPollId(Long pollId);
}
