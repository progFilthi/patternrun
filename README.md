# PatternRun --- Gamified LeetCode Interview Prep

> **Master patterns. Understand the problem. Build the solution. Explain
> it. Speedrun it.**

PatternRun is a full-stack interview-preparation web app built around
one idea:

**Stop memorizing hundreds of LeetCode solutions. Learn the small set of
patterns that repeatedly appear, then train your brain to recognize and
execute those patterns quickly.**

The product is intentionally designed like a game rather than a
traditional problem list.

You do not simply open:

> `Two Sum → read problem → write code → submit`

Instead, you enter a short training loop:

``` text
SPOT THE PATTERN
      ↓
UNDERSTAND THE PROBLEM
      ↓
GET A SMALL ANIMATED HINT
      ↓
PREDICT THE NEXT MOVE
      ↓
BUILD THE SOLUTION
      ↓
RUN TESTS
      ↓
EXPLAIN YOUR APPROACH
      ↓
SPEEDRUN
      ↓
EARN XP + MASTER THE PATTERN
```

The goal is not to make LeetCode easier.

The goal is to make **your thinking process faster, clearer, and more
automatic.**

------------------------------------------------------------------------

# 1. Product Vision

## The problem

Traditional DSA preparation creates several problems:

-   hundreds of problems feel unrelated
-   users memorize solutions instead of recognizing patterns
-   hints often reveal too much
-   explanations are too long
-   progress is usually measured only by solved count
-   users repeat easy problems because they feel productive
-   mistakes disappear instead of becoming training data
-   there is no obvious connection between a problem and the mental
    pattern behind it

PatternRun changes the unit of learning.

Instead of:

``` text
Problem → Solution
```

we train:

``` text
Pattern → Mental Model → Problem → Execution → Explanation → Speed
```

------------------------------------------------------------------------

# 2. The Core Philosophy

## Rule 1 --- Patterns first

The application is organized around a small set of reusable patterns.

The reference cheat sheet provides the initial pattern system:

1.  Complexity
2.  Sliding Window
3.  Two Pointers
4.  Binary Search
5.  Hashing + Prefix
6.  Monotonic Stack
7.  Heap / Top K
8.  Intervals
9.  BFS / DFS
10. Trees
11. DP Basics
12. Interview Routine
13. Golden Rules
14. Final Takeaway

The application should treat **Pattern Picker** as the central learning
mechanic.

The user should gradually develop the reflex:

``` text
"I see this problem."
        ↓
"What is changing?"
        ↓
"What is the constraint?"
        ↓
"What pattern fits?"
        ↓
"Which template do I already know?"
```

------------------------------------------------------------------------

# 3. Product Goal

The product should answer one question:

> **Can I look at a new interview problem and quickly figure out how to
> attack it?**

Not:

> "How many LeetCode problems have I clicked?"

The important metrics are therefore:

-   pattern recognition accuracy
-   time to identify a pattern
-   hint dependency
-   solution completion time
-   explanation quality
-   repeated mistake frequency
-   retention after review
-   speedrun improvement
-   pattern mastery

------------------------------------------------------------------------

# 4. The Game

PatternRun is structured like a small RPG.

## Player

The user is the player.

``` text
Name
Level
XP
Streak
Pattern Mastery
Problems Solved
Bosses Defeated
Current Combo
```

Example:

``` text
┌──────────────────────────────────────────┐
│  EMMA                                    │
│                                          │
│  LEVEL 7                                  │
│  ████████████████░░░░  1,640 / 2,000 XP │
│                                          │
│  🔥 6 day streak                         │
│  ⚡ 4 problem combo                       │
└──────────────────────────────────────────┘
```

Do not make the UI childish.

The game mechanics should be obvious, but the visual design should
remain professional.

Think:

-   Apple-like spacing
-   clean typography
-   subtle motion
-   strong hierarchy
-   minimal cards
-   restrained color
-   clear progress
-   no noisy gradients
-   no excessive gamification graphics

------------------------------------------------------------------------

# 5. The Main Game Loop

Every problem follows the same mental sequence.

## Stage 1 --- Scout

The user reads only:

-   title
-   difficulty
-   constraints
-   examples

Then the app asks:

> **What pattern do you think this is?**

The user chooses one.

Example:

``` text
Which pattern would you try first?

○ Hashing
○ Sliding Window
○ Two Pointers
○ Binary Search
○ DP
```

No code yet.

This is important.

The app trains recognition before implementation.

------------------------------------------------------------------------

# 6. Stage 2 --- Decode

Break the problem into tiny statements.

Example:

``` text
Given:
nums = [2, 7, 11, 15]
target = 9
```

Instead of immediately showing:

``` java
Map<Integer, Integer> map = new HashMap<>();
```

show:

``` text
WE NEED
┌─────────────────────────────┐
│ Find TWO numbers            │
│ whose sum equals TARGET     │
└─────────────────────────────┘

Target = 9

2 + ? = 9
        ↓
       7
```

Then:

``` text
Question:

"What information would make finding 7 fast?"
```

The user answers:

``` text
Have I seen 7 before?
```

That leads naturally to hashing.

------------------------------------------------------------------------

# 7. Stage 3 --- Animated Hint Ladder

Hints must reveal thinking progressively.

Never dump the solution.

Use five levels.

## Hint 0 --- No help

``` text
Try it yourself.
```

## Hint 1 --- Direction

``` text
What would you need to know
while scanning the array?
```

## Hint 2 --- Pattern

``` text
Can you remember what you have
already seen?
```

## Hint 3 --- Structure

``` text
Use a data structure that gives
fast lookup.
```

## Hint 4 --- Pseudocode

``` text
for each number:
    calculate what is missing
    check if it was seen
    otherwise remember current number
```

## Hint 5 --- Implementation

Only after the user explicitly asks:

``` text
Show me the Java implementation.
```

This creates a **hint dependency score**.

Example:

``` text
Hints used: 2 / 5
Hint efficiency: 80%
```

------------------------------------------------------------------------

# 8. Stage 4 --- Predict the Move

This is one of the most important mechanics.

Instead of only typing code, occasionally pause the animation and ask:

> **What happens next?**

Example:

``` text
nums = [2, 7, 11, 15]
target = 9

Current:
2

What should we calculate?

A. target + 2
B. target - 2
C. target / 2
D. 2 × target
```

Correct:

``` text
target - current
9 - 2 = 7
```

The app then animates:

``` text
[2] [7] [11] [15]
 ↑
current

target - current
      ↓
      7

Have we seen 7?
NO

Remember 2.

Next...
```

This is much closer to actual learning than reading an explanation.

------------------------------------------------------------------------

# 9. Stage 5 --- Build

Now the user writes the solution.

The editor should provide:

-   syntax highlighting
-   language selector
-   run button
-   test cases
-   expected output
-   actual output
-   runtime
-   memory
-   basic complexity analysis

Initial languages:

``` text
Java
Python
```

Java should be first-class because the application is also useful as a
backend/software-engineering interview tool.

Python can be added immediately or shortly after because it is useful
for DSA speed.

------------------------------------------------------------------------

# 10. Stage 6 --- Test

Tests are displayed like checkpoints.

``` text
CHECKPOINTS

✓ Example 1
✓ Example 2
✓ Empty input
✓ Single element
✗ Duplicate values
✓ Large input
```

Do not simply say:

``` text
Wrong Answer
```

Instead:

``` text
Your solution failed checkpoint 4.

Input:
[3, 3]

Expected:
true

Your output:
false

Pattern lesson:
You may be overwriting information
before checking it.
```

The failure becomes a lesson.

------------------------------------------------------------------------

# 11. Stage 7 --- Complexity

The user must provide:

``` text
Time: O(?)
Space: O(?)
```

Before showing the official answer.

Example:

``` text
TIME

○ O(1)
○ O(log n)
● O(n)
○ O(n²)
```

Then:

``` text
SPACE

○ O(1)
● O(n)
○ O(n²)
```

This reinforces the complexity section of the reference cheat sheet.

------------------------------------------------------------------------

# 12. Stage 8 --- Explain

Interviewers care about whether the candidate can explain their
solution.

PatternRun therefore requires a short explanation.

Prompt:

> Explain your solution in 3 sentences as if you were speaking to an
> interviewer.

The user should cover:

1.  pattern
2.  mechanism
3.  complexity

Example:

``` text
"I use a HashMap to remember numbers I have already seen.
For each number, I calculate the complement needed to reach
the target and check whether that complement exists.
This gives O(n) time and O(n) space."
```

The app can score structure without pretending to be an interviewer.

------------------------------------------------------------------------

# 13. Stage 9 --- Speedrun

Once the problem is understood, enter:

``` text
SPEEDRUN MODE
```

The timer starts.

The user must:

``` text
1. Identify pattern
2. Write approach
3. Code
4. Pass tests
5. Give complexity
```

The goal is not blind speed.

The goal is:

``` text
FAST + CORRECT + EXPLAINABLE
```

------------------------------------------------------------------------

# 14. Stage 10 --- Mastery

After completion:

``` text
PATTERN MASTERED +12 XP

Hashing
██████████████░░░░░░ 72%

New record:
Previous: 08:42
Current: 06:31

Hint dependency:
Previous: 3 hints
Current: 1 hint

Accuracy:
100%
```

Then recommend the next problem based on weakness.

------------------------------------------------------------------------

# 15. Pattern System

The initial pattern system is based on the supplied reference cheat
sheet.

------------------------------------------------------------------------

## Pattern 01 --- Complexity

### Mental model

Ask:

``` text
How many times can this operation happen?
```

