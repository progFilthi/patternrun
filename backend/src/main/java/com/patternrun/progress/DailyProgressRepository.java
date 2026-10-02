package com.patternrun.progress;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyProgressRepository extends JpaRepository<DailyProgressEntity, UUID> {

    Optional<DailyProgressEntity> findByUserIdAndDay(UUID userId, LocalDate day);

    List<DailyProgressEntity> findByUserIdOrderByDayDesc(UUID userId);

    /** The rows a streak is derived from, newest first. */
    List<DailyProgressEntity> findByUserIdAndGoalMetTrueOrderByDayDesc(UUID userId);
}
