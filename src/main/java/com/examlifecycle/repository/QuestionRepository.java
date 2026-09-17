package com.examlifecycle.repository;

import com.examlifecycle.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByBlueprintIdOrderByIdAsc(Long blueprintId);
    List<Question> findByIdIn(List<Long> ids);
}