Common categories:

``` text
O(1)       constant
O(log n)   logarithmic
O(n)       linear
O(n log n) sorting-ish
O(n²)      nested scanning
```

### Game mechanic

**Complexity Boss**

Show a tiny code fragment:

``` java
for (int i = 0; i < n; i++) {
    System.out.println(i);
}
```

Ask:

``` text
Predict complexity.
```

Then animate the number of operations.

------------------------------------------------------------------------

# 16. Pattern 02 --- Sliding Window

Reference problems:

``` text
3
76
424
904
```

### Recognition clues

Use when the problem involves:

-   substring
-   subarray
-   contiguous region
-   longest
-   shortest
-   maximum
-   minimum
-   valid window
-   frequency/count state

### Mental model

``` text
RIGHT → expand
LEFT  → shrink
```

Visual:

``` text
[1][2][3][4][5][6][7][8]
       └───────┘
         WINDOW

L → move right
R → move right

When invalid:
L → move right until valid
```

### Game

**Window Runner**

The player controls `L` and `R`.

Each action moves the window.

The app asks:

``` text
Window valid?

YES → expand
NO  → shrink
```

The player gets XP for choosing the correct pointer action.

------------------------------------------------------------------------

# 17. Pattern 03 --- Two Pointers

Reference problems:

``` text
11
15
167
42
```

### Recognition clues

-   sorted array
-   pair
-   palindrome
-   opposite ends
-   deduplication
-   in-place scanning

### Mental model

``` text
L →          ← R

[1][2][3][4][5][6][7][8]
 ↑                       ↑
 L                       R
```

Example:

``` text
sum < target
→ move L

sum > target
→ move R

sum == target
→ found
```

### Game

**Pointer Duel**

The user predicts which pointer moves.

``` text
L = 2
R = 8

sum = 10
target = 12

What moves?

→ L
→ R
→ both
```

------------------------------------------------------------------------

# 18. Pattern 04 --- Binary Search

Reference problems:

``` text
33
153
875
1011
```

### Recognition clues

-   sorted data
-   rotated sorted data
-   monotonic condition
-   minimum feasible answer
-   maximum feasible answer
-   "can we do it?"
-   search space can be cut in half

### Mental model

``` text
LOW ─────────────── HIGH
          ↓
         MID

Discard half.

LOW ─────── MID
             X
             X
             X
```

### Game

**Search Space Assassin**

Every correct decision eliminates half the search space.

Display:

``` text
Search space:

1 2 3 4 5 6 7 8 9

MID = 5

Target > 5

Eliminated:
1 2 3 4 5

Remaining:
6 7 8 9
```

------------------------------------------------------------------------

# 19. Pattern 05 --- Hashing + Prefix

Reference problems:

``` text
1
49
560
128
```

### Hashing mental model

``` text
I need fast lookup.

"What have I already seen?"
```

### Prefix mental model

``` text
a:  1  2  3  4  5
p:  1  3  6 10 15
```

Then:

``` text
sum(i..j) = prefix[j] - prefix[i]
```

### Game

**Memory Grid**

Animate the hash map:

``` text
seen

2 → index 0
7 → index 1
11 → index 2
```

For prefix sums:

``` text
prefix:
0
↓
1
↓
3
↓
6
↓
10
```

The user predicts the lookup.

------------------------------------------------------------------------

# 20. Pattern 06 --- Monotonic Stack

Reference problems:

``` text
739
503
84
901
```

### Recognition clues

-   next greater
-   next smaller
-   previous greater
-   previous smaller
-   histogram boundaries
-   elements waiting for an answer

### Mental model

A stack contains elements that are **waiting**.

Example:

``` text
[2, 1, 5]

2 waits
1 waits

5 arrives

5 answers both.
```

### Game

**Stack Tower**

Cards enter one at a time.

The player chooses:

``` text
PUSH
or
POP
```

The animation makes the monotonic property visible.

------------------------------------------------------------------------

# 21. Pattern 07 --- Heap / Top K

Reference problems:

``` text
215
347
973
703
```

### Recognition clues

-   top K
-   Kth largest/smallest
-   closest
-   streaming minimum/maximum
-   repeatedly remove min/max

### Mental model

``` text
Priority Queue

        10
       /  \
      7    8
     / \
    3   5
```

Do not teach the user to fully sort when a heap is sufficient.

### Game

**Top-K Arena**

The player inserts values into a heap and watches the heap maintain the
important elements.

------------------------------------------------------------------------

# 22. Pattern 08 --- Intervals

Reference problems:

``` text
56
435
57
252
```

### Recognition clues

-   meetings
-   schedules
-   overlap
-   merge
-   insert
-   ranges
-   start/end

### Mental model

Usually:

``` text
SORT FIRST
      ↓
SCAN
      ↓
OVERLAP?
      ↓
MERGE
```

### Game

**Calendar Collision**

Intervals appear on a timeline.

The player drags or selects intervals.

The app asks:

``` text
Overlap?

YES → merge
NO  → keep separate
```

------------------------------------------------------------------------

# 23. Pattern 09 --- BFS / DFS

Reference problems:

``` text
200
994
102
133
```

### Recognition clues

BFS:

``` text
levels
shortest path
unweighted graph
minimum number of steps
```

DFS:

``` text
exploration
components
recursive traversal
backtracking-like exploration
```

### Game

**Maze Mode**

BFS:

``` text
START
 ↓
LEVEL 1
 ↓
LEVEL 2
 ↓
LEVEL 3
```

DFS:

``` text
START
 ↓
GO DEEP
 ↓
DEAD END
 ↓
BACKTRACK
```

The user controls the traversal.

------------------------------------------------------------------------

# 24. Pattern 10 --- Trees

Reference problems:

``` text
104
236
543
124
```

### Mental model

Think recursively:

``` text
What is the answer for this node
if I already knew the answers
for its children?
```

### Game

**Tree Climb**

The app highlights:

``` text
current node
left result
right result
current result
```

Example:

``` text
        1
       / \
      2   3

left = ?
right = ?

current = combine(left, right)
```

------------------------------------------------------------------------

# 25. Pattern 11 --- DP Basics

Reference problems:

``` text
70
198
322
300
```

### Mental model

DP is:

``` text
STATE
+
TRANSITION
+
BASE CASE
```

Ask:

``` text
What does dp[i] mean?
```

Then:

``` text
How does dp[i] depend on previous states?
```

### Game

**State Builder**

The user fills a table:

``` text
i:    0  1  2  3  4

dp:   ?  ?  ?  ?  ?
```

Then the app asks:

``` text
What is dp[0]?
What is dp[1]?
What does dp[i] depend on?
```

------------------------------------------------------------------------

# 26. Pattern Picker

The pattern picker is the heart of the product.

Use the reference mapping:

  Signal                               First pattern to investigate
  ------------------------------------ ------------------------------
  contiguous substring/subarray        Sliding Window
  sorted array, scan from both ends    Two Pointers
  sorted / monotonic search space      Binary Search
  counts / complements / fast lookup   Hashing
  prefix range-sum questions           Prefix Sum
  next greater/smaller                 Monotonic Stack
  top K / repeated min/max             Heap
  overlap / scheduling                 Intervals
  levels / shortest unweighted path    BFS
  exploration / components             DFS
  recursive parent-child structure     Trees
  repeated subproblems                 DP

The app should teach users to identify the **signal**, not memorize a
problem number.

------------------------------------------------------------------------

# 27. Problem Anatomy

Every problem in the database should have structured metadata.

``` text
Problem
├── title
├── slug
├── externalId
├── difficulty
├── statement
├── examples
├── constraints
├── patterns
├── primaryPattern
├── secondaryPatterns
├── tags
├── hints
├── animationSteps
├── pseudocode
├── solutions
├── testCases
├── complexity
├── explanation
└── interviewNotes
```

------------------------------------------------------------------------

# 28. The Animated Problem Format

Do not store animations as hardcoded React components.

Store them as data.

Example:

``` json
{
  "type": "ARRAY_SCAN",
  "step": 1,
  "title": "Start scanning",
  "description": "Look at the current number.",
  "state": {
    "array": [2, 7, 11, 15],
    "currentIndex": 0
  }
}
```

Next:

``` json
{
  "type": "CALCULATE",
  "step": 2,
  "title": "Find the complement",
  "description": "What number would complete the target?",
  "state": {
    "target": 9,
    "current": 2,
    "complement": 7
  }
}
```

This allows the frontend to render generic animations.

------------------------------------------------------------------------

# 29. Animation Engine

Create a reusable animation engine.

``` text
Animation
    ↓
Step[]
    ↓
Renderer
    ↓
User interaction
    ↓
Next step
```

Supported step types:

``` text
ARRAY
POINTER
WINDOW
HASH_MAP
STACK
HEAP
TREE
GRAPH
GRID
INTERVAL
PREFIX_SUM
DP_TABLE
CODE
TEXT
QUESTION
SUCCESS
FAILURE
```

Example:

``` ts
type AnimationStep =
  | ArrayStep
  | PointerStep
  | WindowStep
  | HashMapStep
  | StackStep
  | HeapStep
  | TreeStep
  | GraphStep
  | GridStep
  | IntervalStep
  | DPTableStep
  | QuestionStep;
```

------------------------------------------------------------------------

# 30. Problem Difficulty

Use two different difficulty systems.

## LeetCode difficulty

``` text
Easy
Medium
Hard
```

## Training difficulty

``` text
1 — Recognition
2 — Guided
3 — Independent
4 — Speedrun
5 — Boss
```

