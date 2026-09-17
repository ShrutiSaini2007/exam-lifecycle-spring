package com.examlifecycle.service;

import com.examlifecycle.domain.Blueprint;
import com.examlifecycle.dto.CatalogDtos.BlueprintRequest;
import com.examlifecycle.dto.CatalogDtos.BlueprintView;
import com.examlifecycle.dto.CatalogDtos.SectionDto;
import com.examlifecycle.exception.ApiException;
import com.examlifecycle.repository.BlueprintRepository;
import com.examlifecycle.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BlueprintService {

    private final BlueprintRepository blueprintRepository;
    private final AuditService auditService;
    private final ObjectMapper mapper;

    public BlueprintService(BlueprintRepository blueprintRepository, AuditService auditService, ObjectMapper mapper) {
        this.blueprintRepository = blueprintRepository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional
    public BlueprintView create(CurrentUser user, BlueprintRequest req) {
        String sectionsJson = writeSections(req.sections());
        Blueprint bp = blueprintRepository.save(new Blueprint(req.courseId(), req.title(), sectionsJson, user.id()));
        auditService.record(user.id(), "CREATE_BLUEPRINT", "blueprint", bp.getId(), java.util.Map.of("title", req.title()));
        return toView(bp);
    }

    public List<BlueprintView> listAll() {
        return blueprintRepository.findAll().stream().map(this::toView).toList();
    }

    public Blueprint getOrThrow(Long id) {
        return blueprintRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "blueprint not found"));
    }

    public BlueprintView get(Long id) {
        return toView(getOrThrow(id));
    }

    private BlueprintView toView(Blueprint bp) {
        return new BlueprintView(bp.getId(), bp.getCourseId(), bp.getTitle(),
                readSections(bp.getSectionsJson()), bp.getCreatedBy(), bp.getCreatedAt());
    }

    private String writeSections(List<SectionDto> sections) {
        try {
            return mapper.writeValueAsString(sections);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize sections", e);
        }
    }

    private List<SectionDto> readSections(String json) {
        try {
            return List.of(mapper.readValue(json, SectionDto[].class));
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse stored sections", e);
        }
    }
}
