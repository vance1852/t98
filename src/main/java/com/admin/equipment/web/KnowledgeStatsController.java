package com.admin.equipment.web;

import com.admin.equipment.model.KnowledgeAdoption;
import com.admin.equipment.service.KnowledgeStatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeStatsController {

    private final KnowledgeStatsService statsService;

    public KnowledgeStatsController(KnowledgeStatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/cases/{caseId}/adopt")
    public ResponseEntity<?> adopt(@PathVariable Long caseId,
                                   @RequestBody AdoptRequest req,
                                   @RequestHeader(value = "X-Username", required = false) String username) {
        KnowledgeAdoption adoption = statsService.adoptCase(caseId, req.workOrderId(), username);
        if (adoption == null) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("detail", "采纳失败，案例不存在或已采纳"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(adoption);
    }

    @GetMapping("/adoptions")
    public ResponseEntity<?> listAdoptions(@RequestParam(required = false) Long caseId,
                                           @RequestParam(required = false) Long workOrderId,
                                           @RequestParam(required = false) String username) {
        if (caseId != null) {
            return ResponseEntity.ok(statsService.getAdoptionsByCase(caseId));
        }
        if (workOrderId != null) {
            return ResponseEntity.ok(statsService.getAdoptionsByWorkOrder(workOrderId));
        }
        if (username != null) {
            return ResponseEntity.ok(statsService.getAdoptionsByUser(username));
        }
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("detail", "需指定 caseId, workOrderId 或 username"));
    }

    @GetMapping("/stats/overview")
    public Map<String, Object> overview() {
        return statsService.getOverallStats();
    }

    @GetMapping("/stats/contributors")
    public List<Map<String, Object>> contributorRanking() {
        return statsService.getContributorRanking();
    }

    @GetMapping("/stats/contributors/{username}")
    public Map<String, Object> contributorStats(@PathVariable String username) {
        return statsService.getContributorStats(username);
    }

    public record AdoptRequest(Long workOrderId) {}
}
