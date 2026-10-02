-- Account and progress domain. Replaces the browser-only progress of Phase 2.
--
-- Shape decisions that the rest of the game layer depends on:
--
--  * users rows are created anonymously, so an anonymous row carries no identity at all and a
--    registered row always carries an email and a hash. The check constraint makes the
--    half-created states unrepresentable rather than merely discouraged.
--  * user_sessions stores a hash of the cookie value, never the value. A database read must not
--    yield a usable session.
--  * XP lives in an append-only ledger with an idempotency key, never in a mutable counter. A
--    counter drifts under retries, two open tabs and imports; the unique index makes a repeat
--    award unrepresentable instead of merely unlikely.
--  * total XP is a sum over that ledger, so nothing has to be kept in sync.

create table users (
    id            uuid primary key default gen_random_uuid(),
    username      varchar(40)  unique,
    email         varchar(255) unique,
    password_hash varchar(255),
    -- IANA zone, read once from the browser. Streak days are computed server-side in this zone
    -- so that a client cannot pick its own day boundary and silently rewrite its history.
    timezone      varchar(64)  not null default 'UTC',
    anonymous     boolean      not null default true,
    -- Set when an anonymous row is claimed by a real account. Progress never moves: the same
    -- row becomes the account.
    claimed_at    timestamptz,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),

    constraint users_shape check (
        (anonymous     and email is null and password_hash is null)
        or (not anonymous and email is not null and password_hash is not null)
    )
);

create table user_sessions (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references users (id) on delete cascade,
    -- SHA-256 of the cookie value. Never the value itself.
    token_hash   varchar(64) not null unique,
    created_at   timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    expires_at   timestamptz not null
);

create index idx_user_sessions_user on user_sessions (user_id);

