package com.admin.equipment.repo;

import com.admin.equipment.model.KnowledgeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeAttachmentRepository extends JpaRepository<KnowledgeAttachment, Long> {

    List<KnowledgeAttachment> findByCaseIdOrderByIdAsc(Long caseId);

    void deleteByCaseId(Long caseId);
}
