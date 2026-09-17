package com.examlifecycle.service;

import com.examlifecycle.domain.Question;
import com.examlifecycle.dto.CatalogDtos.QuestionDto;
import com.examlifecycle.repository.QuestionRepository;
import com.examlifecycle.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AuditService auditService;

    public QuestionService(QuestionRepository questionRepository, AuditService auditService) {
        this.questionRepository = questionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<Question> importQuestions(CurrentUser user, Long blueprintId, List<QuestionDto> questions) {
        List<Question> saved = questions.stream()
                .filter(q -> q.text() != null && !q.text().isBlank() && q.marks() != null)
                .map(q -> new Question(blueprintId, q.text(), q.topic(), q.marks(), q.difficulty(), user.id()))
                .map(questionRepository::save)
                .toList();
        auditService.record(user.id(), "IMPORT_QUESTIONS", "blueprint", blueprintId,
                java.util.Map.of("count", saved.size()));
        return saved;
    }

    public List<Question> listForBlueprint(Long blueprintId) {
        return questionRepository.findByBlueprintIdOrderByIdAsc(blueprintId);
    }

    public List<Question> findByIds(List<Long> ids) {
        return questionRepository.findByIdIn(ids);
    }
}
