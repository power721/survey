package cn.har01d.survey.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cn.har01d.survey.entity.VoteRecord;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    boolean existsByPollIdAndUserId(Long pollId, Long userId);

    boolean existsByPollIdAndIp(Long pollId, String ip);

    boolean existsByPollIdAndDeviceId(Long pollId, String deviceId);

    boolean existsByPollIdAndUserIdAndCreatedAtAfter(Long pollId, Long userId, Instant after);

    boolean existsByPollIdAndIpAndCreatedAtAfter(Long pollId, String ip, Instant after);

    List<VoteRecord> findByPollId(Long pollId);

    List<VoteRecord> findByOptionId(Long optionId);

    long countByPollId(Long pollId);

    long countByPollIdAndUserId(Long pollId, Long userId);

    long countByPollIdAndIp(Long pollId, String ip);

    long countByPollIdAndDeviceId(Long pollId, String deviceId);

    long countByOptionIdAndUserId(Long optionId, Long userId);

    long countByOptionIdAndIp(Long optionId, String ip);

    long countByOptionIdAndDeviceId(Long optionId, String deviceId);

    void deleteByOptionId(Long optionId);

    void deleteByPollId(Long pollId);
}
