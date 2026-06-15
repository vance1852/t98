package com.admin.equipment.web;

import com.admin.equipment.model.KnowledgeCategory;
import com.admin.equipment.model.KnowledgeTag;
import com.admin.equipment.service.KnowledgeTagCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeTagCategoryController {

    private final KnowledgeTagCategoryService service;

    public KnowledgeTagCategoryController(KnowledgeTagCategoryService service) {
        this.service = service;
    }

    @GetMapping("/tags")
    public List<KnowledgeTag> listTags(@RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "20") int limit) {
        return service.listTags(keyword, limit);
    }

    @PostMapping("/tags")
    public ResponseEntity<?> createTag(@RequestBody TagRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("detail", "标签名称必填"));
        }
        KnowledgeTag tag = service.createTag(req.name());
        if (tag == null) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("detail", "创建失败"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(tag);
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        boolean deleted = service.deleteTag(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "标签不存在"));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public List<KnowledgeCategory> listCategories() {
        return service.listCategories();
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<?> getCategory(@PathVariable Long id) {
        KnowledgeCategory category = service.findCategoryById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "分类不存在"));
        }
        return ResponseEntity.ok(category);
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("detail", "分类名称必填"));
        }
        KnowledgeCategory category = service.createCategory(req.name(), req.description(), req.sortOrder());
        if (category == null) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("detail", "分类名称已存在"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest req) {
        KnowledgeCategory updated = service.updateCategory(id, req.name(), req.description(), req.sortOrder());
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "分类不存在或名称冲突"));
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        boolean deleted = service.deleteCategory(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "分类不存在"));
        }
        return ResponseEntity.noContent().build();
    }

    public record TagRequest(String name) {}
    public record CategoryRequest(String name, String description, Integer sortOrder) {}
}
