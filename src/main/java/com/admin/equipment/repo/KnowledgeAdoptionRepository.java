package com.admin.equipment.repo;

import com.admin.equipment.model.KnowledgeAdoption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeAdoptionRepository extends JpaRepository<KnowledgeAdoption, Long> {

    List<KnowledgeAdoption> findByCaseIdOrderByAdoptedAtDesc(Long caseId);

    List<KnowledgeAdoption> findByWorkOrderIdOrderByAdoptedAtDesc(Long workOrderId);

    List<KnowledgeAdoption> findByAdopterUsernameOrderByAdoptedAtDesc(String username);

    long countByCaseId(Long caseId);

    boolean existsByCaseIdAndWorkOrderId(Long caseId, Long workOrderId);
}