A Medium problem can therefore be a Training Level 2 if the user is
learning the pattern.

------------------------------------------------------------------------

# 31. Boss Problems

Every pattern gets boss problems.

A boss should:

-   reveal no hints initially
-   require pattern selection
-   require complexity
-   require implementation
-   require explanation
-   have a timer
-   award bonus XP

Example:

``` text
SLIDING WINDOW BOSS

Problem:
Longest Repeating Character Replacement

Requirements:

[ ] Identify pattern
[ ] Explain why
[ ] Write pseudocode
[ ] Implement
[ ] Pass tests
[ ] Complexity
[ ] Interview explanation
```

------------------------------------------------------------------------

# 32. XP System

Keep XP simple.

Example:

``` text
Pattern identified       +10
Correct prediction       +5
Problem solved           +50
No hints                 +20
Correct complexity       +10
Good explanation         +20
Speedrun PB              +25
Boss defeated            +100
Review completed         +20
```

Avoid making XP dependent on arbitrary grinding.

The point is to reward useful behavior.

------------------------------------------------------------------------

# 33. Combo System

A combo increases when the user solves consecutive problems correctly.

``` text
1 → START
2 → 2x
3 → 3x
4 → 4x
5 → HOT STREAK
```

Break the combo if:

-   user intentionally skips
-   solution fails repeatedly
-   user reveals full solution immediately

Do not punish mistakes harshly.

Mistakes are valuable training data.

------------------------------------------------------------------------

# 34. Streak System

Daily goal:

``` text
10 minutes
or
1 problem
```

Not:

``` text
Solve 20 problems every day.
```

The goal should be sustainable.

Example:

``` text
MON ✓
TUE ✓
WED ✓
THU ✓
FRI ✓
SAT ○
SUN ○
```

------------------------------------------------------------------------

# 35. Daily Quest

Every day:

``` text
TODAY'S QUEST

[ ] Identify 3 patterns
[ ] Solve 1 problem
[ ] Review 1 mistake
[ ] Complete 1 speedrun
```

Completion:

``` text
+100 XP
```

------------------------------------------------------------------------

# 36. Pattern Mastery

Every pattern gets its own progression.

``` text
SLIDING WINDOW

Recognition
████████████████░░ 80%

Implementation
███████████░░░░░░░ 55%

Complexity
██████████████████ 90%

Speed
████████░░░░░░░░░░ 40%

Overall
████████████░░░░░░ 66%
```

Mastery should not simply equal number of solved problems.

------------------------------------------------------------------------

# 37. Mastery Formula

Initial simple formula:

``` text
mastery =
    recognition * 0.25
  + correctness * 0.30
  + explanation * 0.15
  + speed * 0.15
  + retention * 0.15
```

Do not overfit the formula in MVP.

Store the raw metrics so the formula can evolve later.

------------------------------------------------------------------------

# 38. Mistake Journal

Every failed attempt can become a lesson.

Categories:

``` text
PATTERN_MISSED
LOGIC_ERROR
EDGE_CASE
OFF_BY_ONE
COMPLEXITY
SYNTAX
DATA_STRUCTURE
STATE_DEFINITION
POINTER_MOVEMENT
BASE_CASE
```

Example:

``` text
Mistake:

Forgot to shrink the window.

Pattern:
Sliding Window

Problem:
Minimum Window Substring

Lesson:
When the window becomes valid,
try shrinking from the left.
```

The review system can later resurface this mistake.

------------------------------------------------------------------------

# 39. Smart Review

The app should ask:

> What should I practice next?

Simple initial algorithm:

``` text
1. Find patterns with low mastery.
2. Find problems previously failed.
3. Find problems with high hint dependency.
4. Find problems not reviewed recently.
5. Mix easy + medium.
6. Avoid repeating the exact same problem too often.
```

Later:

``` text
Spaced repetition
+
difficulty adjustment
+
error classification
```

------------------------------------------------------------------------

# 40. Review Queue

Example:

``` text
REVIEW NOW

1. Binary Search
   Missed 2 times
   Last reviewed: 5 days ago

2. Sliding Window
   High hint dependency
   Last reviewed: 3 days ago

3. DP
   Low recognition
   Last reviewed: 7 days ago
```

------------------------------------------------------------------------

# 41. Interview Mode

Separate from learning mode.

## Learning Mode

``` text
Hints
Animations
Guidance
Explanations
```

## Interview Mode

``` text
No hints
Timer
Minimal UI
Hidden solution
Tests only after submission
Complexity required
Final explanation required
```

The same problem can therefore be used twice.

------------------------------------------------------------------------

# 42. Blind 75 / NeetCode-Style Lists

Do not build a giant problem catalog initially.

Start with a curated set.

The first dataset should be approximately:

``` text
10 patterns
×
5–10 representative problems
```

Target:

``` text
60–100 problems
```

Then expand.

The goal is **representative coverage**, not maximum quantity.

------------------------------------------------------------------------

# 43. Initial Problem Seed

Start with the problems visible in the supplied cheat sheet.

### Sliding Window

``` text
3   Longest Substring Without Repeating Characters
76  Minimum Window Substring
424 Longest Repeating Character Replacement
904 Fruit Into Baskets
```

### Two Pointers

``` text
11  Container With Most Water
15  3Sum
167 Two Sum II
42  Trapping Rain Water
```

### Binary Search

``` text
33   Search in Rotated Sorted Array
153  Find Minimum in Rotated Sorted Array
875  Koko Eating Bananas
1011 Capacity To Ship Packages Within D Days
```

### Hashing / Prefix

``` text
1   Two Sum
49  Group Anagrams
560 Subarray Sum Equals K
128 Longest Consecutive Sequence
```

### Monotonic Stack

``` text
739 Daily Temperatures
503 Next Greater Element II
84  Largest Rectangle in Histogram
901 Online Stock Span
```

### Heap / Top K

``` text
215 Kth Largest Element in an Array
347 Top K Frequent Elements
973 K Closest Points to Origin
703 Kth Largest Element in a Stream
```

### Intervals

``` text
56  Merge Intervals
435 Non-overlapping Intervals
57  Insert Interval
252 Meeting Rooms
```

### BFS / DFS

``` text
200 Number of Islands
994 Rotting Oranges
102 Binary Tree Level Order Traversal
133 Clone Graph
```

### Trees

``` text
104 Maximum Depth of Binary Tree
236 Lowest Common Ancestor
543 Diameter of Binary Tree
124 Binary Tree Maximum Path Sum
```

### DP

``` text
70  Climbing Stairs
198 House Robber
322 Coin Change
300 Longest Increasing Subsequence
```

------------------------------------------------------------------------

# 44. Problem Content Contract

Every problem should have:

``` json
{
  "id": "two-sum",
  "externalId": 1,
  "title": "Two Sum",
  "difficulty": "EASY",
  "primaryPattern": "HASHING",
  "secondaryPatterns": [],
  "statement": "...",
  "constraints": [],
  "examples": [],
  "hints": [
    {
      "level": 1,
      "content": "..."
    },
    {
      "level": 2,
      "content": "..."
    }
  ],
  "pseudocode": [],
  "complexity": {
    "time": "O(n)",
    "space": "O(n)"
  },
  "animationSteps": [],
  "interviewExplanation": "...",
  "commonMistakes": []
}
```

------------------------------------------------------------------------

# 45. Database Design

Use PostgreSQL.

Initial schema:

``` text
users
patterns
problems
problem_examples
problem_hints
problem_animation_steps
problem_test_cases
problem_solutions
user_problem_attempts
user_pattern_mastery
user_daily_progress
user_mistakes
achievements
user_achievements
```

------------------------------------------------------------------------

# 46. Core Tables

## users

``` text
id
username
email
created_at
updated_at
```

For the MVP, authentication can be postponed.

If authentication is required immediately, use a simple email/password
system or Supabase Auth rather than building a complicated OAuth system
tonight.

------------------------------------------------------------------------

## patterns

``` text
id
slug
name
description
recognition_rules
mental_model
difficulty
created_at
```

------------------------------------------------------------------------

## problems

``` text
id
external_id
slug
title
difficulty
primary_pattern_id
statement
constraints
pseudocode
time_complexity
space_complexity
created_at
updated_at
```

------------------------------------------------------------------------

## problem_hints

``` text
id
problem_id
level
content
```

------------------------------------------------------------------------

## animation_steps

``` text
id
problem_id
step_order
step_type
payload
```

`payload` should be JSONB.

------------------------------------------------------------------------

## test_cases

``` text
id
problem_id
input_data
expected_output
is_hidden
```

------------------------------------------------------------------------

## attempts

``` text
id
user_id
problem_id
language
code
status
time_ms
memory_kb
hints_used
time_spent_seconds
pattern_guess
pattern_correct
complexity_guess
complexity_correct
created_at
```

------------------------------------------------------------------------

## pattern_mastery

``` text
id
user_id
pattern_id
recognition_score
correctness_score
speed_score
explanation_score
retention_score
overall_score
updated_at
```

------------------------------------------------------------------------

## mistakes

``` text
id
user_id
problem_id
attempt_id
category
description
lesson
resolved
created_at
```

------------------------------------------------------------------------

# 47. API Design

Use REST initially.

Base:

``` text
/api/v1
```

## Patterns

``` http
GET /api/v1/patterns
GET /api/v1/patterns/{slug}
GET /api/v1/patterns/{slug}/problems
```

## Problems

``` http
GET /api/v1/problems
GET /api/v1/problems/{slug}
GET /api/v1/problems/{slug}/hints
GET /api/v1/problems/{slug}/animation
GET /api/v1/problems/{slug}/test-cases
```

