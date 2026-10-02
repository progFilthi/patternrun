-- Problem breakdown: the step that teaches a learner to read what is being asked.
--
-- Stored as JSONB on problems for the same reason constraints, pseudocode and common mistakes
-- are: it is list-valued content authored in the seed files and owned by Flyway only as a column.
-- The answer key lives inside it, which is why the read endpoint projects it away rather than
-- serving the document.
--
-- breakdown_answers on the attempt records what the learner chose, in the same shape, so the
-- completion can judge the stored answers instead of believing a flag sent by the browser.

alter table problems
    add column breakdown jsonb not null default '[]'::jsonb;

-- [{key, chosenIndex}]. Empty until the breakdown step has been answered.
alter table user_problem_attempts
    add column breakdown_answers jsonb not null default '[]'::jsonb;