package com.examlifecycle.repository;

import com.examlifecycle.domain.Approval;
import com.examlifecycle.domain.ApprovalDecision;
import com.examlifecycle.domain.ApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByDraftIdOrderByIdAsc(Long draftId);
    Optional<Approval> findFirstByDraftIdAndStageAndDecisionOrderByIdDesc(
            Long draftId, ApprovalStage stage, ApprovalDecision decision);
}