Do not expose hidden test cases.

------------------------------------------------------------------------

# 48. Training API

``` http
POST /api/v1/attempts
POST /api/v1/attempts/{id}/hint
POST /api/v1/attempts/{id}/predict
POST /api/v1/attempts/{id}/complete
POST /api/v1/attempts/{id}/mistake
```

------------------------------------------------------------------------

# 49. Progress API

``` http
GET /api/v1/progress
GET /api/v1/progress/patterns
GET /api/v1/progress/review
GET /api/v1/progress/streak
GET /api/v1/progress/daily
```

------------------------------------------------------------------------

# 50. Recommended Backend

For the first version:

``` text
Java
Spring Boot
PostgreSQL
Spring Data JPA
Flyway
Bean Validation
JUnit
Testcontainers
```

Keep the backend as a **modular monolith**.

Do NOT start with:

``` text
auth-service
problem-service
animation-service
gamification-service
analytics-service
notification-service
gateway
```

That is unnecessary for this product.

One Spring Boot application is enough.

------------------------------------------------------------------------

# 51. Recommended Frontend

``` text
Next.js
TypeScript
Tailwind CSS
shadcn/ui
Monaco Editor
Framer Motion
TanStack Query
Zod
```

Use shadcn for primitives, not for the entire visual identity.

Build your own:

``` text
ProblemCard
PatternCard
ProgressBar
HintPanel
AnimationStage
CodeEditor
TestPanel
ComplexitySelector
SpeedrunTimer
MasteryCard
QuestCard
MistakeCard
```

------------------------------------------------------------------------

# 52. Visual Design

The application should feel like:

``` text
professional developer tool
+
learning game
```

Not:

``` text
children's educational game
```

## Typography

Use a clean sans-serif.

Recommended:

``` text
Inter
Geist
SF Pro-like system stack
```

Use a small type scale.

------------------------------------------------------------------------

# 53. Layout

Desktop:

``` text
┌─────────────────────────────────────────────────────────┐
│ Logo        Problems  Patterns  Review       Profile   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Main content                                            │
│                                                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

Training screen:

``` text
┌─────────────────────────────────────────────────────────┐
│ ← Two Sum                 HASHING          03:42         │
├──────────────────────────────┬──────────────────────────┤
│                              │                          │
│ PROBLEM                      │ ANIMATION                │
│                              │                          │
│ statement                    │ [visual state]           │
│ examples                     │                          │
│ constraints                  │                          │
│                              │                          │
├──────────────────────────────┴──────────────────────────┤
│ HINT 1       HINT 2       HINT 3       HINT 4          │
├─────────────────────────────────────────────────────────┤
│ CODE                                                    │
│                                                         │
│ Monaco editor                                           │
│                                                         │
├─────────────────────────────────────────────────────────┤
│ Tests                         Complexity                │
└─────────────────────────────────────────────────────────┘
```

------------------------------------------------------------------------

# 54. Animation Principles

Animations should be:

-   short
-   purposeful
-   reversible
-   step-based
-   interruptible
-   understandable without sound

Avoid:

-   constant bouncing
-   excessive particle effects
-   giant transitions
-   unnecessary confetti
-   slow animations

Use motion to explain state changes.

------------------------------------------------------------------------

# 55. Animation Timing

Default:

``` text
pointer movement: 250–400ms
array shift: 300–500ms
highlight: 200ms
success: 400ms
screen transition: 200–300ms
```

Allow:

``` text
Pause
Next
Previous
Replay
Speed:
0.5x
1x
1.5x
2x
```

------------------------------------------------------------------------

# 56. Accessibility

Every animation must also have a text explanation.

Example:

``` text
Visual:
L moves from index 2 → 3

Text:
"Left pointer moves right because
the current window is invalid."
```

Support:

-   keyboard navigation
-   reduced motion
-   focus states
-   readable contrast
-   screen-reader labels
-   no information communicated by color alone

------------------------------------------------------------------------

# 57. Dashboard

The dashboard should answer three questions immediately.

### What should I do?

``` text
TODAY'S QUEST
```

### How am I doing?

``` text
XP
STREAK
PROBLEMS
MASTERy
```

### What am I weak at?

``` text
PATTERN RADAR
```

Example:

``` text
Pattern Mastery

Sliding Window   ███████████████ 82%
Hashing          █████████████ 72%
Two Pointers     ███████████ 61%
Binary Search    ████████ 43%
DP               █████ 28%
```

------------------------------------------------------------------------

# 58. Pattern Explorer

Page:

``` text
/patterns
```

Each pattern shows:

``` text
Pattern
Mental model
Recognition signals
Template
Problems
Mastery
Common mistakes
```

Example:

``` text
SLIDING WINDOW

Signal:
"Contiguous + longest/shortest/valid"

Mental model:
"Expand right. Shrink left."

Template:
L = 0

for R in range(n):
    add nums[R]

    while invalid:
        remove nums[L]
        L++

    update answer
```

------------------------------------------------------------------------

# 59. Problem Page

URL:

``` text
/problems/two-sum
```

Sections:

``` text
Problem
Pattern
Examples
Animated Breakdown
Hint Ladder
Pseudocode
Code
Tests
Complexity
Interview Explanation
Mistakes
```

------------------------------------------------------------------------

# 60. Training State Machine

The problem session should have explicit states.

``` text
IDLE
 ↓
SCOUT
 ↓
PATTERN_GUESS
 ↓
BREAKDOWN
 ↓
ANIMATION
 ↓
PREDICTION
 ↓
IMPLEMENTATION
 ↓
TESTING
 ↓
COMPLEXITY
 ↓
EXPLANATION
 ↓
SPEEDRUN
 ↓
COMPLETE
```

Persist the state so a refresh does not destroy the session.

------------------------------------------------------------------------

# 61. Session Model

Frontend state:

``` ts
type TrainingPhase =
  | "SCOUT"
  | "PATTERN_GUESS"
  | "BREAKDOWN"
  | "ANIMATION"
  | "PREDICTION"
  | "IMPLEMENTATION"
  | "TESTING"
  | "COMPLEXITY"
  | "EXPLANATION"
  | "SPEEDRUN"
  | "COMPLETE";
```

This prevents the UI from becoming one giant component.

------------------------------------------------------------------------

# 62. Component Architecture

Recommended:

``` text
src/
├── app/
│   ├── page.tsx
│   ├── dashboard/
│   ├── patterns/
│   ├── problems/
│   ├── review/
│   └── speedrun/
│
├── components/
│   ├── ui/
│   ├── dashboard/
│   ├── patterns/
│   ├── problems/
│   ├── training/
│   ├── animation/
│   └── gamification/
│
├── lib/
│   ├── api/
│   ├── animations/
│   ├── scoring/
│   ├── training/
│   └── utils/
│
├── hooks/
├── types/
└── data/
```

Backend:

``` text
src/main/java/.../
├── pattern/
│   ├── PatternController
│   ├── PatternService
│   ├── PatternRepository
│   └── PatternEntity
│
├── problem/
├── attempt/
├── progress/
├── mistake/
├── gamification/
└── common/
```

------------------------------------------------------------------------

# 63. Backend Layering

Use:

``` text
Controller
    ↓
Service
    ↓
Repository
```

DTOs:

``` text
Request DTO
Response DTO
Entity
Mapper
```

Do not return JPA entities directly from controllers.

Use records where appropriate:

``` java
public record ProblemSummaryResponse(
    UUID id,
    String slug,
    String title,
    Difficulty difficulty,
    String pattern
) {}
```

------------------------------------------------------------------------

# 64. Database Migration

Use Flyway.

Example:

``` text
V1__create_patterns.sql
V2__create_problems.sql
V3__create_hints.sql
V4__create_animation_steps.sql
V5__create_attempts.sql
V6__create_progress.sql
V7__create_mistakes.sql
V8__seed_patterns.sql
V9__seed_problems.sql
```

Never rely on:

``` text
spring.jpa.hibernate.ddl-auto=create
```

for the deployed application.

------------------------------------------------------------------------

# 65. Content Seeding

Problem content is product content.

Keep it version-controlled.

Recommended:

``` text
backend/
└── src/main/resources/
    └── seed/
        ├── patterns/
        ├── problems/
        └── animations/
```

Or insert via Flyway migrations for the first MVP.

Later build an admin content editor.

------------------------------------------------------------------------

# 66. Code Execution Strategy

Do NOT execute arbitrary user code directly inside the Spring Boot API.

That creates a serious security problem.

For the first deploy:

### Option A --- safest MVP

Do not execute arbitrary code server-side.

Provide:

``` text
Monaco editor
+
local browser-side test runner for restricted languages
```

But Java execution in the browser is not trivial.

### Option B --- delayed judge service

Build a separate sandbox later:

``` text
API
 ↓
Job Queue
 ↓
Isolated Runner
 ↓
