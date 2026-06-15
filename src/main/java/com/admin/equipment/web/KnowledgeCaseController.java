package com.admin.equipment.web;

import com.admin.equipment.model.KnowledgeCase;
import com.admin.equipment.service.KnowledgeCaseService;
import com.admin.equipment.service.KnowledgeStatsService;
import com.admin.equipment.service.SimilarityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge/cases")
public class KnowledgeCaseController {

    private final KnowledgeCaseService caseService;
    private final SimilarityService similarityService;
    private final KnowledgeStatsService statsService;

    public KnowledgeCaseController(KnowledgeCaseService caseService,
                                   SimilarityService similarityService,
                                   KnowledgeStatsService statsService) {
        this.caseService = caseService;
        this.similarityService = similarityService;
        this.statsService = statsService;
    }

    @GetMapping
    public List<KnowledgeCase> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String equipmentType,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return caseService.searchCases(keyword, equipmentType, status);
        }
        return caseService.listCases(status, equipmentType, categoryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id,
                                 @RequestParam(defaultValue = "false") boolean incrementView) {
        KnowledgeCase kase;
        if (incrementView) {
            kase = caseService.getCaseWithViewIncrement(id);
        } else {
            kase = caseService.findById(id).orElse(null);
        }
        if (kase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "案例不存在"));
        }
        return ResponseEntity.ok(kase);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CaseCreateRequest req,
                                    @RequestHeader(value = "X-Username", required = false) String username) {
        if (req.title == null || req.title.isBlank()) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("detail", "标题必填"));
        }
        KnowledgeCase kase = new KnowledgeCase();
        kase.setCategoryId(req.categoryId);
        kase.setEquipmentType(req.equipmentType != null ? req.equipmentType : "");
        kase.setTitle(req.title);
        kase.setFaultSymptom(req.faultSymptom != null ? req.faultSymptom : "");
        kase.setCauseAnalysis(req.causeAnalysis != null ? req.causeAnalysis : "");
        kase.setSolutionSteps(req.solutionSteps != null ? req.solutionSteps : "");
        kase.setSpareParts(req.spareParts != null ? req.spareParts : "");
        kase.setEstimatedMinutes(req.estimatedMinutes != null ? req.estimatedMinutes : 0);
        kase.setTags(req.tags != null ? req.tags : "");
        kase.setKeywords(req.keywords != null ? req.keywords : "");
        KnowledgeCase created = caseService.createCase(kase, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CaseUpdateRequest req) {
        KnowledgeCase update = new KnowledgeCase();
        update.setCategoryId(req.categoryId());
        update.setEquipmentType(req.equipmentType());
        update.setTitle(req.title());
        update.setFaultSymptom(req.faultSymptom());
        update.setCauseAnalysis(req.causeAnalysis());
        update.setSolutionSteps(req.solutionSteps());
        update.setSpareParts(req.spareParts());
        update.setEstimatedMinutes(req.estimatedMinutes());
        update.setTags(req.tags());
        update.setKeywords(req.keywords());
        KnowledgeCase updated = caseService.updateCase(id, update);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "案例不存在"));
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean deleted = caseService.deleteCase(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "案例不存在"));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id) {
        KnowledgeCase kase = caseService.publishCase(id);
        if (kase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "案例不存在"));
        }
        return ResponseEntity.ok(kase);
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable Long id) {
        KnowledgeCase kase = caseService.archiveCase(id);
        if (kase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "案例不存在"));
        }
        return ResponseEntity.ok(kase);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> like(@PathVariable Long id) {
        KnowledgeCase kase = caseService.findById(id).orElse(null);
        if (kase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "案例不存在"));
        }
        caseService.incrementLike(id);
        return ResponseEntity.ok(Map.of("detail", "点赞成功"));
    }

    @PostMapping("/from-work-order/{workOrderId}")
    public ResponseEntity<?> createFromWorkOrder(@PathVariable Long workOrderId,
                                                 @RequestHeader(value = "X-Username", required = false) String username) {
        KnowledgeCase kase = caseService.createFromWorkOrder(workOrderId, username);
        if (kase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "工单不存在"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(kase);
    }

    @GetMapping("/similar/recommend")
    public ResponseEntity<?> recommend(@RequestParam(required = false) String equipmentType,
                                       @RequestParam(required = false) String faultDescription,
                                       @RequestParam(required = false) String keywords,
                                       @RequestParam(defaultValue = "5") int limit,
                                       @RequestParam(required = false) Long workOrderId) {
        if (limit < 1) limit = 1;
        if (limit > 20) limit = 20;

        List<SimilarityService.SimilarityResult> results =
                similarityService.findSimilarCases(
                        equipmentType != null ? equipmentType : "",
                        faultDescription != null ? faultDescription : "",
                        keywords != null ? keywords : "",
                        limit);

        List<Long> caseIds = results.stream()
                .map(r -> r.getKnowledgeCase().getId())
                .collect(Collectors.toList());

        if (workOrderId != null) {
            statsService.logRecommendation(
                    workOrderId,
                    equipmentType,
                    faultDescription != null ? faultDescription : (keywords != null ? keywords : ""),
                    caseIds,
                    results.size());
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (SimilarityService.SimilarityResult result : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("case", result.getKnowledgeCase());
            item.put("score", Math.round(result.getScore() * 100.0) / 100.0);
            item.put("matchedTags", result.getMatchedTags());
            item.put("matchedKeywords", result.getMatchedKeywords());
            item.put("scoreBreakdown", result.getScoreBreakdown());
            response.add(item);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ranking/adoption")
    public List<KnowledgeCase> topAdopted(@RequestParam(defaultValue = "10") int limit) {
        return caseService.getTopAdopted(limit);
    }

    @GetMapping("/ranking/like")
    public List<KnowledgeCase> topLiked(@RequestParam(defaultValue = "10") int limit) {
        return caseService.getTopLiked(limit);
    }

    public record CaseCreateRequest(Long categoryId, String equipmentType, String title,
                                    String faultSymptom, String causeAnalysis, String solutionSteps,
                                    String spareParts, Integer estimatedMinutes, String tags, String keywords) {}

    public record CaseUpdateRequest(Long categoryId, String equipmentType, String title,
                                    String faultSymptom, String causeAnalysis, String solutionSteps,
                                    String spareParts, Integer estimatedMinutes, String tags, String keywords) {}
}
