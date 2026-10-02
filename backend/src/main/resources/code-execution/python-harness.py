"""Runs one learner's program against a list of cases and reports what happened.

This file is not the security boundary. The container it runs inside is: no network, a read-only
filesystem, a memory cap, a pid cap, no capabilities and a non-root user. This file exists so that
a failure arrives as a structured answer a learner can act on, rather than as an exit code.

Protocol
--------
stdin  one JSON object:
    {"source": str, "entrypoint": str, "timeoutSeconds": float,
     "argumentMode": "PLAIN" | "TREE",
     "cases": [{"label": str, "args": [...], "expected": <json>}, ...]}
stdout one JSON object:
    {"status": "COMPLETED" | "SYNTAX_ERROR" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" |
     "MEMORY_LIMIT_EXCEEDED" | "INTERNAL_ERROR",
     "cases": [{"passed": bool, "actual": str | null}],
     "error": {"type": str, "message": str, "line": int | null} | null}

The learner never writes to this channel. Their stdout and stderr are redirected into a bounded
sink before their code runs, so a stray print() cannot corrupt the answer and a print loop cannot
exhaust the container's memory.

`status` answers one question: did the code run? It is deliberately NOT the verdict. Whether the
answers were right is for the server to work out from the per-case results, because "it executed"
and "it is correct" are different claims and conflating them here would make a wrong answer look
like a pass for reasons the runner has no business knowing.

The result is written with a reference to os.write captured at import time. That is a guard
against the learner reassigning os.write, not a guarantee; see the note above.
"""

import io
import json
import os
import signal
import sys
import traceback
from collections import Counter, defaultdict, deque

# The namespace a learner's source runs in.
#
# Seeded with the collections every DSA solution in this catalogue reaches for, for the same reason
# a judge does: `Counter` for frequency maps, `deque` for breadth-first work, `defaultdict` for
# counting prefixes. Without this a learner whose solution is entirely correct raises NameError on
# the import line, and the failure teaches them nothing about their algorithm.
#
# It is deliberately small. Adding the whole standard library would mean a learner's solution could
# quietly depend on something the catalogue never intended, and a name here has to be a deliberate
# promise rather than an accident of what is in scope.
class TreeNode:
    """A binary tree node, supplied because two problems are asked in terms of one.

    Level order with nulls is how a tree is written down in every problem statement, so that is the
    form the test data is stored in. The learner is not asked to deserialise it: their file contains
    only their function, exactly as it would on a judge, and `build_tree` is here because the
    calling convention is the runner's business rather than theirs.
    """

    __slots__ = ("value", "left", "right")

    def __init__(self, value=None, left=None, right=None):
        self.value = value
        self.left = left
        self.right = right


def build_tree(level_order):
    """Build a tree from level order with nulls for absent children.

    Returns None for an empty tree, so a null return value is not confused with an empty one.
    """
    if not level_order or level_order[0] is None:
        return None
    nodes = [TreeNode(value) if value is not None else None for value in level_order]
    children = 1
    for node in nodes:
        if node is None:
            continue
        if children < len(nodes):
            node.left = nodes[children]
            children += 1
        if children < len(nodes):
            node.right = nodes[children]
            children += 1
    return nodes[0]


RUNNER_NAMESPACE = {
    "Counter": Counter,
    "defaultdict": defaultdict,
    "deque": deque,
    "TreeNode": TreeNode,
    "build_tree": build_tree,
    "__name__": "__main__",
    "__builtins__": __builtins__,
}

# The name the learner's source is compiled under. It appears in tracebacks, which is how a
# learner is told which of their own lines failed. A real filename, not "<string>", so tracebacks
# read like a normal editor's.
SOLUTION_FILENAME = "solution.py"

# A returned value is only ever shown to the person who produced it, so the cap is generous
# enough for a debug print to be useful and small enough that a runaway repr cannot hurt.
MAX_VALUE_CHARS = 2000

_write = os.write


class _Timeout(Exception):
    pass


def _raise_timeout(*_):
    raise _Timeout()


class _BoundedSink(io.StringIO):
    """A StringIO that stops growing once it is full.

    A print() inside a loop is the cheapest way to burn a memory limit, and a learner debugging
    with print() is exactly who will do it. Keeping the first N characters and dropping the rest
    preserves the debugging value while making the runaway impossible.
    """

    def __init__(self, limit):
        super().__init__()
        self._limit = limit
        self._used = 0

    def write(self, text):
        if self._used >= self._limit:
            return len(text)
        room = self._limit - self._used
        chunk = text[:room]
        self._used += len(chunk)
        return super().write(chunk)


def _render(value):
    """A short, readable form of whatever came back, for the result panel."""
    try:
        text = json.dumps(value)
    except (TypeError, ValueError):
        text = repr(value)
    return text if len(text) <= MAX_VALUE_CHARS else text[:MAX_VALUE_CHARS] + "..."


