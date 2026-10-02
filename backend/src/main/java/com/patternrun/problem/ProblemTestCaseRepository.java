package com.patternrun.problem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProblemTestCaseRepository extends JpaRepository<ProblemTestCaseEntity, UUID> {

    List<ProblemTestCaseEntity> findByProblemIdAndIsHiddenFalseOrderByOrdinalAsc(UUID problemId);

    long countByProblemIdAndIsHiddenFalse(UUID problemId);

    /**
     * Every runnable case, hidden included, ordered as authored.
     *
     * For the runner only. Nothing maps this to a response DTO, which is the mechanism that keeps
     * hidden cases from reaching the browser: they are selected here, evaluated here, and the
     * only thing that leaves is a boolean and a value the learner's own code produced.
     */
    @Query("""
            select case_ from ProblemTestCaseEntity case_
             where case_.problem.id = :problemId
               and case_.call is not null
               and case_.expectedJson is not null
             order by case_.ordinal asc
            """)
    List<ProblemTestCaseEntity> findRunnableByProblemId(UUID problemId);

    /** The visible subset of {@link #findRunnableByProblemId}, for Run. */
    @Query("""
            select case_ from ProblemTestCaseEntity case_
             where case_.problem.id = :problemId
               and case_.isHidden = false
               and case_.call is not null
               and case_.expectedJson is not null
             order by case_.ordinal asc
            """)
    List<ProblemTestCaseEntity> findRunnableVisibleByProblemId(UUID problemId);
}