Result
```

Possible infrastructure:

``` text
Docker
Firecracker
isolated worker
resource limits
timeout
memory limit
network disabled
```

This should be a later phase.

------------------------------------------------------------------------

# 67. Tonight's MVP

Do not build the entire dream tonight.

Build the smallest version that already feels like the product.

## MVP target

The user can:

``` text
1. Open dashboard
2. Pick a pattern
3. Pick a problem
4. Identify the pattern
5. Walk through animation
6. Reveal progressive hints
7. Read pseudocode
8. Write solution
9. Record completion
10. Earn XP
11. See mastery
12. Start another problem
```

That is enough for version 0.1.

------------------------------------------------------------------------

# 68. What NOT to Build Tonight

Do not build:

``` text
OAuth
real-time multiplayer
AI interviewer
AI code judge
mobile app
notifications
email
leaderboards
social profiles
subscriptions
payments
microservices
Kafka
RabbitMQ
video processing
complex analytics
admin CMS
```

Those are future features.

------------------------------------------------------------------------

# 69. Tonight's Architecture

``` text
                  ┌─────────────────┐
                  │     Browser     │
                  │    Next.js      │
                  └────────┬────────┘
                           │ HTTPS
                           ↓
                  ┌─────────────────┐
                  │ Spring Boot API │
                  │     Render      │
                  └────────┬────────┘
                           │
                           ↓
                  ┌─────────────────┐
                  │    Supabase     │
                  │   PostgreSQL    │
                  └─────────────────┘
```

Frontend:

``` text
Vercel
```

API:

``` text
Render Free Web Service
```

Database:

``` text
Supabase Free Postgres
```

This is intentionally boring.

Boring infrastructure is good for an interview-prep product.

As of October 2026, Vercel provides zero-config Next.js deployment;
Render provides free web services but free services spin down after 15
minutes of inactivity; Supabase's free plan includes a 500 MB Postgres
database, 1 GB file storage, and 5 GB egress, with free projects subject
to inactivity pausing. citeturn0search10turn0search0turn1search2

For this app, keep the database mostly relational and store animation
payloads as JSONB. The free Supabase database quota is more than enough
for an initial text-heavy DSA dataset. citeturn1search0turn1search13

------------------------------------------------------------------------

# 70. Free Deployment Reality

The target is:

``` text
$0 tonight
```

not:

``` text
$0 forever under every possible workload
```

Free hosting has limits.

Render's free API service can sleep after inactivity and may take around
a minute to wake. Render also states that free Postgres databases expire
after 30 days, so **do not use Render Postgres as the long-term database
for this project**. citeturn0search0

Use:

``` text
Vercel → frontend
Render → Spring Boot API
Supabase → PostgreSQL
```

The app should tolerate a sleeping API.

------------------------------------------------------------------------

# 71. Environment Variables

Frontend:

``` env
NEXT_PUBLIC_API_URL=https://your-api.onrender.com
```

Backend:

``` env
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...

CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

Never commit secrets.

------------------------------------------------------------------------

# 72. CORS

Allow only:

``` text
http://localhost:3000
https://your-app.vercel.app
```

Do not use:

``` text
*
```

in production.

------------------------------------------------------------------------

# 73. Authentication Strategy

## MVP

Start without accounts if the goal is to deploy tonight.

Use:

``` text
anonymous user
+
localStorage session
```

Progress can initially be stored locally.

Then add authentication.

## Version 0.2

Add:

``` text
email/password
```

or Supabase Auth.

## Version 0.3

Add:

``` text
Google
GitHub
```

Do not let authentication delay the core learning loop.

------------------------------------------------------------------------

# 74. Local Persistence

For anonymous mode:

``` text
localStorage:

patternProgress
problemProgress
streak
xp
mistakes
settings
```

Example:

``` json
{
  "xp": 420,
  "streak": 4,
  "completedProblems": [
    "two-sum",
    "valid-anagram"
  ]
}
```

When authentication arrives, migrate local state to the backend.

------------------------------------------------------------------------

# 75. Smart Problem Recommendation

Version 1:

``` text
score(problem) =
    weakness
  + mistakeFrequency
  + hintDependency
  + timeSinceReview
  + patternImportance
```

Then choose the highest-scoring eligible problem.

Example:

``` text
Your weakest pattern is DP.

You have failed House Robber twice.

Recommendation:

→ House Robber

Reason:
"DP recognition needs practice."
```

Keep explanations transparent.

Do not make recommendations mysterious.

------------------------------------------------------------------------

# 76. Learning Modes

The application should have four modes.

## Learn

``` text
slow
animated
guided
```

## Practice

``` text
moderate guidance
limited hints
```

## Review

``` text
focus on mistakes
```

## Interview

``` text
timed
no hints
minimal UI
```

------------------------------------------------------------------------

# 77. Pattern Recognition Mini-Game

Before every problem:

``` text
PATTERN RADAR

You have 20 seconds.

What signal do you see?

[ Sliding Window ]
[ Two Pointers ]
[ Hashing ]
[ Binary Search ]
[ Heap ]
[ Intervals ]
```

Correct answer:

``` text
+10 XP

Pattern detected in:
4.8 seconds
```

Wrong:

``` text
Not quite.

Signal:
"contiguous substring + longest"

Try again.

You have 1 more attempt.
```

Then show a tiny clue.

------------------------------------------------------------------------

# 78. Edge Case Mini-Game

Before submitting code:

``` text
EDGE CASE RADAR

Pick the cases that could break your solution.

☐ empty input
☐ one element
☐ duplicates
☐ negative values
☐ sorted input
☐ maximum size
```

This trains interview habits.

------------------------------------------------------------------------

# 79. Complexity Mini-Game

Show the algorithm visually.

Example:

``` text
n = 10

Your algorithm performs:

10
20
30
...
100

Pattern:
linear

O(n)
```

For nested loops:

``` text
10 × 10 = 100

O(n²)
```

Make complexity something the user sees, not just memorizes.

------------------------------------------------------------------------

# 80. Pseudocode Builder

Instead of immediately showing pseudocode, make it interactive.

Example:

``` text
Two Sum

1. _________ each number
2. calculate the _________
3. check if complement was _________
4. otherwise _________ current number
```

Options:

``` text
scan
complement
seen
store
```

The user constructs the algorithm.

------------------------------------------------------------------------

# 81. Explanation Builder

Turn interview explanations into blocks.

``` text
I use _________
because _________.

For each _________,
I _________.

This gives _________ time
and _________ space.
```

Then reveal the polished explanation.

This trains communication.

------------------------------------------------------------------------

# 82. "Why This Pattern?" Button

Every problem must answer:

``` text
WHY THIS PATTERN?
```

Example:

``` text
Why Hashing?

We need fast lookup of previously
seen values.

A HashMap gives average O(1) lookup.

Therefore:
Array scan + HashMap
```

This prevents pattern memorization without understanding.

------------------------------------------------------------------------

# 83. "Why Not Brute Force?" Button

Every problem should include:

``` text
BRUTE FORCE
```

Then:

``` text
Why is it too slow?

Input size:
n = 100,000

Brute force:
O(n²)

Approximate operations:
10,000,000,000

Better:
O(n)
```

This teaches optimization naturally.

------------------------------------------------------------------------

# 84. Pattern Template Library

Each pattern should have a canonical template.

Example:

## Sliding Window

``` text
left = 0

for right in range(n):

    add right element

    while window is invalid:
        remove left element
        left++

    update answer
```

## Two Pointers

``` text
left = 0
right = n - 1

while left < right:

    evaluate current state

    if need larger:
        left++
    else:
        right--
```

## Binary Search

``` text
low = ...
high = ...

while low <= high:

    mid = ...

    if found:
        return

    if go left:
        high = mid - 1
    else:
        low = mid + 1
```

## Hashing

``` text
seen = {}

for item in input:

    calculate needed information

    if needed exists:
        use it

    store current item
```

## Monotonic Stack

``` text
stack = []

for item:

    while stack violates monotonic rule:
        resolve stack top
        pop

    push current
```

## Heap

``` text
heap = priority queue

for item:
    push item

    if heap too large:
        pop
```

## BFS

``` text
queue = [start]
visited = {start}

while queue:

    node = queue.pop()

    for neighbor:
        if not visited:
            visited.add(neighbor)
            queue.push(neighbor)
```

## DFS

``` text
dfs(node):

    if base case:
        return

    mark visited

    for child:
        dfs(child)
```

## DP

``` text
define dp[i]

base cases

for each state:
    dp[i] = transition(previous states)

return answer
```

------------------------------------------------------------------------

# 85. Content Authoring Rules

Every problem should be written in this order:

``` text
1. What are we asked to find?
2. What makes brute force expensive?
3. What signal suggests the pattern?
4. What state do we need?
5. What changes each step?
6. What is the invariant?
7. What is the template?
8. What are the edge cases?
9. What is complexity?
10. How would I explain this in an interview?
```

If an explanation cannot answer these questions, the content is
incomplete.

------------------------------------------------------------------------

# 86. The "Invariant" Mechanic

For every major pattern, teach one invariant.

Examples:

### Sliding Window

``` text
The window is valid after shrinking.
```

### Two Pointers

``` text
Moving the correct pointer cannot discard a valid answer.
```

### Binary Search

``` text
The answer remains inside the search space.
```

### Hashing

``` text
The data structure represents information seen so far.
```

### Monotonic Stack

``` text
The stack maintains monotonic order.
```

### BFS

``` text
Nodes are processed level by level.
```

### DP

``` text
Each state stores the best/required answer for its definition.
```

This is where the application moves from memorization to actual
algorithmic reasoning.

------------------------------------------------------------------------

# 87. Analytics

Track useful learning analytics.

``` text
problem_started
pattern_selected
pattern_correct
hint_opened
animation_completed
prediction_correct
code_submitted
test_failed
test_passed
complexity_selected
explanation_submitted
speedrun_started
problem_completed
mistake_created
review_completed
```

Do not track everything.

Track events that can improve learning.

------------------------------------------------------------------------

# 88. Useful Dashboard Analytics

Example:

