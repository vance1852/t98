package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_logs")
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "equipment_type", length = 32)
    private String equipmentType = "";

    @Column(name = "query_text", columnDefinition = "TEXT")
    private String queryText = "";

    @Column(name = "recommended_case_ids", columnDefinition = "TEXT")
    private String recommendedCaseIds = "";

    @Column(name = "adopted_case_id")
    private Long adoptedCaseId;

    @Column(name = "hit_count")
    private Integer hitCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public String getRecommendedCaseIds() { return recommendedCaseIds; }
    public void setRecommendedCaseIds(String recommendedCaseIds) { this.recommendedCaseIds = recommendedCaseIds; }
    public Long getAdoptedCaseId() { return adoptedCaseId; }
    public void setAdoptedCaseId(Long adoptedCaseId) { this.adoptedCaseId = adoptedCaseId; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