def _equivalent(actual, expected):
    """Structural comparison, order-insensitive for sequences.

    Order-insensitive because returning the same indices in a different order is the same
    answer; a learner told they were wrong for writing [1, 0] instead of [0, 1] would be taught
    something false about their own code.
    """
    if isinstance(expected, list) and isinstance(actual, (list, tuple)):
        if len(actual) != len(expected):
            return False
        remaining = list(actual)
        for item in expected:
            if item in remaining:
                remaining.remove(item)
            else:
                return False
        return True
    if isinstance(expected, dict) and isinstance(actual, dict):
        if set(actual.keys()) != set(expected.keys()):
            return False
        return all(_equivalent(actual[k], expected[k]) for k in expected)
    # bool is a subclass of int in Python, and True == 1 would let a wrong answer pass.
    if isinstance(expected, bool) != isinstance(actual, bool):
        return False
    return actual == expected


def _failure(status, error_type, message, line=None, cases=None):
    return {
        "status": status,
        "cases": cases or [],
        "error": {"type": error_type, "message": message, "line": line},
    }


def _run(payload):
    source = payload["source"]
    entrypoint = payload["entrypoint"]
    timeout = float(payload.get("timeoutSeconds", 2.0))

    namespace = dict(RUNNER_NAMESPACE)
    try:
        compiled = compile(source, SOLUTION_FILENAME, "exec")
    except SyntaxError as error:
        return _failure("SYNTAX_ERROR", type(error).__name__, str(error.msg), error.lineno)
    except ValueError as error:  # null bytes, and similar source-level problems
        return _failure("SYNTAX_ERROR", type(error).__name__, str(error))

    real_stdout, real_stderr = sys.stdout, sys.stderr
    sink = _BoundedSink(64 * 1024)
    # The default disposition of SIGALRM is to terminate the process, so a timeout would kill the
    # runner and produce no answer at all. Turning it into an exception is what makes
    # TIME_LIMIT_EXCEEDED a reportable result rather than an empty pipe.
    signal.signal(signal.SIGALRM, _raise_timeout)
    signal.setitimer(signal.ITIMER_REAL, timeout)
    try:
        sys.stdout = sink
        sys.stderr = sink
        exec(compiled, namespace)  # noqa: S102 - executing the learner is the entire point
        function = namespace.get(entrypoint)
        if not callable(function):
            return _failure(
                "RUNTIME_ERROR",
                "NameError",
                "No function named %s was defined. The runner calls %s()." % (entrypoint, entrypoint),
            )

        tree_argument = payload.get("argumentMode") == "TREE"
        results = []
        for case in payload["cases"]:
            try:
                arguments = list(case["args"])
                if tree_argument and arguments:
                    # Only the first argument is a tree. Every tree problem has the root first and
                    # nothing else, so this is unambiguous rather than a guess.
                    arguments[0] = build_tree(arguments[0])
                actual = function(*arguments)
                results.append({"passed": _equivalent(actual, case["expected"]),
                                "actual": _render(actual)})
            except _Timeout:
                raise
            except BaseException as error:  # noqa: BLE001 - every failure is reportable
                results.append({
                    "passed": False,
                    "actual": None,
                    "error": _describe(error),
                })
    except _Timeout:
        return _failure("TIME_LIMIT_EXCEEDED", "TimeoutError",
                        "The solution did not finish in time.")
    except RecursionError:
        return _failure("RUNTIME_ERROR", "RecursionError", "Maximum recursion depth exceeded.")
    except MemoryError:
        return _failure("MEMORY_LIMIT_EXCEEDED", "MemoryError",
                        "The solution ran out of memory.")
    except BaseException as error:  # noqa: BLE001 - report anything that escaped the loop
        return _failure(*_describe(error, status="RUNTIME_ERROR"))
    finally:
        sys.stdout, sys.stderr = real_stdout, real_stderr
        signal.setitimer(signal.ITIMER_REAL, 0)

    # A per-case exception outranks a wrong answer: "wrong" when the code actually threw would be
    # a false explanation of the learner's problem.
    for result in results:
        if result.get("error") is not None:
            described = result["error"]
            return _failure(described.pop("status"), described["type"], described["message"],
                            described.get("line"), results)

    # COMPLETED means "it ran", not "it passed". Whether the answers were right is the server's
    # call to make from `cases`, and making it here would put the verdict in the one component that
    # has the least context to make it.
    return {"status": "COMPLETED", "cases": results, "error": None}


def _describe(error, status="RUNTIME_ERROR"):
    """Turn an exception into something a learner can act on.

    Only frames from the learner's own file are considered. The harness's frames would otherwise
    dominate the message and would expose the sandbox's own file layout, which is both noise and
    a small amount of information about the infrastructure.
    """
    line = None
    for frame in traceback.extract_tb(error.__traceback__):
        if frame.filename == SOLUTION_FILENAME:
            line = frame.lineno
    message = str(error) or type(error).__name__
    return {"status": status, "type": type(error).__name__, "message": message, "line": line}


def main():
    try:
        payload = json.loads(sys.stdin.read())
    except ValueError:
        _write(1, json.dumps(_failure("INTERNAL_ERROR", "ProtocolError",
                                      "The runner sent an unreadable request.")).encode())
        return

    try:
        result = _run(payload)
    except BaseException as error:  # noqa: BLE001 - the harness must never crash silently
        result = _failure("INTERNAL_ERROR", type(error).__name__, "The runner itself failed.")

    _write(1, json.dumps(result).encode())


if __name__ == "__main__":
    main()