``` text
THIS WEEK

Problems:
17

Pattern accuracy:
76%

Average solve time:
08:14

Hints/problem:
1.7

Most missed:
Binary Search

Most common mistake:
Off-by-one

Speed improvement:
-19%
```

This gives the user actionable information.

------------------------------------------------------------------------

# 89. Privacy

Do not collect unnecessary personal information.

For anonymous mode:

``` text
no email
no name
no profile
```

For accounts:

``` text
email
username
password hash
```

Never store raw passwords.

Never expose user code publicly by default.

------------------------------------------------------------------------

# 90. Security Checklist

Backend:

``` text
✓ Validate input
✓ DTOs
✓ Bean Validation
✓ CORS restrictions
✓ Rate limiting
✓ SQL injection protection via JPA/parameters
✓ No secrets in Git
✓ Security headers
✓ Error responses without stack traces
✓ Do not execute arbitrary code in API
```

If a code execution service is added:

``` text
✓ isolated container
✓ CPU limit
✓ memory limit
✓ execution timeout
✓ network disabled
✓ filesystem restrictions
✓ process limit
✓ output size limit
✓ automatic cleanup
```

------------------------------------------------------------------------

# 91. Testing Strategy

## Backend

Unit tests:

``` text
PatternService
ProblemService
ScoringService
MasteryService
RecommendationService
```

Integration tests:

``` text
Problem API
Attempt API
Progress API
```

Use:

``` text
JUnit
Mockito where appropriate
Testcontainers PostgreSQL
```

## Frontend

Test:

``` text
pattern selection
hint progression
animation controls
training state
progress calculation
```

------------------------------------------------------------------------

# 92. Acceptance Criteria

A problem is considered complete only when:

``` text
✓ Pattern can be selected
✓ Pattern explanation exists
✓ Hints exist
✓ Animation works
✓ Pseudocode exists
✓ Complexity exists
✓ Test cases exist
✓ Problem can be completed
✓ XP is awarded
✓ Mastery updates
✓ Mistake can be recorded
```

------------------------------------------------------------------------

# 93. Performance

Do not over-optimize.

Initial targets:

``` text
First meaningful page:
< 2.5s

Problem API:
< 300ms locally
< 1s warm production

Animation:
60 FPS where possible

Dashboard:
minimal requests
```

Use:

``` text
TanStack Query
server-side caching where useful
pagination
lazy loading
```

Do not load every animation for every problem on the dashboard.

------------------------------------------------------------------------

# 94. API Response Example

``` json
{
  "slug": "two-sum",
  "title": "Two Sum",
  "difficulty": "EASY",
  "pattern": {
    "slug": "hashing",
    "name": "Hashing"
  },
  "recognition": [
    "Need fast lookup",
    "Need previously seen values"
  ],
  "complexity": {
    "time": "O(n)",
    "space": "O(n)"
  }
}
```

------------------------------------------------------------------------

# 95. Animation API Example

``` json
{
  "problem": "two-sum",
  "steps": [
    {
      "order": 1,
      "type": "ARRAY",
      "payload": {
        "values": [2, 7, 11, 15],
        "currentIndex": 0
      }
    },
    {
      "order": 2,
      "type": "CALCULATION",
      "payload": {
        "expression": "9 - 2",
        "result": 7
      }
    },
    {
      "order": 3,
      "type": "HASH_LOOKUP",
      "payload": {
        "value": 7,
        "found": false
      }
    }
  ]
}
```

------------------------------------------------------------------------

# 96. Scoring Engine

Keep scoring deterministic.

``` ts
type CompletionScore = {
  pattern: number
  hints: number
  correctness: number
  complexity: number
  explanation: number
  speed: number
}
```

Example:

``` text
Pattern recognition     20
Implementation          30
Tests                   20
Complexity              10
Explanation             10
Speed                   10
--------------------------
Total                  100
```

Do not make speed dominate correctness.

------------------------------------------------------------------------

# 97. Performance Grades

Use descriptive grades only for the user's own training result.

``` text
90–100  Excellent
75–89   Strong
60–74   Developing
<60     Needs review
```

These are not claims about real interview performance.

------------------------------------------------------------------------

# 98. Anti-Cram Mechanic

If a user repeatedly solves the same problem:

``` text
"You already solved this 3 times."

Try a new variant.
```

Example:

``` text
Two Sum
    ↓
Two Sum II
    ↓
3Sum
    ↓
4Sum
```

The user learns the pattern across variants.

------------------------------------------------------------------------

# 99. Pattern Transfer

This is a major long-term feature.

After solving:

``` text
Two Sum
```

ask:

``` text
Where else could the same idea appear?
```

Then connect:

``` text
Two Sum
   ↓
3Sum
   ↓
4Sum
   ↓
Subarray Sum
   ↓
Pair problems
```

This builds pattern transfer.

------------------------------------------------------------------------

# 100. Problem Graph

Eventually problems become a graph.

``` text
HASHING
   │
   ├── Two Sum
   │
   ├── Group Anagrams
   │
   ├── Subarray Sum Equals K
   │
   └── Longest Consecutive Sequence

SLIDING WINDOW
   │
   ├── Longest Substring
   ├── Character Replacement
   ├── Minimum Window
   └── Fruit Into Baskets
```

Problems can have multiple edges.

Example:

``` text
Subarray Sum Equals K
        │
        ├── Hashing
        └── Prefix Sum
```

------------------------------------------------------------------------

# 101. "You Know This Pattern" Moment

If the user repeatedly demonstrates mastery:

``` text
PATTERN UNLOCKED

HASHING

You now recognize:
✓ complements
✓ frequency maps
✓ seen sets
✓ prefix lookup
✓ O(1) average lookup
```

Then unlock:

``` text
INTERVIEW MODE
```

This gives the product a sense of progression.

------------------------------------------------------------------------

# 102. Levels

Example:

``` text
Level 1 — Rookie
Level 2 — Pattern Scout
Level 3 — Problem Solver
Level 4 — Algorithm Builder
Level 5 — Speedrunner
Level 6 — Interview Ready
Level 7 — Pattern Master
```

Keep levels cosmetic.

Do not imply that reaching a level guarantees interview success.

------------------------------------------------------------------------

# 103. Achievement System

Examples:

``` text
FIRST BLOOD
Solve your first problem.

PATTERN SCOUT
Correctly identify 10 patterns.

NO HINTS
Solve 5 problems without hints.

SPEEDRUNNER
Beat your personal best.

BOSS SLAYER
Complete a boss problem.

COMEBACK
Fix a previously failed problem.

POLYGLOT
Solve problems in Java and Python.

PATTERN MASTER
Reach high mastery in a pattern.
```

------------------------------------------------------------------------

# 104. The Most Important Achievement

``` text
COMEBACK

You failed this problem before.

Today:
✓ Pattern
✓ Logic
✓ Tests
✓ Explanation
```

Reward fixing mistakes more than grinding new problems.

------------------------------------------------------------------------

# 105. Search

Search should support:

``` text
problem title
problem number
pattern
difficulty
tag
```

Example:

``` text
Search:
"window"

Results:
3 Longest Substring...
76 Minimum Window...
424 Character Replacement...
904 Fruit Into Baskets
```

------------------------------------------------------------------------

# 106. Filters

``` text
Pattern
Difficulty
Training level
Solved
Unsolved
Failed
Needs review
Boss
Language
```

------------------------------------------------------------------------

# 107. Recommended Navigation

``` text
Dashboard
Patterns
Problems
Review
Speedrun
Progress
```

Avoid 15 navigation items.

The reference cheat sheet has 15 sections because it is a poster.

The app should have a much smaller navigation model.

------------------------------------------------------------------------

# 108. Mobile

MVP should be desktop-first because code editing is central.

Still make:

``` text
Dashboard
Pattern pages
Problem reading
Progress
```

responsive.

On mobile:

``` text
Problem
↓
Animation
↓
Hints
↓
Code
```

Do not try to make Monaco comfortable on a tiny screen.

------------------------------------------------------------------------

# 109. Error Handling

User-friendly API states:

``` text
Loading...
```

``` text
Could not load this problem.

Try again.
```

``` text
The API is waking up.

This can take a moment on the free hosting tier.
```

This is particularly useful because Render free services can sleep after
inactivity. citeturn0search0

------------------------------------------------------------------------

# 110. Empty States

Example:

``` text
NO MISTAKES YET

Good.

Once you make one,
we'll turn it into a review.
```

Do not show generic:

``` text
No data found.
```

------------------------------------------------------------------------

# 111. First-Time User Flow

``` text
Landing page
      ↓
"Start Training"
      ↓
Choose language
      ↓
Pattern introduction
      ↓
Two Sum
      ↓
Pattern prediction
      ↓
Animation
      ↓
Code
      ↓
Success
      ↓
Dashboard
```

The user should experience the value within five minutes.

------------------------------------------------------------------------

# 112. Landing Page

Hero:

``` text
Stop memorizing LeetCode.

Learn the patterns behind it.
```

Subheading:

``` text
Practice DSA through animated explanations,
pattern recognition, guided problem solving,
and interview-style speedruns.
```

CTA:

``` text
Start Training
```

Secondary:

``` text
Explore Patterns
```

Show:

``` text
Pattern → Animation → Code → Mastery
```

Not a generic SaaS dashboard screenshot.

------------------------------------------------------------------------

# 113. Landing Page Sections

``` text
Hero
↓
How it works
↓
Pattern system
↓
Animated problem example
↓
Gamified progress
↓
Interview mode
↓
CTA
```

------------------------------------------------------------------------

# 114. Brand Direction

Possible name:

# PatternRun

Tagline:

