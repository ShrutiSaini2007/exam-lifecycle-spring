package com.examlifecycle.controller;

import com.examlifecycle.domain.Question;
import com.examlifecycle.domain.Role;
import com.examlifecycle.dto.CatalogDtos.*;
import com.examlifecycle.security.AuthUtil;
import com.examlifecycle.service.BlueprintService;
import com.examlifecycle.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blueprints")
public class BlueprintController {

    private final BlueprintService blueprintService;
    private final QuestionService questionService;

    public BlueprintController(BlueprintService blueprintService, QuestionService questionService) {
        this.blueprintService = blueprintService;
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlueprintView create(@Valid @RequestBody BlueprintRequest req) {
        var user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.PAPER_SETTER);
        return blueprintService.create(user, req);
    }

    @GetMapping
    public List<BlueprintView> list() {
        AuthUtil.current();
        return blueprintService.listAll();
    }

    @GetMapping("/{id}")
    public BlueprintView get(@PathVariable Long id) {
        AuthUtil.current();
        return blueprintService.get(id);
    }

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> importQuestions(@PathVariable Long id, @Valid @RequestBody QuestionImportRequest req) {
        var user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.PAPER_SETTER);
        List<Question> saved = questionService.importQuestions(user, id, req.questions());
        return Map.of("imported", saved.size(), "ids", saved.stream().map(Question::getId).toList());
    }

    @GetMapping("/{id}/questions")
    public List<Question> listQuestions(@PathVariable Long id) {
        AuthUtil.current();
        return questionService.listForBlueprint(id);
    }
}
