package com.patternrun.pattern;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternRepository extends JpaRepository<PatternEntity, UUID> {

    Optional<PatternEntity> findBySlug(String slug);

    List<PatternEntity> findAllByOrderByDifficultyOrderAsc();
}