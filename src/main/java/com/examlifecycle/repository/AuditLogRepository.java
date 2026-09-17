package com.examlifecycle.repository;

import com.examlifecycle.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByIdDesc();

    @Query("select a from AuditLog a where (a.entityType = 'draft' and a.entityId = :draftId) " +
           "or (a.entityType = 'draft_version' and a.entityId in :versionIds) order by a.id asc")
    List<AuditLog> findForDraft(Long draftId, List<Long> versionIds);
}