-- One row per training session. Append-only history: repeating a problem adds a row rather
-- than overwriting, which is what the mistake journal and spaced repetition both need.
create table user_problem_attempts (
    id                     uuid primary key default gen_random_uuid(),
    user_id                uuid not null references users (id) on delete cascade,
    problem_id             uuid not null references problems (id) on delete cascade,
    -- Denormalised from problems.primary_pattern_id so mastery can aggregate without a join.
    pattern_id             uuid not null references patterns (id),
    mode                   varchar(20) not null,
    status                 varchar(20) not null,
    -- LIVE is a real session. IMPORT is history that predates the account and was recovered
    -- from the browser; it is marked so that missing signals stay explainable.
    source                 varchar(20) not null default 'LIVE',
    -- From the Build phase. Null while the learner has not written code.
    language               varchar(20),
    code                   text,
    hints_used             int     not null default 0,
    pattern_guess          varchar(80),
    -- Authored by the server from the guess and problems.primary_pattern_id, never by the client.
    pattern_correct        boolean not null default false,
    -- [{stepOrder, chosenIndex, correct}]. `correct` is recomputed server-side from the
    -- animation step's answerIndex, because the answer already ships to the browser.
    predictions            jsonb   not null default '[]'::jsonb,
    complexity_time_guess  varchar(20),
    complexity_space_guess varchar(20),
    complexity_correct     boolean not null default false,
    breakdown_correct      boolean not null default false,
    duration_ms            bigint,
    xp_awarded             int     not null default 0,
    completed_at           timestamptz,
    created_at             timestamptz not null default now(),

    constraint attempts_mode   check (mode   in ('STANDARD', 'SPEEDRUN', 'BOSS')),
    constraint attempts_status check (status in ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    constraint attempts_source check (source in ('LIVE', 'IMPORT')),
    constraint attempts_hints  check (hints_used between 0 and 5),
    constraint attempts_duration check (duration_ms is null or duration_ms >= 0)
);

create index idx_attempts_user_recent on user_problem_attempts (user_id, completed_at desc);
create index idx_attempts_user_problem on user_problem_attempts (user_id, problem_id);

-- Two tabs must not both believe they own the live session for a problem.
create unique index uq_attempts_live
    on user_problem_attempts (user_id, problem_id)
    where status = 'IN_PROGRESS';

-- Append-only. This table is the only source of XP, and `xp_awarded` on the attempt is a
-- denormalised convenience for reading one attempt's result, never a running total.
create table user_xp_awards (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references users (id) on delete cascade,
    attempt_id uuid references user_problem_attempts (id) on delete set null,
    problem_id uuid references problems (id) on delete set null,
    reason     varchar(40) not null,
    xp         int  not null,
    -- Idempotency key. NULL only for awards that are guarded by a monotonic comparison instead
    -- (SPEEDRUN_PB), which a key cannot express.
    award_key  varchar(160),
    awarded_at timestamptz not null default now(),

    constraint xp_awards_reason check (reason in (
        'PATTERN_IDENTIFIED', 'PREDICTION_CORRECT', 'PROBLEM_COMPLETED', 'NO_HINTS',
        'CORRECT_COMPLEXITY', 'PROBLEM_BREAKDOWN', 'SPEEDRUN_PB', 'BOSS_DEFEATED',
        'REVIEW_COMPLETED', 'DAILY_QUEST', 'COMEBACK', 'COMBO_BONUS')),
    constraint xp_awards_positive check (xp > 0)
);

-- The anti-abuse keystone. Solving one problem a hundred times pays out once, because the
-- second insert is impossible rather than merely skipped.
create unique index uq_xp_award_once
    on user_xp_awards (user_id, award_key)
    where award_key is not null;

create index idx_xp_awards_user on user_xp_awards (user_id, awarded_at desc);

-- Raw per-pattern axes plus a derived overall. Each axis is nullable and NULL means "not
-- measured yet", which is different from zero: a learner who has never been reviewed has no
-- retention score, and one who has a bad one has a zero. Overall is a weighted average over
-- the axes that exist (README section 37), so an axis can start being measured later without a
-- migration or a backfill.
create table user_pattern_mastery (
    id                uuid primary key default gen_random_uuid(),
    user_id           uuid not null references users (id) on delete cascade,
    pattern_id        uuid not null references patterns (id) on delete cascade,
    recognition_score numeric(5,2),
    correctness_score numeric(5,2),
    speed_score       numeric(5,2),
    -- No signal until the explanation builder exists (README section 81/118).
    explanation_score numeric(5,2),
    -- No signal until the review queue has history.
    retention_score   numeric(5,2),
    attempts_count    int  not null default 0,
    overall_score     numeric(5,2),
    updated_at        timestamptz not null default now(),

    unique (user_id, pattern_id)
);

-- One row per learner-local day. The streak is derived from consecutive goal_met days rather
-- than stored, so it cannot drift out of step with the rows that justify it.
create table user_daily_progress (
    id                  uuid primary key default gen_random_uuid(),
    user_id             uuid not null references users (id) on delete cascade,
    day                 date  not null,
    xp_earned           int not null default 0,
    problems_completed  int not null default 0,
    patterns_identified int not null default 0,
    mistakes_reviewed   int not null default 0,
    speedruns_completed int not null default 0,
    active_minutes      int not null default 0,
    goal_met            boolean not null default false,
    quest_completed     boolean not null default false,
    -- When the row was written, which is not the same as the day it describes: an imported
    -- day is written now but happened weeks ago.
    created_at          timestamptz not null default now(),

    unique (user_id, day)
);

create index idx_daily_progress_recent on user_daily_progress (user_id, day desc);

-- Personal best per problem. A speedrun record is the minimum duration among completed
-- speedruns that identified the pattern correctly: a fast wrong answer is not a record.
create table user_speedrun_records (
    id               uuid primary key default gen_random_uuid(),
    user_id          uuid not null references users (id) on delete cascade,
    problem_id       uuid not null references problems (id) on delete cascade,
    best_duration_ms bigint not null,
    best_attempt_id  uuid references user_problem_attempts (id) on delete set null,
    runs             int not null default 1,
    updated_at       timestamptz not null default now(),

    unique (user_id, problem_id),
    constraint speedruns_runs check (runs >= 1)
);

-- Spaced repetition (Leitner). `due_at` is advanced when a review happens rather than by a
-- scheduled job, which keeps the scheduler out of the application.
create table user_mistakes (
    id               uuid primary key default gen_random_uuid(),
    user_id          uuid not null references users (id) on delete cascade,
    problem_id       uuid not null references problems (id) on delete cascade,
    attempt_id       uuid references user_problem_attempts (id) on delete set null,
    category         varchar(40) not null,
    description      text not null,
    lesson           text not null,
    resolved         boolean      not null default false,
    review_count     int          not null default 0,
    interval_days    int          not null default 1,
    last_reviewed_at timestamptz,
    due_at           timestamptz  not null default now(),
    created_at       timestamptz  not null default now(),

    constraint mistakes_category check (category in
        ('PATTERN', 'LOGIC', 'TESTS', 'COMPLEXITY', 'EXPLANATION')),
    constraint mistakes_interval check (interval_days >= 1)
);

create index idx_mistakes_due
    on user_mistakes (user_id, due_at)
    where not resolved;

-- Recovery of browser history that predates the account. batch_key makes a resubmission of the
-- same blob a no-op, so a dropped response or a nervous learner cannot double-credit.
create table progress_imports (
    id               uuid primary key default gen_random_uuid(),
    user_id          uuid not null references users (id) on delete cascade,
    batch_key        varchar(64) not null,
    records_seen     int not null,
    records_imported int not null,
    records_skipped  int not null,
    created_at       timestamptz not null default now(),

    unique (user_id, batch_key)
);