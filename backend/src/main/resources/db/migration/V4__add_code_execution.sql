-- Phase 4: runnable code. Everything here extends the existing content and attempt structures
-- rather than introducing a parallel set of tables, because the whole point is that a code
-- execution is an event inside a training session, not a separate kind of session.

-- ---------------------------------------------------------------------------
-- 1. What the runner calls.
-- ---------------------------------------------------------------------------
-- `entrypoint` is the function name in the learner's file, e.g. "two_sum". It is the one piece of
-- per-problem knowledge a runner needs, and it lives beside the content rather than in the client,
-- so the client cannot decide which function to call and thereby choose its own argument shape.
--
-- Nullable: only the problems whose test cases carry structured `call` arguments are runnable. A
-- problem without an entrypoint is simply not offered an editor, and saying so with a NULL is
-- clearer than saying so with a magic default name.
alter table problems
    add column entrypoint varchar(80);

-- ---------------------------------------------------------------------------
-- 2. Machine-readable test arguments.
-- ---------------------------------------------------------------------------
-- `input_data` / `expected_output` are written for a human to read: "nums = [2,7,11,15], target = 9".
-- That is not runnable and should never be parsed. `call` holds the positional arguments as JSON
-- so the harness can call the entrypoint directly, and `expected_json` holds the expected return
-- value so comparison is structural rather than string equality.
--
-- Both are nullable for the same reason `entrypoint` is: the display columns stay authoritative for
-- what the learner is shown, and a problem has to be opted in to being runnable.
--
-- The CHECKs matter. Without them a partially authored case would look runnable and fail as a
-- wrong answer, which would teach the learner the wrong lesson about their own code.
alter table problem_test_cases
    add column call JSONB,
    add column expected_json JSONB;

alter table problem_test_cases
    add constraint problem_test_cases_call_present_together
        check ((call is null and expected_json is null) or (call is not null and expected_json is not null));

alter table problem_test_cases
    add constraint problem_test_cases_call_is_args check (call is null or jsonb_typeof(call) = 'array');

-- ---------------------------------------------------------------------------
-- 3. Context-aware hints.
-- ---------------------------------------------------------------------------
-- A hint that fits the reasoning stage is noise during the coding stage, and one that fits a wrong
-- answer is noise before there is a wrong answer. `stage` and `trigger` select the right rung
-- without changing what `level` means, so the Phase 3 counter (`hintsUsed = max(level)`) and the
-- NO_HINTS award keep working exactly as before.
--
-- Existing rows take 'ANY', which is the pre-Phase-4 behaviour: one ladder, always applicable.
alter table problem_hints
    add column stage VARCHAR(20) NOT NULL DEFAULT 'ANY' CHECK (stage IN ('ANY', 'BREAKDOWN', 'REASONING', 'CODING')),
    add column trigger VARCHAR(24) NOT NULL DEFAULT 'ANY'
        CHECK (trigger IN ('ANY', 'WRONG_ANSWER', 'RUNTIME_ERROR', 'SYNTAX_ERROR', 'TIME_LIMIT_EXCEEDED'));

-- The old (problem_id, level) key assumed one rung per level. A level now has one row per
-- stage/trigger pair, so the key has to widen to match.
alter table problem_hints
    drop constraint problem_hints_problem_id_level_key;

alter table problem_hints
    add constraint uq_problem_hints_rung unique (problem_id, level, stage, trigger);

-- ---------------------------------------------------------------------------
-- 4. What the coding stage concluded about the attempt.
-- ---------------------------------------------------------------------------
-- These are server-derived summaries of the execution log below, denormalised onto the attempt so
-- completing a session reads one row rather than a correlated aggregate. As with `xp_awarded`,
-- nothing sums them and nothing trusts them from the client: the browser cannot write any of these
-- columns.
alter table user_problem_attempts
    add column code_outcome VARCHAR(24) CHECK (
        code_outcome IS NULL OR code_outcome IN
            ('ACCEPTED', 'WRONG_ANSWER', 'RUNTIME_ERROR', 'TIME_LIMIT_EXCEEDED',
             'MEMORY_LIMIT_EXCEEDED', 'SYNTAX_ERROR', 'INTERNAL_ERROR')),
    -- True only when the backend ran the code against the hidden evaluation set and it passed.
    add column code_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    -- How many times the learner pressed Run or Submit. Cheap to keep now, and the answer to
    -- "how many attempts did they need" cannot be reconstructed later without it.
    add column code_executions INT NOT NULL DEFAULT 0,
    -- The last escape hatch. Kept separate from hints because it means something categorically
    -- different: a rung is a nudge, the reference implementation is the answer.
    add column solution_revealed BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------------------------------------------------------------------------
-- 5. The execution log.
-- ---------------------------------------------------------------------------
-- This is the one genuinely new table in this migration, and it is not redundant with the attempt.
-- An attempt holds the summary; this holds the ordered events that produced it. The questions the
-- product needs to answer later — how many times did they run code, what failed, did they solve it
-- before or after using hints — are all "what happened, in order", which a single summary column
-- cannot represent. Overwriting one row per attempt would lose exactly that history.
create table user_code_executions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    attempt_id     UUID NOT NULL REFERENCES user_problem_attempts (id) ON DELETE CASCADE,
    problem_id     UUID NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    -- RUN is the learner testing against the visible examples. SUBMIT is the evaluation set.
    -- The distinction is the whole point: a Run can never make a problem solved.
    kind           VARCHAR(10) NOT NULL CHECK (kind IN ('RUN', 'SUBMIT')),
    language       VARCHAR(20) NOT NULL CHECK (language IN ('JAVA', 'PYTHON')),
    outcome        VARCHAR(24) NOT NULL CHECK (outcome IN
        ('ACCEPTED', 'WRONG_ANSWER', 'RUNTIME_ERROR', 'TIME_LIMIT_EXCEEDED',
         'MEMORY_LIMIT_EXCEEDED', 'SYNTAX_ERROR', 'INTERNAL_ERROR')),
    cases_total    INT  NOT NULL,
    cases_passed   INT  NOT NULL,
    -- Bounded at write time by the service. Stored so the mistake journal can classify a failure
    -- without re-reading the code that caused it.
    duration_ms    INT,
    -- The learner's own source, so a failure can be looked at again later. It is their code, not
    -- infrastructure, and it is already stored on the attempt; this is the per-attempt history.
    source         TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT code_executions_counts_sane CHECK (cases_passed BETWEEN 0 AND cases_total)
);

-- Ordered history for one problem, which is how the mistake journal and any future analytics read it.
create index idx_code_executions_user_problem on user_code_executions (user_id, problem_id, created_at desc);
-- The most recent outcome for an attempt, which is what the coding stage needs on every keystroke
-- cycle of hint selection.
create index idx_code_executions_attempt_recent on user_code_executions (attempt_id, created_at desc);