package cn.har01d.survey.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cn.har01d.survey.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT CAST(u.createdAt AS date) as date, COUNT(u) as count " +
            "FROM User u WHERE u.createdAt >= :startTime GROUP BY CAST(u.createdAt AS date)")
    List<Object[]> findCreatedAtGroupByDate(@Param("startTime") Instant startTime);
}
