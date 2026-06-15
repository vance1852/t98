package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeCaseService {

    private static final Set<String> VALID_STATUSES = Set.of("draft", "published", "archived");

    private final KnowledgeCaseRepository caseRepo;
    private final KnowledgeTagRepository tagRepo;
    private final KnowledgeAttachmentRepository attachmentRepo;
    private final EquipmentRepository equipmentRepo;
    private final WorkOrderRepository workOrderRepo;

    public KnowledgeCaseService(KnowledgeCaseRepository caseRepo,
                                KnowledgeTagRepository tagRepo,
                                KnowledgeAttachmentRepository attachmentRepo,
                                EquipmentRepository equipmentRepo,
                                WorkOrderRepository workOrderRepo) {
        this.caseRepo = caseRepo;
        this.tagRepo = tagRepo;
        this.attachmentRepo = attachmentRepo;
        this.equipmentRepo = equipmentRepo;
        this.workOrderRepo = workOrderRepo;
    }

    public Optional<KnowledgeCase> findById(Long id) {
        return caseRepo.findById(id);
    }

    public KnowledgeCase getCaseWithViewIncrement(Long id) {
        KnowledgeCase kase = caseRepo.findById(id).orElse(null);
        if (kase != null) {
            kase.setViewCount(kase.getViewCount() + 1);
            caseRepo.save(kase);
        }
        return kase;
    }

    public List<KnowledgeCase> listCases(String status, String equipmentType, Long categoryId) {
        if (status == null || status.isEmpty()) {
            status = "published";
        }
        if (equipmentType != null && !equipmentType.isEmpty()) {
            return caseRepo.findByEquipmentTypeAndStatusOrderByIdDesc(equipmentType, status);
        }
        if (categoryId != null) {
            return caseRepo.findByCategoryIdAndStatusOrderByIdDesc(categoryId, status);
        }
        return caseRepo.findByStatusOrderByIdDesc(status);
    }

    public List<KnowledgeCase> searchCases(String keyword, String equipmentType, String status) {
        if (status == null || status.isEmpty()) {
            status = "published";
        }
        if (equipmentType == null) {
            equipmentType = "";
        }
        if (keyword == null || keyword.isEmpty()) {
            return listCases(status, equipmentType, null);
        }
        return caseRepo.searchByKeyword(status, equipmentType, keyword);
    }

    @Transactional
    public KnowledgeCase createCase(KnowledgeCase kase, String authorUsername) {
        kase.setId(null);
        kase.setAuthorUsername(authorUsername != null ? authorUsername : "");
        kase.setStatus("draft");
        kase.setLikeCount(0);
        kase.setAdoptionCount(0);
        kase.setViewCount(0);
        kase.setWeightScore(1.0);
        kase.setCreatedAt(LocalDateTime.now());
        kase.setUpdatedAt(LocalDateTime.now());
        kase.setPublishedAt(null);
        updateTagUseCounts(kase.getTags(), Collections.emptySet());
        return caseRepo.save(kase);
    }

    @Transactional
    public KnowledgeCase updateCase(Long id, KnowledgeCase update) {
        KnowledgeCase existing = caseRepo.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        String oldTags = existing.getTags();
        if (update.getCategoryId() != null) {
            existing.setCategoryId(update.getCategoryId());
        }
        if (update.getEquipmentType() != null) {
            existing.setEquipmentType(update.getEquipmentType());
        }
        if (update.getTitle() != null) {
            existing.setTitle(update.getTitle());
        }
        if (update.getFaultSymptom() != null) {
            existing.setFaultSymptom(update.getFaultSymptom());
        }
        if (update.getCauseAnalysis() != null) {
            existing.setCauseAnalysis(update.getCauseAnalysis());
        }
        if (update.getSolutionSteps() != null) {
            existing.setSolutionSteps(update.getSolutionSteps());
        }
        if (update.getSpareParts() != null) {
            existing.setSpareParts(update.getSpareParts());
        }
        if (update.getEstimatedMinutes() != null) {
            existing.setEstimatedMinutes(update.getEstimatedMinutes());
        }
        if (update.getTags() != null) {
            existing.setTags(update.getTags());
        }
        if (update.getKeywords() != null) {
            existing.setKeywords(update.getKeywords());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        if (!Objects.equals(oldTags, existing.getTags())) {
            updateTagUseCounts(existing.getTags(), parseTags(oldTags));
        }
        return caseRepo.save(existing);
    }

    @Transactional
    public boolean deleteCase(Long id) {
        if (!caseRepo.existsById(id)) {
            return false;
        }
        KnowledgeCase kase = caseRepo.findById(id).orElse(null);
        if (kase != null) {
            updateTagUseCounts("", parseTags(kase.getTags()));
        }
        attachmentRepo.deleteByCaseId(id);
        caseRepo.deleteById(id);
        return true;
    }

    @Transactional
    public KnowledgeCase publishCase(Long id) {
        KnowledgeCase kase = caseRepo.findById(id).orElse(null);
        if (kase == null) {
            return null;
        }
        kase.setStatus("published");
        kase.setPublishedAt(LocalDateTime.now());
        kase.setUpdatedAt(LocalDateTime.now());
        return caseRepo.save(kase);
    }

    @Transactional
    public KnowledgeCase archiveCase(Long id) {
        KnowledgeCase kase = caseRepo.findById(id).orElse(null);
        if (kase == null) {
            return null;
        }
        kase.setStatus("archived");
        kase.setUpdatedAt(LocalDateTime.now());
        return caseRepo.save(kase);
    }

    @Transactional
    public KnowledgeCase createFromWorkOrder(Long workOrderId, String authorUsername) {
        WorkOrder order = workOrderRepo.findById(workOrderId).orElse(null);
        if (order == null) {
            return null;
        }
        Equipment equipment = equipmentRepo.findById(order.getEquipmentId()).orElse(null);

        KnowledgeCase kase = new KnowledgeCase();
        kase.setEquipmentType(equipment != null ? equipment.getType() : "");
        kase.setTitle(order.getTitle());
        kase.setFaultSymptom(order.getDescription());
        kase.setSourceWorkOrderId(workOrderId);
        kase.setAuthorUsername(authorUsername != null ? authorUsername : "");
        kase.setStatus("draft");
        kase.setLikeCount(0);
        kase.setAdoptionCount(0);
        kase.setViewCount(0);
        kase.setWeightScore(1.0);
        kase.setCreatedAt(LocalDateTime.now());
        kase.setUpdatedAt(LocalDateTime.now());

        return caseRepo.save(kase);
    }

    @Transactional
    public void incrementLike(Long id) {
        KnowledgeCase kase = caseRepo.findById(id).orElse(null);
        if (kase != null) {
            kase.setLikeCount(kase.getLikeCount() + 1);
            updateWeightScore(kase);
            caseRepo.save(kase);
        }
    }

    @Transactional
    public void incrementAdoption(Long id) {
        KnowledgeCase kase = caseRepo.findById(id).orElse(null);
        if (kase != null) {
            kase.setAdoptionCount(kase.getAdoptionCount() + 1);
            updateWeightScore(kase);
            caseRepo.save(kase);
        }
    }

    private void updateWeightScore(KnowledgeCase kase) {
        double base = 1.0;
        base += kase.getAdoptionCount() * 0.05;
        base += kase.getLikeCount() * 0.02;
        base += kase.getViewCount() * 0.001;
        kase.setWeightScore(Math.min(base, 5.0));
    }

    private void updateTagUseCounts(String newTagsStr, Set<String> oldTags) {
        Set<String> newTags = parseTags(newTagsStr);

        for (String tagName : newTags) {
            if (!oldTags.contains(tagName)) {
                KnowledgeTag tag = tagRepo.findByName(tagName).orElse(null);
                if (tag == null) {
                    tag = new KnowledgeTag();
                    tag.setName(tagName);
                    tag.setUseCount(1);
                    tagRepo.save(tag);
                } else {
                    tag.setUseCount(tag.getUseCount() + 1);
                    tagRepo.save(tag);
                }
            }
        }

        for (String tagName : oldTags) {
            if (!newTags.contains(tagName)) {
                KnowledgeTag tag = tagRepo.findByName(tagName).orElse(null);
                if (tag != null) {
                    tag.setUseCount(Math.max(0, tag.getUseCount() - 1));
                    tagRepo.save(tag);
                }
            }
        }
    }

    private Set<String> parseTags(String tagsStr) {
        Set<String> result = new HashSet<>();
        if (tagsStr == null || tagsStr.isEmpty()) {
            return result;
        }
        String[] parts = tagsStr.split("[,，、;；\\s]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public List<KnowledgeCase> getTopAdopted(int limit) {
        return caseRepo.findTop10ByStatusOrderByAdoptionCountDesc("published")
                .stream().limit(limit).toList();
    }

    public List<KnowledgeCase> getTopLiked(int limit) {
        return caseRepo.findTop10ByStatusOrderByLikeCountDesc("published")
                .stream().limit(limit).toList();
    }
}
