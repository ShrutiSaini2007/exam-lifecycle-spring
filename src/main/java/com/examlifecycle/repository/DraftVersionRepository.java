package com.examlifecycle.repository;

import com.examlifecycle.domain.DraftVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DraftVersionRepository extends JpaRepository<DraftVersion, Long> {
    Optional<DraftVersion> findByDraftIdAndVersionNumber(Long draftId, Integer versionNumber);
    List<DraftVersion> findByDraftIdOrderByVersionNumberAsc(Long draftId);
}
