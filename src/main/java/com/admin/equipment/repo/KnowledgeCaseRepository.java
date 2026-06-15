package com.admin.equipment.repo;

import com.admin.equipment.model.KnowledgeCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeCaseRepository extends JpaRepository<KnowledgeCase, Long> {

    List<KnowledgeCase> findByStatusOrderByIdDesc(String status);

    Page<KnowledgeCase> findByStatusOrderByIdDesc(String status, Pageable pageable);

    List<KnowledgeCase> findByEquipmentTypeAndStatusOrderByIdDesc(String equipmentType, String status);

    List<KnowledgeCase> findByCategoryIdAndStatusOrderByIdDesc(Long categoryId, String status);

    List<KnowledgeCase> findTop10ByStatusOrderByAdoptionCountDesc(String status);

    List<KnowledgeCase> findTop10ByStatusOrderByLikeCountDesc(String status);

    List<KnowledgeCase> findByAuthorUsernameOrderByIdDesc(String username);

    @Query("SELECT k FROM KnowledgeCase k WHERE k.status = :status AND " +
           "(k.equipmentType = :equipmentType OR :equipmentType = '') AND " +
           "(k.title LIKE %:keyword% OR k.faultSymptom LIKE %:keyword% OR " +
           "k.keywords LIKE %:keyword% OR k.tags LIKE %:keyword%)")
    List<KnowledgeCase> searchByKeyword(@Param("status") String status,
                                        @Param("equipmentType") String equipmentType,
                                        @Param("keyword") String keyword);

    long countByStatus(String status);

    long countByAuthorUsername(String username);

    @Query("SELECT k.authorUsername, COUNT(k) FROM KnowledgeCase k WHERE k.status = 'published' GROUP BY k.authorUsername ORDER BY COUNT(k) DESC")
    List<Object[]> countByAuthorGrouped();

    @Query("SELECT k.authorUsername, SUM(k.adoptionCount) FROM KnowledgeCase k WHERE k.status = 'published' GROUP BY k.authorUsername ORDER BY SUM(k.adoptionCount) DESC")
    List<Object[]> sumAdoptionsByAuthorGrouped();
}
