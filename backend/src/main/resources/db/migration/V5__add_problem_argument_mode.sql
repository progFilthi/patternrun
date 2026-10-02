-- Phase 4, second pass: how a runner turns stored test arguments into a call.
--
-- Two Sum and eighteen others take their arguments literally: a list of ints, a string, a list of
-- lists. Two of them take a tree, and the content describes a tree the way everybody writes one
-- down --- level order, with nulls for absent children --- which is not a Python object.
--
-- The conversion belongs to the runner, not to the learner. A learner asked to write
--
--     def max_depth(root): ...
--
-- and handed a list would either have to serialise the tree themselves, which is boilerplate that
-- hides the algorithm they are being asked about, or conclude their solution is broken. Both are
-- worse than the three lines of configuration that fix it.
--
-- One column on problems rather than a flag per case, because the calling convention is a property
-- of the signature, and every case of a given problem shares it. PLAIN is the default, so the
-- eighteen problems that need nothing are unaffected.
alter table problems
    add column argument_mode varchar(20) not null default 'PLAIN'
        check (argument_mode in ('PLAIN', 'TREE'));

comment on column problems.argument_mode is
    'How the runner turns a stored test case''s arguments into a call: PLAIN passes them as stored, TREE builds a binary tree from a level-order array.';