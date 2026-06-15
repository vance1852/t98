package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_cases")
public class KnowledgeCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "equipment_type", length = 32)
    private String equipmentType = "";

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "fault_symptom", columnDefinition = "TEXT")
    private String faultSymptom = "";

    @Column(name = "cause_analysis", columnDefinition = "TEXT")
    private String causeAnalysis = "";

    @Column(name = "solution_steps", columnDefinition = "TEXT")
    private String solutionSteps = "";

    @Column(name = "spare_parts", columnDefinition = "TEXT")
    private String spareParts = "";

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes = 0;

    @Column(name = "tags", length = 256)
    private String tags = "";

    @Column(name = "keywords", length = 256)
    private String keywords = "";

    @Column(name = "source_work_order_id")
    private Long sourceWorkOrderId;

    @Column(name = "author_username", length = 64)
    private String authorUsername = "";

    @Column(length = 16)
    private String status = "draft";

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "adoption_count")
    private Integer adoptionCount = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "weight_score")
    private Double weightScore = 1.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFaultSymptom() { return faultSymptom; }
    public void setFaultSymptom(String faultSymptom) { this.faultSymptom = faultSymptom; }
    public String getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(String causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public String getSolutionSteps() { return solutionSteps; }
    public void setSolutionSteps(String solutionSteps) { this.solutionSteps = solutionSteps; }
    public String getSpareParts() { return spareParts; }
    public void setSpareParts(String spareParts) { this.spareParts = spareParts; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Long getSourceWorkOrderId() { return sourceWorkOrderId; }
    public void setSourceWorkOrderId(Long sourceWorkOrderId) { this.sourceWorkOrderId = sourceWorkOrderId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getAdoptionCount() { return adoptionCount; }
    public void setAdoptionCount(Integer adoptionCount) { this.adoptionCount = adoptionCount; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Double getWeightScore() { return weightScore; }
    public void setWeightScore(Double weightScore) { this.weightScore = weightScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
