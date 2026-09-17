package com.examlifecycle.repository;

import com.examlifecycle.domain.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DraftRepository extends JpaRepository<Draft, Long> {
    List<Draft> findAllByOrderByIdDesc();
}
