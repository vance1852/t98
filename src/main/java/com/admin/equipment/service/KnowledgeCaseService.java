package com.admin.equipment.service;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        kase.setCauseAnalysis(order.getCauseAnalysis() != null ? order.getCauseAnalysis() : "");
        kase.setSolutionSteps(order.getSolutionSteps() != null ? order.getSolutionSteps() : "");
        kase.setSpareParts(order.getSparePartsUsed() != null ? order.getSparePartsUsed() : "");
        kase.setEstimatedMinutes(order.getActualMinutes() != null ? order.getActualMinutes() : 0);
        if (order.getResolutionSummary() != null && !order.getResolutionSummary().isEmpty()) {
            if (kase.getSolutionSteps().isEmpty()) {
                kase.setSolutionSteps(order.getResolutionSummary());
            }
        }
        kase.setKeywords(generateKeywordsFromOrder(order));
        kase.setTags(kase.getKeywords());
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

    private String generateKeywordsFromOrder(WorkOrder order) {
        Set<String> keywords = new LinkedHashSet<>();
        if (order.getType() != null && !order.getType().isEmpty()) {
            if ("repair".equals(order.getType())) keywords.add("维修");
            else if ("maintenance".equals(order.getType())) keywords.add("保养");
            else if ("inspection".equals(order.getType())) keywords.add("巡检");
        }
        if (order.getPriority() != null && !"medium".equals(order.getPriority())) {
            if ("urgent".equals(order.getPriority())) keywords.add("紧急");
            else if ("high".equals(order.getPriority())) keywords.add("重要");
        }
        String allText = (order.getTitle() != null ? order.getTitle() : "")
                + " " + (order.getDescription() != null ? order.getDescription() : "")
                + " " + (order.getCauseAnalysis() != null ? order.getCauseAnalysis() : "")
                + " " + (order.getResolutionSummary() != null ? order.getResolutionSummary() : "");

        String[] commonTerms = {"故障", "异常", "报警", "停机", "损坏", "泄漏", "堵塞", "磨损",
                               "过热", "异响", "振动", "过载", "短路", "接地", "缺相",
                               "压力", "流量", "温度", "电流", "电压", "轴承", "密封",
                               "滤芯", "滤网", "油泵", "电机", "阀门", "管道"};
        for (String term : commonTerms) {
            if (allText.contains(term)) {
                keywords.add(term);
            }
        }
        return String.join(",", keywords);
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

    private static final String ATTACHMENT_DIR = "./data/knowledge-attachments/";

    public List<KnowledgeAttachment> listAttachments(Long caseId) {
        return attachmentRepo.findByCaseIdOrderByIdAsc(caseId);
    }

    @Transactional
    public KnowledgeAttachment addAttachment(Long caseId, MultipartFile file) throws IOException {
        KnowledgeCase kase = caseRepo.findById(caseId).orElse(null);
        if (kase == null) {
            return null;
        }

        File dir = new File(ATTACHMENT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String storedFilename = System.currentTimeMillis() + "_" +
                (originalFilename != null ? originalFilename : "file");
        Path filePath = Paths.get(ATTACHMENT_DIR, storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        KnowledgeAttachment attachment = new KnowledgeAttachment();
        attachment.setCaseId(caseId);
        attachment.setFilename(originalFilename != null ? originalFilename : "file");
        attachment.setFilePath(filePath.toString());
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType() != null ? file.getContentType() : "");

        return attachmentRepo.save(attachment);
    }

    @Transactional
    public boolean deleteAttachment(Long attachmentId) {
        KnowledgeAttachment attachment = attachmentRepo.findById(attachmentId).orElse(null);
        if (attachment == null) {
            return false;
        }
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
        }
        attachmentRepo.deleteById(attachmentId);
        return true;
    }

    public KnowledgeAttachment getAttachment(Long attachmentId) {
        return attachmentRepo.findById(attachmentId).orElse(null);
    }
}
