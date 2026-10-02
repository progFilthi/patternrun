package com.patternrun.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patternrun.account.UserEntity;
import com.patternrun.attempt.AttemptEntity;
import com.patternrun.attempt.AttemptRepository;
import com.patternrun.common.ResourceNotFoundException;
import com.patternrun.execution.dto.ExecuteCodeRequest;
import com.patternrun.problem.ProblemEntity;
import com.patternrun.problem.ProblemService;
import com.patternrun.problem.ProblemTestCaseEntity;
import com.patternrun.problem.ProblemTestCaseRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The refusals the integration test can no longer set up.
 *
 * One case here used to be an end-to-end test that pointed at a problem with no runnable content.
 * Every problem is runnable now, so the fixture stopped existing and that test failed --- which was
 * the right failure, because it was the same assertion noticing the content gap had been closed.
 *
 * The behaviour still matters. Content gaps are routine on this project: a problem added without
 * test arguments, a half-authored case, a server started without a runner. None of those are
 * reachable through the API now, so the state is built here instead of being waited for.
 *
 * Scope note: the attempt-ownership and finished-session refusals are deliberately absent, because
 * a mock cannot run an interface's default method and CodeExecutionApiIT already covers both
 * against a real repository. Duplicating them here would test Mockito rather than the code.
 */
@ExtendWith(MockitoExtension.class)
class CodeExecutionServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();
    private static final UUID PROBLEM_ID = UUID.randomUUID();

    @Mock
    private CodeExecutionRepository executions;
    @Mock
    private AttemptRepository attempts;
    @Mock
    private ProblemTestCaseRepository testCases;
    @Mock
    private ProblemService problems;

    private CodeExecutionService service;

    @BeforeEach
    void setUp() {
        service = new CodeExecutionService(executions, attempts, testCases, problems,
                new ExecutionProperties(), List.of());
    }

    private AttemptEntity live() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        AttemptEntity attempt = new AttemptEntity();
        attempt.setId(ATTEMPT_ID);
        attempt.setUser(user);
        attempt.setProblem(new ProblemEntity());
        attempt.getProblem().setId(PROBLEM_ID);
        attempt.getProblem().setSlug("no-editor-yet");
        return attempt;
    }

    @Test
    @DisplayName("A problem with no entrypoint is refused, not offered an editor that cannot work")
    void refusesAProblemWithNoEntrypoint() {
        AttemptEntity attempt = live();
        when(attempts.requireLive(USER_ID, ATTEMPT_ID)).thenReturn(attempt);

        // entrypoint is null: display-only test cases, which is every problem added without a
        // structured `call`.
        for (var call : List.<Executable>of(
                () -> service.run(live().getUser(), ATTEMPT_ID, request()),
                () -> service.submit(live().getUser(), ATTEMPT_ID, request()))) {
            assertThatThrownBy(call::run)
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not available for coding yet");
        }

        verify(executions, never()).save(any());
    }

    @Test
    @DisplayName("A blank entrypoint is as absent as a missing one")
    void refusesABlankEntrypoint() {
        AttemptEntity attempt = live();
        attempt.getProblem().setEntrypoint("   ");
        when(attempts.requireLive(USER_ID, ATTEMPT_ID)).thenReturn(attempt);

        assertThatThrownBy(() -> service.submit(attempt.getUser(), ATTEMPT_ID, request()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("A disabled runner reports unavailable instead of falling back to anything")
    void aDisabledRunnerRefuses() {
        // No providers at all, which is what the container looks like with execution switched off.
        AttemptEntity attempt = live();
        attempt.getProblem().setEntrypoint("solve");
        when(attempts.requireLive(USER_ID, ATTEMPT_ID)).thenReturn(attempt);
        when(testCases.findRunnableByProblemId(PROBLEM_ID)).thenReturn(List.of());

        var result = service.submit(attempt.getUser(), ATTEMPT_ID, request());

        assertThat(result.outcome()).isEqualTo("INTERNAL_ERROR");
        // The raw "not enabled on this server" never reaches the browser; the service replaces it
        // with words that do not read as a verdict on the learner's code.
        assertThat(result.error().message()).contains("not with your code");
        assertThat(result.cases()).as("a runner that ran nothing has no per-case results").isEmpty();
    }

    @Test
    @DisplayName("A runnable case with no expected value is still not runnable")
    void aCaseMissingItsAnswerIsNotRunnable() {
        ProblemTestCaseEntity testCase = new ProblemTestCaseEntity();
        testCase.setLabel("half authored");
        testCase.setIsHidden(false);
        testCase.setInputData("nums = [1, 2]");
        testCase.setExpectedOutput("[0, 1]");
        // `call` set but `expected_json` absent: exactly the half-authored shape seed validation
        // rejects at startup, checked here so the query cannot reintroduce it.
        testCase.setCall(null);

        assertThat(testCase.isRunnable()).isFalse();
        assertThat(testCases.findRunnableByProblemId(PROBLEM_ID)).isEmpty();
    }

    private ExecuteCodeRequest request() {
        return new ExecuteCodeRequest("PYTHON", "def solve(nums):\n    return []\n");
    }

    @FunctionalInterface
    private interface Executable {
        void run() throws Exception;
    }
}