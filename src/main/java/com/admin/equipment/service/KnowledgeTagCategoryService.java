package com.admin.equipment.service;

import com.admin.equipment.model.KnowledgeCategory;
import com.admin.equipment.model.KnowledgeTag;
import com.admin.equipment.repo.KnowledgeCategoryRepository;
import com.admin.equipment.repo.KnowledgeTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class KnowledgeTagCategoryService {

    private final KnowledgeTagRepository tagRepo;
    private final KnowledgeCategoryRepository categoryRepo;

    public KnowledgeTagCategoryService(KnowledgeTagRepository tagRepo,
                                       KnowledgeCategoryRepository categoryRepo) {
        this.tagRepo = tagRepo;
        this.categoryRepo = categoryRepo;
    }

    public List<KnowledgeTag> listTags(String keyword, int limit) {
        if (keyword != null && !keyword.isEmpty()) {
            return tagRepo.findByNameContainingOrderByUseCountDesc(keyword)
                    .stream().limit(limit).toList();
        }
        return tagRepo.findTop20ByOrderByUseCountDesc();
    }

    @Transactional
    public KnowledgeTag createTag(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        if (tagRepo.existsByName(trimmed)) {
            return tagRepo.findByName(trimmed).orElse(null);
        }
        KnowledgeTag tag = new KnowledgeTag();
        tag.setName(trimmed);
        tag.setUseCount(0);
        return tagRepo.save(tag);
    }

    @Transactional
    public boolean deleteTag(Long id) {
        if (!tagRepo.existsById(id)) {
            return false;
        }
        tagRepo.deleteById(id);
        return true;
    }

    public Optional<KnowledgeTag> findTagById(Long id) {
        return tagRepo.findById(id);
    }

    public List<KnowledgeCategory> listCategories() {
        return categoryRepo.findAllByOrderBySortOrderAscIdAsc();
    }

    public Optional<KnowledgeCategory> findCategoryById(Long id) {
        return categoryRepo.findById(id);
    }

    @Transactional
    public KnowledgeCategory createCategory(String name, String description, Integer sortOrder) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        if (categoryRepo.existsByName(trimmed)) {
            return null;
        }
        KnowledgeCategory category = new KnowledgeCategory();
        category.setName(trimmed);
        category.setDescription(description != null ? description : "");
        category.setSortOrder(sortOrder != null ? sortOrder : 0);
        return categoryRepo.save(category);
    }

    @Transactional
    public KnowledgeCategory updateCategory(Long id, String name, String description, Integer sortOrder) {
        KnowledgeCategory existing = categoryRepo.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            if (!trimmed.equals(existing.getName()) && categoryRepo.existsByName(trimmed)) {
                return null;
            }
            existing.setName(trimmed);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (sortOrder != null) {
            existing.setSortOrder(sortOrder);
        }
        return categoryRepo.save(existing);
    }

    @Transactional
    public boolean deleteCategory(Long id) {
        if (!categoryRepo.existsById(id)) {
            return false;
        }
        categoryRepo.deleteById(id);
        return true;
    }
}
