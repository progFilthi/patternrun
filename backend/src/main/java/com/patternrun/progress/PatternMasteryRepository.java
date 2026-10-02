package com.patternrun.progress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternMasteryRepository extends JpaRepository<PatternMasteryEntity, UUID> {

    List<PatternMasteryEntity> findByUserId(UUID userId);

    Optional<PatternMasteryEntity> findByUserIdAndPatternId(UUID userId, UUID patternId);
}
