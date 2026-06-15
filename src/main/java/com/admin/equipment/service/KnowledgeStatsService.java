package com.admin.equipment.service;

import com.admin.equipment.model.KnowledgeAdoption;
import com.admin.equipment.model.KnowledgeCase;
import com.admin.equipment.model.RecommendationLog;
import com.admin.equipment.repo.KnowledgeAdoptionRepository;
import com.admin.equipment.repo.KnowledgeCaseRepository;
import com.admin.equipment.repo.RecommendationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeStatsService {

    private final KnowledgeAdoptionRepository adoptionRepo;
    private final RecommendationLogRepository recLogRepo;
    private final KnowledgeCaseRepository caseRepo;
    private final KnowledgeCaseService caseService;

    public KnowledgeStatsService(KnowledgeAdoptionRepository adoptionRepo,
                                 RecommendationLogRepository recLogRepo,
                                 KnowledgeCaseRepository caseRepo,
                                 KnowledgeCaseService caseService) {
        this.adoptionRepo = adoptionRepo;
        this.recLogRepo = recLogRepo;
        this.caseRepo = caseRepo;
        this.caseService = caseService;
    }

    @Transactional
    public KnowledgeAdoption adoptCase(Long caseId, Long workOrderId, String adopterUsername) {
        if (workOrderId != null && adoptionRepo.existsByCaseIdAndWorkOrderId(caseId, workOrderId)) {
            return null;
        }
        KnowledgeCase kase = caseRepo.findById(caseId).orElse(null);
        if (kase == null || !"published".equals(kase.getStatus())) {
            return null;
        }

        KnowledgeAdoption adoption = new KnowledgeAdoption();
        adoption.setCaseId(caseId);
        adoption.setWorkOrderId(workOrderId);
        adoption.setAdopterUsername(adopterUsername != null ? adopterUsername : "");
        adoption.setAdoptedAt(LocalDateTime.now());

        caseService.incrementAdoption(caseId);

        return adoptionRepo.save(adoption);
    }

    public List<KnowledgeAdoption> getAdoptionsByCase(Long caseId) {
        return adoptionRepo.findByCaseIdOrderByAdoptedAtDesc(caseId);
    }

    public List<KnowledgeAdoption> getAdoptionsByWorkOrder(Long workOrderId) {
        return adoptionRepo.findByWorkOrderIdOrderByAdoptedAtDesc(workOrderId);
    }

    public List<KnowledgeAdoption> getAdoptionsByUser(String username) {
        return adoptionRepo.findByAdopterUsernameOrderByAdoptedAtDesc(username);
    }

    @Transactional
    public RecommendationLog logRecommendation(Long workOrderId, String equipmentType,
                                               String queryText, List<Long> recommendedCaseIds,
                                               int hitCount) {
        RecommendationLog log = new RecommendationLog();
        log.setWorkOrderId(workOrderId);
        log.setEquipmentType(equipmentType != null ? equipmentType : "");
        log.setQueryText(queryText != null ? queryText : "");
        log.setRecommendedCaseIds(recommendedCaseIds != null ?
                recommendedCaseIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") : "");
        log.setHitCount(hitCount);
        log.setCreatedAt(LocalDateTime.now());
        return recLogRepo.save(log);
    }

    @Transactional
    public void markRecommendationAdopted(Long recommendationLogId, Long caseId) {
        RecommendationLog log = recLogRepo.findById(recommendationLogId).orElse(null);
        if (log != null) {
            log.setAdoptedCaseId(caseId);
            recLogRepo.save(log);
        }
    }

    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalCases = caseRepo.count();
        long publishedCases = caseRepo.countByStatus("published");
        long draftCases = caseRepo.countByStatus("draft");
        long archivedCases = caseRepo.countByStatus("archived");

        stats.put("totalCases", totalCases);
        stats.put("publishedCases", publishedCases);
        stats.put("draftCases", draftCases);
        stats.put("archivedCases", archivedCases);

        long totalAdoptions = adoptionRepo.count();
        stats.put("totalAdoptions", totalAdoptions);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recTotal = recLogRepo.countByCreatedAtAfter(sevenDaysAgo);
        long recAdopted = recLogRepo.countByAdoptedCaseIdIsNotNullAndCreatedAtAfter(sevenDaysAgo);
        long recWithHits = recLogRepo.countWithHitsAfter(sevenDaysAgo);

        stats.put("recommendationTotal7d", recTotal);
        stats.put("recommendationAdopted7d", recAdopted);
        stats.put("recommendationHitRate7d", recTotal > 0 ?
                Math.round(recWithHits * 10000.0 / recTotal) / 100.0 : 0.0);
        stats.put("recommendationAdoptionRate7d", recTotal > 0 ?
                Math.round(recAdopted * 10000.0 / recTotal) / 100.0 : 0.0);

        return stats;
    }

    public List<Map<String, Object>> getContributorRanking() {
        List<Object[]> caseCounts = caseRepo.countByAuthorGrouped();
        List<Object[]> adoptionSums = caseRepo.sumAdoptionsByAuthorGrouped();

        Map<String, Long> adoptionMap = new HashMap<>();
        for (Object[] row : adoptionSums) {
            String author = (String) row[0];
            Long adoptions = ((Number) row[1]).longValue();
            adoptionMap.put(author, adoptions);
        }

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Object[] row : caseCounts) {
            Map<String, Object> item = new LinkedHashMap<>();
            String author = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            item.put("username", author);
            item.put("caseCount", count);
            item.put("adoptionCount", adoptionMap.getOrDefault(author, 0L));
            ranking.add(item);
        }

        ranking.sort((a, b) -> Long.compare(
                (Long) b.get("adoptionCount"),
                (Long) a.get("adoptionCount")));

        return ranking;
    }

    public Map<String, Object> getContributorStats(String username) {
        Map<String, Object> stats = new LinkedHashMap<>();
        long caseCount = caseRepo.countByAuthorUsername(username);
        List<KnowledgeCase> cases = caseRepo.findByAuthorUsernameOrderByIdDesc(username);
        int totalAdoptions = 0;
        int totalLikes = 0;
        int totalViews = 0;
        for (KnowledgeCase kase : cases) {
            totalAdoptions += kase.getAdoptionCount();
            totalLikes += kase.getLikeCount();
            totalViews += kase.getViewCount();
        }
        stats.put("username", username);
        stats.put("caseCount", caseCount);
        stats.put("totalAdoptions", totalAdoptions);
        stats.put("totalLikes", totalLikes);
        stats.put("totalViews", totalViews);
        stats.put("cases", cases);
        return stats;
    }
}
