package com.admin.equipment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_adoptions")
public class KnowledgeAdoption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "adopter_username", length = 64)
    private String adopterUsername = "";

    @Column(name = "adopted_at")
    private LocalDateTime adoptedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public String getAdopterUsername() { return adopterUsername; }
    public void setAdopterUsername(String adopterUsername) { this.adopterUsername = adopterUsername; }
    public LocalDateTime getAdoptedAt() { return adoptedAt; }
    public void setAdoptedAt(LocalDateTime adoptedAt) { this.adoptedAt = adoptedAt; }
}