> **Recognize. Solve. Explain. Repeat.**

Alternative names:

``` text
PatternRun
AlgoSprint
DSAQuest
PatternLab
CodeSprint
AlgoArena
```

Keep the name changeable until the product is built.

------------------------------------------------------------------------

# 115. Design Tokens

Use CSS variables.

``` css
--background
--foreground
--muted
--border
--card
--accent
--success
--warning
--danger
```

Avoid hardcoding colors throughout components.

Use semantic colors.

------------------------------------------------------------------------

# 116. UI Rules

## Cards

Use cards when they create hierarchy.

Do not put every section in a card.

## Buttons

Primary action:

``` text
Start
Continue
Submit
Run
Speedrun
```

Secondary:

``` text
Hint
Replay
Skip
Review
```

## Spacing

Prefer generous whitespace.

``` text
8
12
16
24
32
48
64
```

Do not randomly mix values.

------------------------------------------------------------------------

# 117. Code Editor UX

Top bar:

``` text
Java ▼
Run
Reset
Submit
```

Bottom:

``` text
Tests
Output
Complexity
```

Editor shortcuts:

``` text
Cmd/Ctrl + Enter → Run
Cmd/Ctrl + S     → Save
```

------------------------------------------------------------------------

# 118. Interview Explanation UX

After code passes:

``` text
INTERVIEW CHECK

Explain your solution.

Pattern:
[ Hashing ]

Why:
[ textarea ]

Complexity:
Time [ O(n) ]
Space [ O(n) ]

Submit explanation
```

Then show:

``` text
INTERVIEW CHECKLIST

✓ Identified pattern
✓ Explained data structure
✓ Explained loop
✓ Explained complexity
```

------------------------------------------------------------------------

# 119. Smart Hints

Hint system should track:

``` text
hintsUsed
firstHintTime
lastHintTime
hintLevelReached
```

A user who constantly reaches Hint 5 should receive more basic pattern
training.

------------------------------------------------------------------------

# 120. Adaptive Difficulty

Simple algorithm:

``` text
If:
accuracy > 85%
and hints <= 1
and speed improving

→ increase difficulty
```

If:

``` text
accuracy < 60%
or hints >= 3

→ lower difficulty
or review prerequisite pattern
```

------------------------------------------------------------------------

# 121. Pattern Prerequisites

Example:

``` text
DP
 └── requires:
     Arrays
     Recursion basics
     State thinking
```

Binary Search:

``` text
Arrays
↓
Sorted data
↓
Binary Search
↓
Binary Search on Answer
```

This helps create a learning path.

------------------------------------------------------------------------

# 122. Learning Path

Initial path:

``` text
LEVEL 0
Complexity
Arrays
Hashing

LEVEL 1
Two Pointers
Sliding Window

LEVEL 2
Binary Search
Intervals
Stack

LEVEL 3
Heap
BFS / DFS
Trees

LEVEL 4
DP
Advanced combinations
```

The reference poster remains the source of the initial pattern
vocabulary, but the app can order it pedagogically.

------------------------------------------------------------------------

# 123. Daily 30-Minute Mode

This is important for interview prep.

``` text
30 MINUTES

05 min
Pattern recognition

10 min
Guided problem

10 min
Independent problem

05 min
Review
```

At the end:

``` text
TODAY'S RESULT

Pattern accuracy +8%
Average hints -1
1 mistake reviewed
1 speed record
```

------------------------------------------------------------------------

# 124. 60-Minute Interview Mode

``` text
10 min
Warm-up

20 min
Medium problem

20 min
Second problem

10 min
Explain + review
```

The app generates the session.

------------------------------------------------------------------------

# 125. Weekly Challenge

Every week:

``` text
THE PATTERN GAUNTLET

5 problems
5 different patterns
No hints
Timed
```

Example:

``` text
1. Hashing
2. Sliding Window
3. Binary Search
4. Trees
5. DP
```

The goal is pattern switching.

That is closer to real interviews.

------------------------------------------------------------------------

# 126. Pattern Switching

A strong interview candidate must recognize that the pattern changed.

Game:

``` text
PROBLEM 1
Hashing

PROBLEM 2
Sliding Window

PROBLEM 3
Binary Search

PROBLEM 4
Intervals

PROBLEM 5
DP
```

No category is shown initially.

The user must identify each one.

------------------------------------------------------------------------

# 127. The "Unknown Problem" Mode

Eventually:

``` text
UNKNOWN PROBLEM
```

The app hides:

``` text
pattern
tags
difficulty
```

The user sees only the statement.

Then:

``` text
What do you see?

[Your reasoning]
```

The app compares the selected pattern with the curated expected
patterns.

This is the final recognition test.

------------------------------------------------------------------------

# 128. Future AI Layer

AI should be added only after the deterministic product works.

Possible uses:

``` text
explain user's mistake
generate alternative explanation
simulate interviewer
evaluate explanation structure
generate variants
generate hints
```

AI should not be the core of the app.

The core learning system should work without AI.

------------------------------------------------------------------------

# 129. AI Hint Contract

If AI is eventually used:

Bad:

``` text
Use a HashMap and calculate target - nums[i].
```

Good:

``` text
What information would make it
possible to answer "have I seen
the needed number before?"
```

AI should preserve the hint ladder.

------------------------------------------------------------------------

# 130. AI Interviewer

Future:

``` text
Interviewer:
"What is your approach?"

User:
"I'll use a map..."

Interviewer:
"Why?"

User:
"Because..."

Interviewer:
"What is the complexity?"
```

The AI evaluates:

``` text
clarity
structure
complexity
tradeoffs
edge cases
```

It should not pretend to predict hiring outcomes.

------------------------------------------------------------------------

# 131. Deployment Plan --- Tonight

## Phase 0 --- 20 minutes

Create:

``` text
patternrun/
├── frontend/
├── backend/
└── README.md
```

Initialize Git.

------------------------------------------------------------------------

## Phase 1 --- 60 minutes

Spring Boot:

``` text
Spring Web
Spring Data JPA
PostgreSQL
Validation
Flyway
Lombok
```

Create:

``` text
Pattern
Problem
Hint
AnimationStep
```

------------------------------------------------------------------------

## Phase 2 --- 60 minutes

Create:

``` text
GET /patterns
GET /patterns/{slug}
GET /problems
GET /problems/{slug}
```

Seed the 10 core patterns.

Seed 20--30 problems first.

Do not wait for all 40+ problems.

------------------------------------------------------------------------

## Phase 3 --- 90 minutes

Next.js:

``` text
Dashboard
Patterns
Problems
Training page
```

Implement:

``` text
pattern selection
problem selection
hint ladder
animation player
progress
XP
```

------------------------------------------------------------------------

## Phase 4 --- 60 minutes

Add:

``` text
Monaco
complexity selection
completion
mastery
mistake tracking
```

Do not build server-side code execution.

------------------------------------------------------------------------

## Phase 5 --- 30 minutes

Polish:

``` text
loading states
empty states
error states
responsive layout
keyboard shortcuts
animation speed
```

------------------------------------------------------------------------

## Phase 6 --- 30--45 minutes

Deploy:

``` text
Supabase
Render
Vercel
```

Then test the production flow.

------------------------------------------------------------------------

# 132. First Production Milestone

The first public version should let someone complete this:

``` text
Open app
  ↓
Choose Sliding Window
  ↓
Choose Longest Substring Without Repeating Characters
  ↓
Guess pattern
  ↓
Watch animation
  ↓
Use hint
  ↓
Build pseudocode
  ↓
Write code
  ↓
Submit completion
  ↓
Earn XP
  ↓
See mastery
  ↓
Review mistake
```

If this works beautifully, the product is already useful.

------------------------------------------------------------------------

# 133. Version Roadmap

## v0.1 --- Tonight

``` text
✓ Dashboard
✓ Pattern library
✓ Problem library
✓ Pattern recognition
✓ Animated steps
✓ Hint ladder
✓ Pseudocode
✓ Complexity
✓ XP
✓ Basic mastery
✓ Local progress
✓ Production deployment
```

------------------------------------------------------------------------

## v0.2

``` text
✓ Accounts
✓ Persistent progress
✓ Mistake journal
✓ Review queue
✓ Daily quests
✓ Streaks
✓ Better recommendations
✓ 60–100 problems
```

------------------------------------------------------------------------

## v0.3

``` text
✓ Interview mode
✓ Speedruns
✓ Boss problems
✓ Weekly gauntlets
✓ Java + Python templates
✓ More animations
✓ Pattern transfer
```

------------------------------------------------------------------------

## v0.4

``` text
✓ Secure code execution service
✓ Hidden tests
✓ Runtime metrics
✓ Memory metrics
✓ Code submissions
```

------------------------------------------------------------------------

## v0.5

``` text
✓ AI hints
✓ AI interviewer
✓ Generated variants
✓ Personalized study plans
```

------------------------------------------------------------------------

# 134. Repository Strategy

Recommended monorepo:

``` text
patternrun/
├── apps/
│   ├── web/
│   └── api/
│
├── content/
│   ├── patterns/
│   ├── problems/
│   └── animations/
│
├── docs/
│   ├── architecture.md
│   ├── content-guide.md
│   └── animation-guide.md
│
├── docker-compose.yml
├── README.md
└── .gitignore
```

If managing two separate deployment repos is easier tonight:

``` text
patternrun-web
patternrun-api
```

Either is acceptable.

------------------------------------------------------------------------

# 135. Local Development

Frontend:

``` bash
cd apps/web
npm install
npm run dev
```

Backend:

