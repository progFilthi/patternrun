-- PatternRun content domain (Phase 1).
-- User/progress domain (users, attempts, pattern_mastery, mistakes) is intentionally
-- deferred: for the MVP progress lives in the browser, see README sections 73/74.

CREATE TABLE patterns (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug             VARCHAR(80)  NOT NULL UNIQUE,
    name             VARCHAR(120) NOT NULL,
    summary          TEXT         NOT NULL,
    signal           TEXT         NOT NULL,
    mental_model     TEXT         NOT NULL,
    recognition_rules JSONB       NOT NULL DEFAULT '[]'::jsonb,
    template         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    invariant        TEXT         NOT NULL,
    difficulty_order INT          NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE problems (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id           INT          NOT NULL UNIQUE,
    slug                  VARCHAR(120) NOT NULL UNIQUE,
    title                 VARCHAR(200) NOT NULL,
    difficulty            VARCHAR(10)  NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    training_difficulty   VARCHAR(20)  NOT NULL CHECK (training_difficulty IN
                            ('RECOGNITION', 'GUIDED', 'INDEPENDENT', 'SPEEDRUN', 'BOSS')),
    statement             TEXT         NOT NULL,
    constraints           JSONB        NOT NULL DEFAULT '[]'::jsonb,
    why_this_pattern      TEXT         NOT NULL,
    brute_force           TEXT         NOT NULL,
    invariant             TEXT         NOT NULL,
    pseudocode            JSONB        NOT NULL DEFAULT '[]'::jsonb,
    common_mistakes       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    interview_explanation TEXT         NOT NULL,
    time_complexity       VARCHAR(20)  NOT NULL,
    space_complexity      VARCHAR(20)  NOT NULL,
    primary_pattern_id    UUID         NOT NULL REFERENCES patterns (id),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_problems_primary_pattern ON problems (primary_pattern_id);

CREATE TABLE problem_secondary_patterns (
    problem_id UUID NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    pattern_id UUID NOT NULL REFERENCES patterns (id) ON DELETE CASCADE,
    PRIMARY KEY (problem_id, pattern_id)
);

CREATE TABLE problem_examples (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id  UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    ordinal     INT         NOT NULL,
    input       TEXT        NOT NULL,
    output      TEXT        NOT NULL,
    explanation TEXT        NOT NULL,
    UNIQUE (problem_id, ordinal)
);

CREATE INDEX idx_problem_examples_problem ON problem_examples (problem_id);

CREATE TABLE problem_hints (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    level      INT  NOT NULL CHECK (level BETWEEN 1 AND 5),
    content    TEXT NOT NULL,
    UNIQUE (problem_id, level)
);

CREATE INDEX idx_problem_hints_problem ON problem_hints (problem_id);

CREATE TABLE problem_animation_steps (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id  UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    step_order  INT         NOT NULL,
    step_type   VARCHAR(40) NOT NULL CHECK (step_type IN
                   ('ARRAY', 'POINTER', 'WINDOW', 'HASH_MAP', 'STACK', 'HEAP', 'TREE', 'GRAPH', 'GRID',
                    'INTERVAL', 'PREFIX_SUM', 'DP_TABLE', 'CODE', 'TEXT', 'QUESTION', 'SUCCESS', 'FAILURE')),
    title       VARCHAR(160) NOT NULL,
    description TEXT        NOT NULL,
    -- Text alternative for every animation step (README section 56).
    text        TEXT        NOT NULL,
    payload     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (problem_id, step_order)
);

CREATE INDEX idx_problem_animation_steps_problem ON problem_animation_steps (problem_id);

CREATE TABLE problem_test_cases (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id     UUID    NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    ordinal        INT     NOT NULL,
    label          VARCHAR(120) NOT NULL,
    input_data     TEXT    NOT NULL,
    expected_output TEXT   NOT NULL,
    is_hidden      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (problem_id, ordinal)
);

CREATE INDEX idx_problem_test_cases_problem ON problem_test_cases (problem_id);

CREATE TABLE problem_solutions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    language   VARCHAR(20) NOT NULL CHECK (language IN ('JAVA', 'PYTHON')),
    code       TEXT        NOT NULL,
    UNIQUE (problem_id, language)
);