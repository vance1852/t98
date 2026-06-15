package com.admin.equipment.repo;

import com.admin.equipment.model.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, Long> {

    Optional<KnowledgeCategory> findByName(String name);

    List<KnowledgeCategory> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByName(String name);
}
