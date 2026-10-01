package com.patternrun.problem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<ProblemEntity, UUID> {

    @EntityGraph(attributePaths = "primaryPattern")
    Optional<ProblemEntity> findBySlug(String slug);

    @EntityGraph(attributePaths = "primaryPattern")
    Page<ProblemEntity> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "primaryPattern")
    Page<ProblemEntity> findByDifficulty(Difficulty difficulty, Pageable pageable);

    @EntityGraph(attributePaths = "primaryPattern")
    Page<ProblemEntity> findByPrimaryPattern_Slug(String patternSlug, Pageable pageable);

    @EntityGraph(attributePaths = "primaryPattern")
    List<ProblemEntity> findByPrimaryPattern_SlugOrderByExternalIdAsc(String patternSlug);

    long countByPrimaryPattern_Id(UUID patternId);
}