``` bash
cd apps/api
./mvnw spring-boot:run
```

Database:

``` bash
docker compose up -d postgres
```

------------------------------------------------------------------------

# 136. Local Docker Compose

``` yaml
services:

  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: patternrun
      POSTGRES_USER: patternrun
      POSTGRES_PASSWORD: patternrun
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

------------------------------------------------------------------------

# 137. Backend Configuration

Local:

``` yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/patternrun
    username: patternrun
    password: patternrun

  jpa:
    open-in-view: false

  flyway:
    enabled: true
```

Production values should come from environment variables.

------------------------------------------------------------------------

# 138. API Contract First

Before writing the frontend, define:

``` text
GET /api/v1/patterns
GET /api/v1/problems
GET /api/v1/problems/{slug}
```

Then the frontend consumes those contracts.

This prevents frontend/backend drift.

------------------------------------------------------------------------

# 139. Seed Data First

Do not build a CMS first.

Create:

``` text
10 patterns
20 problems
100+ hints/steps
```

Then build the UI.

The app is an educational product.

Content quality matters as much as code quality.

------------------------------------------------------------------------

# 140. Content Quality Checklist

For each problem:

``` text
[ ] Is the pattern obvious after explanation?
[ ] Is the pattern not obvious before thinking?
[ ] Is brute force explained?
[ ] Is the optimization explained?
[ ] Is the invariant clear?
[ ] Is the animation visual?
[ ] Does every hint reveal only one new idea?
[ ] Are edge cases covered?
[ ] Is complexity explained?
[ ] Can the user explain it in 3 sentences?
[ ] Is there a common mistake?
[ ] Is there a variant?
```

------------------------------------------------------------------------

# 141. Example --- Two Sum Complete Training

## Scout

``` text
Given:
[2, 7, 11, 15]
target = 9
```

Ask:

``` text
Which pattern?

Hashing
Two Pointers
Sliding Window
Binary Search
```

------------------------------------------------------------------------

## Decode

``` text
Need two values.

2 + ? = 9

? = 7
```

------------------------------------------------------------------------

## Hint

``` text
Can we remember numbers
we have already seen?
```

------------------------------------------------------------------------

## Animation

``` text
2
↓
target - 2
↓
7
↓
Have we seen 7?
NO
↓
store 2
```

Next:

``` text
7
↓
target - 7
↓
2
↓
Have we seen 2?
YES
↓
ANSWER
```

------------------------------------------------------------------------

## Complexity

``` text
Time: O(n)
Space: O(n)
```

------------------------------------------------------------------------

## Interview Explanation

``` text
Use a HashMap to store numbers seen so far.
For each number, calculate the complement needed
to reach the target and check the map.
This gives O(n) time and O(n) space.
```

------------------------------------------------------------------------

# 142. Example --- Sliding Window Complete Training

Problem:

``` text
Longest Substring Without Repeating Characters
```

Animation:

``` text
a b c a b c b b
↑
L

a b c
    ↑
    R
```

State:

``` text
window = "abc"
```

Next:

``` text
a b c a
↑     ↑
L     R
```

Invalid because:

``` text
a repeats
```

Shrink:

``` text
a b c a
  ↑   ↑
  L   R
```

Then:

``` text
window = "bca"
```

The user learns the behavior rather than memorizing code.

------------------------------------------------------------------------

# 143. Definition of Done

A feature is not done because the code compiles.

It is done when:

``` text
✓ works
✓ visually understandable
✓ handles loading
✓ handles errors
✓ keyboard accessible
✓ tested
✓ responsive enough
✓ API contract documented
✓ no secrets committed
✓ deployed
```

------------------------------------------------------------------------

# 144. Engineering Rules

Keep the project simple.

## Do

``` text
small components
clear names
DTOs
services
tests
migrations
typed API responses
reusable animation primitives
```

## Avoid

``` text
god components
god services
premature microservices
global mutable state everywhere
random utility files
duplicated animation logic
hardcoded progress logic
```

------------------------------------------------------------------------

# 145. Product Rule

Every feature must answer:

> **Does this help the user recognize, solve, explain, or retain
> algorithms?**

If not:

``` text
defer it.
```

This is the anti-bloat rule.

------------------------------------------------------------------------

# 146. The Core Loop in One Screen

The entire product should ultimately feel like:

``` text
┌────────────────────────────────────────────┐
│             TODAY'S TRAINING               │
│                                            │
│  Pattern: Sliding Window                   │
│  Problem: Minimum Window Substring         │
│                                            │
│  01  SCOUT                                 │
│  ✓                                           │
│  02  IDENTIFY                              │
│  ✓                                           │
│  03  ANIMATE                               │
│  →                                           │
│  04  BUILD                                 │
│  ○                                           │
│  05  TEST                                  │
│  ○                                           │
│  06  EXPLAIN                               │
│  ○                                           │
│  07  SPEEDRUN                              │
│  ○                                           │
│                                            │
│              [ CONTINUE ]                  │
└────────────────────────────────────────────┘
```

This is the product.

Everything else supports this loop.

------------------------------------------------------------------------

# 147. Final Product Principle

The reference cheat sheet says:

> **Master patterns, solve faster.**

PatternRun should turn that sentence into an actual training system.

The user should eventually see:

``` text
NEW PROBLEM
     ↓
What is the signal?
     ↓
What pattern fits?
     ↓
What state do I maintain?
     ↓
What changes each step?
     ↓
What invariant must stay true?
     ↓
Can I write the template?
     ↓
Can I solve the problem?
     ↓
Can I explain it?
     ↓
Can I solve a variant?
     ↓
Can I do it faster tomorrow?
```

That is the real game.

------------------------------------------------------------------------

# 148. Immediate Build Order

Start here.

``` text
DAY 0 — PRODUCT FOUNDATION

[ ] Create monorepo
[ ] Create Next.js app
[ ] Create Spring Boot app
[ ] Connect PostgreSQL
[ ] Add Flyway
[ ] Create Pattern entity
[ ] Create Problem entity
[ ] Seed 10 patterns
[ ] Seed first 20 problems

DAY 1 — TRAINING LOOP

[ ] Dashboard
[ ] Pattern picker
[ ] Problem page
[ ] Pattern guess
[ ] Hint ladder
[ ] Animation player
[ ] Pseudocode
[ ] Complexity
[ ] Completion

DAY 2 — GAME

[ ] XP
[ ] Streak
[ ] Mastery
[ ] Mistakes
[ ] Review queue
[ ] Daily quest

DAY 3 — INTERVIEW

[ ] Speedrun
[ ] Interview mode
[ ] Explanation
[ ] Edge-case check
[ ] Boss problems

DAY 4+

[ ] More problems
[ ] Better animations
[ ] Accounts
[ ] Code execution
[ ] AI layer
```

------------------------------------------------------------------------

# 149. Tonight's Non-Negotiable Scope

If time gets tight, ship this:

``` text
1. Pattern picker
2. Problem page
3. Animated breakdown
4. Hint ladder
5. Pseudocode builder
6. Complexity check
7. Completion
8. XP
9. Mastery
10. Deployment
```

Everything else can wait.

------------------------------------------------------------------------

# 150. The First Commit

The first meaningful commit should eventually look like:

``` text
feat: create PatternRun learning loop
```

Not:

``` text
feat: create 47 microservices
```

Build the learning loop first.

Then build the platform around it.

------------------------------------------------------------------------

# 151. Final Architecture

``` text
                           PATTERNRUN
                               │
              ┌────────────────┴────────────────┐
              │                                 │
          LEARNING                           GAMING
              │                                 │
       ┌──────┼──────┐                  ┌───────┼───────┐
       │      │      │                  │       │       │
    Patterns Problems Hints            XP    Streaks Mastery
       │      │      │                  │       │       │
       └──────┼──────┘                  └───────┼───────┘
              │                                 │
              └──────────────┬──────────────────┘
                             │
                       TRAINING ENGINE
                             │
         ┌───────────────────┼────────────────────┐
         │                   │                    │
      Scout               Animate              Solve
         │                   │                    │
      Pattern              Predict              Code
      Guess                 State               Tests
         │                   │                    │
         └───────────────────┼────────────────────┘
                             │
                          Explain
                             │
                         Speedrun
                             │
                         Review
                             │
                         Mastery
```

------------------------------------------------------------------------

# 152. The North Star

Do not optimize PatternRun for:

``` text
more problems
more badges
more pages
more animations
more AI
more features
```

Optimize it for:

``` text
LESS CONFUSION
      ↓
BETTER PATTERN RECOGNITION
      ↓
FASTER IMPLEMENTATION
      ↓
BETTER EXPLANATIONS
      ↓
BETTER RETENTION
      ↓
BETTER INTERVIEW PREPARATION
```

The app should make DSA feel less like:

> "I have 500 LeetCode problems to memorize."

and more like:

> "I have a small set of patterns. Show me the problem. Let me figure
> out which tool fits."

That is the product.

------------------------------------------------------------------------

# 153. First Mission

``` text
MISSION 001
────────────────────────────

Build the PatternRun prototype.

Goal:

Recognize → Animate → Solve → Explain → Earn XP

Patterns:

✓ Hashing
✓ Sliding Window
✓ Two Pointers
✓ Binary Search
✓ Monotonic Stack
✓ Heap
✓ Intervals
✓ BFS / DFS
✓ Trees
✓ DP

First boss:

Two Sum

Reward:

+100 XP
+1 Pattern Mastery
+1 Problem Completed

Next:

Longest Substring Without Repeating Characters
```

**Ship the loop first. Expand the world second.**
