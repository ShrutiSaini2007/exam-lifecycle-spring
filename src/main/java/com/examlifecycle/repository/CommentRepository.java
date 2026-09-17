package com.examlifecycle.repository;

import com.examlifecycle.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByDraftVersionIdOrderByIdAsc(Long draftVersionId);
}
