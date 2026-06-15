package com.admin.equipment.repo;

import com.admin.equipment.model.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByAdoptedCaseIdIsNotNullAndCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COUNT(r) FROM RecommendationLog r WHERE r.hitCount > 0 AND r.createdAt >= :dateTime")
    long countWithHitsAfter(LocalDateTime dateTime);
}
