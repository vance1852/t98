package com.admin.equipment.service;

import com.admin.equipment.model.KnowledgeCase;
import com.admin.equipment.repo.KnowledgeCaseRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimilarityService {

    private static final double WEIGHT_EQUIPMENT_TYPE = 25.0;
    private static final double WEIGHT_TAG_MATCH = 8.0;
    private static final double WEIGHT_KEYWORD_MATCH = 6.0;
    private static final double WEIGHT_TITLE_TF = 4.0;
    private static final double WEIGHT_SYMPTOM_TF = 3.0;
    private static final double WEIGHT_CAUSE_TF = 1.5;
    private static final double WEIGHT_ADOPTION_BOOST = 0.02;
    private static final double WEIGHT_LIKE_BOOST = 0.01;
    private static final double MIN_SCORE_THRESHOLD = 5.0;

    private final KnowledgeCaseRepository caseRepo;

    public SimilarityService(KnowledgeCaseRepository caseRepo) {
        this.caseRepo = caseRepo;
    }

    public static class SimilarityResult {
        private final KnowledgeCase knowledgeCase;
        private final double score;
        private final List<String> matchedTags;
        private final List<String> matchedKeywords;
        private final Map<String, Double> scoreBreakdown;

        public SimilarityResult(KnowledgeCase knowledgeCase, double score,
                                List<String> matchedTags, List<String> matchedKeywords,
                                Map<String, Double> scoreBreakdown) {
            this.knowledgeCase = knowledgeCase;
            this.score = score;
            this.matchedTags = matchedTags;
            this.matchedKeywords = matchedKeywords;
            this.scoreBreakdown = scoreBreakdown;
        }

        public KnowledgeCase getKnowledgeCase() { return knowledgeCase; }
        public double getScore() { return score; }
        public List<String> getMatchedTags() { return matchedTags; }
        public List<String> getMatchedKeywords() { return matchedKeywords; }
        public Map<String, Double> getScoreBreakdown() { return scoreBreakdown; }
    }

    public List<SimilarityResult> findSimilarCases(String equipmentType, String faultDescription,
                                                   String inputKeywords, int limit) {
        List<KnowledgeCase> candidates = caseRepo.findByStatusOrderByIdDesc("published");

        Set<String> queryTerms = extractTerms(faultDescription);
        Set<String> queryKeywords = parseCommaSeparated(inputKeywords);

        List<SimilarityResult> results = new ArrayList<>();

        for (KnowledgeCase kase : candidates) {
            SimilarityResult result = calculateSimilarity(kase, equipmentType, queryTerms, queryKeywords);
            if (result.getScore() >= MIN_SCORE_THRESHOLD) {
                results.add(result);
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private SimilarityResult calculateSimilarity(KnowledgeCase kase, String equipmentType,
                                                 Set<String> queryTerms, Set<String> queryKeywords) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        double totalScore = 0.0;
        List<String> matchedTags = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();

        if (equipmentType != null && !equipmentType.isEmpty() &&
            equipmentType.equalsIgnoreCase(kase.getEquipmentType())) {
            totalScore += WEIGHT_EQUIPMENT_TYPE;
            breakdown.put("设备类型匹配", WEIGHT_EQUIPMENT_TYPE);
        }

        Set<String> caseTags = parseCommaSeparated(kase.getTags());
        int tagHits = 0;
        for (String tag : caseTags) {
            if (queryTerms.contains(tag.toLowerCase()) || queryKeywords.contains(tag.toLowerCase())) {
                tagHits++;
                matchedTags.add(tag);
            }
        }
        if (tagHits > 0) {
            double tagScore = tagHits * WEIGHT_TAG_MATCH;
            totalScore += tagScore;
            breakdown.put("标签匹配(" + tagHits + "个)", tagScore);
        }

        Set<String> caseKeywords = parseCommaSeparated(kase.getKeywords());
        int keywordHits = 0;
        for (String kw : caseKeywords) {
            String kwLower = kw.toLowerCase();
            if (queryTerms.contains(kwLower) || queryKeywords.contains(kwLower)) {
                keywordHits++;
                matchedKeywords.add(kw);
            }
        }
        if (keywordHits > 0) {
            double kwScore = keywordHits * WEIGHT_KEYWORD_MATCH;
            totalScore += kwScore;
            breakdown.put("关键词匹配(" + keywordHits + "个)", kwScore);
        }

        int titleHits = countTermHits(queryTerms, kase.getTitle());
        if (titleHits > 0) {
            double titleScore = titleHits * WEIGHT_TITLE_TF;
            totalScore += titleScore;
            breakdown.put("标题词频(" + titleHits + "次)", titleScore);
        }

        int symptomHits = countTermHits(queryTerms, kase.getFaultSymptom());
        if (symptomHits > 0) {
            double symptomScore = symptomHits * WEIGHT_SYMPTOM_TF;
            totalScore += symptomScore;
            breakdown.put("故障现象词频(" + symptomHits + "次)", symptomScore);
        }

        int causeHits = countTermHits(queryTerms, kase.getCauseAnalysis());
        if (causeHits > 0) {
            double causeScore = causeHits * WEIGHT_CAUSE_TF;
            totalScore += causeScore;
            breakdown.put("原因分析词频(" + causeHits + "次)", causeScore);
        }

        double adoptionBoost = kase.getAdoptionCount() * WEIGHT_ADOPTION_BOOST;
        double likeBoost = kase.getLikeCount() * WEIGHT_LIKE_BOOST;
        double qualityBoost = adoptionBoost + likeBoost;
        if (qualityBoost > 0) {
            totalScore += qualityBoost;
            breakdown.put("质量加分", Math.round(qualityBoost * 100.0) / 100.0);
        }

        double weightBoost = kase.getWeightScore() - 1.0;
        if (weightBoost > 0) {
            double weightScore = totalScore * weightBoost;
            totalScore += weightScore;
            breakdown.put("权重加成", Math.round(weightScore * 100.0) / 100.0);
        }

        breakdown.put("总分", Math.round(totalScore * 100.0) / 100.0);

        return new SimilarityResult(kase, totalScore, matchedTags, matchedKeywords, breakdown);
    }

    private Set<String> extractTerms(String text) {
        Set<String> terms = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return terms;
        }

        String lower = text.toLowerCase();

        String[] words = lower.split("[\\s,，。.!?？、；：;:\"'()（）\\[\\]【】《》<>/\\\\\\-—_=+@#$%^&*`~]+");
        for (String word : words) {
            if (word.length() >= 2) {
                terms.add(word);
            }
        }

        for (int i = 0; i < lower.length() - 1; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            if (isChinese(c1) && isChinese(c2)) {
                terms.add("" + c1 + c2);
            }
        }

        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);
            if (isChinese(c1) && isChinese(c2) && isChinese(c3)) {
                terms.add("" + c1 + c2 + c3);
            }
        }

        return terms;
    }

    private int countTermHits(Set<String> queryTerms, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int hits = 0;
        String lower = text.toLowerCase();
        for (String term : queryTerms) {
            if (lower.contains(term)) {
                hits++;
            }
        }
        return hits;
    }

    private Set<String> parseCommaSeparated(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        String[] parts = text.split("[,，、;；\\s]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }
}
