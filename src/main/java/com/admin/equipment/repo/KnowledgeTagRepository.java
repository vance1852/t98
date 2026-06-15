package com.admin.equipment.repo;

import com.admin.equipment.model.KnowledgeTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeTagRepository extends JpaRepository<KnowledgeTag, Long> {

    Optional<KnowledgeTag> findByName(String name);

    List<KnowledgeTag> findTop20ByOrderByUseCountDesc();

    List<KnowledgeTag> findByNameContainingOrderByUseCountDesc(String name);

    boolean existsByName(String name);
}
