# Contributing to RAGUnit

Thank you for your interest in RAGUnit! This guide covers everything you need
to go from zero to a merged pull request.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Setting up the development environment](#setting-up-the-development-environment)
3. [Running Ollama for integration tests](#running-ollama-for-integration-tests)
4. [Build and test commands](#build-and-test-commands)
5. [Development cycle (TDD)](#development-cycle-tdd)
6. [Naming conventions](#naming-conventions)
7. [Code rules](#code-rules)
8. [Submitting a PR](#submitting-a-pr)
9. [Review and merge process](#review-and-merge-process)

---

## Prerequisites

- Java 17+
- Maven 3.9+
- [Ollama](https://ollama.com) — only required for `@RagTest` integration tests

---

## Setting up the development environment

```bash
git clone https://github.com/baretto/ragunit.git
cd ragunit
mvn clean verify -DskipTests   # verify the build compiles and plugins are satisfied
```

The project is a standard Maven multi-module build. Import it into any IDE as
a Maven project — no special plugins required.

**Modules:**

| Module | Purpose |
|--------|---------|
| `ragunit-core` | Core library — zero production dependencies |
| `ragunit-embedding` | Optional embedding support (cosine similarity) |
| `ragunit-examples/ragunit-example-vanilla` | Runnable examples against a real Ollama instance |

---

## Running Ollama for integration tests

Integration tests (tagged `@RagTest`) require a running Ollama instance with
the `qwen2.5:14b` model. This is the minimum model size for reliable evaluation quality.

```bash
# Install Ollama: https://ollama.com/download
ollama pull qwen2.5:14b
ollama serve              # starts on localhost:11434 by default
```

To run the full test suite including integration tests:

```bash
mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval test
```

To skip integration tests (fast CI / no Ollama required):

```bash
mvn test                  # @RagTest are excluded by default
```

---

## Build and test commands

```bash
mvn clean verify               # compile + test + Checkstyle + SpotBugs
mvn test                       # fast tests only (excludes @RagTest)
mvn -pl ragunit-core test      # single module
mvn -Dtest=MyTestClass test    # single test class
```

### Mutation testing

```bash
mvn -pl ragunit-core test-compile org.pitest:pitest-maven:mutationCoverage
```

Threshold: **80%**. PRs that drop below this threshold on new classes are blocked.

---

## Development cycle (TDD)

RAGUnit follows strict TDD. No production code without a failing test first.

1. Pick a task from `.tasks/BACKLOG.md` with status `ready`
2. Update its status to `in-progress` in the task file
3. **Write a failing test first** — naming: `should_[behavior]_when_[condition]`
4. Write the minimum production code to make it pass
5. Refactor if needed, keeping tests green
6. Verify DoD: `mvn verify` + pitest ≥ 80% + Javadoc + docs page
7. Open a PR using the PR template

---

## Naming conventions

Every public class, interface, method, and field must use a term from [`DOMAIN.md`](DOMAIN.md).
Any deviation requires a comment explaining why.

**Forbidden names in `ragunit-core`:** `Manager`, `Handler`, `Helper`, `Utils`,
`Service`, `Evaluator`, `Result`, `Client`.

---

## Code rules

- Records for all immutable data (`Document`, `Verdict`, `Score`…)
- No `null` in public APIs — use `Optional` or throw at the boundary
- Text blocks (`"""..."""`) for all multi-line strings — never `+` concatenation
- Method length ≤ 20 lines · Class length ≤ 200 lines · Cyclomatic complexity ≤ 5
- `ragunit-core` has **zero production dependencies** — period

---

## Submitting a PR

1. Fork the repository and create a branch: `git checkout -b feat/my-feature`
2. Implement following the TDD cycle above
3. Ensure `mvn verify` passes with 0 Checkstyle and 0 SpotBugs violations
4. Open a pull request — fill in the PR template checklist completely

---

## Review and merge process

- A maintainer will review within a few days
- Feedback will be given as PR comments — address each point or explain why not
- Once all checks pass and the review is approved, the PR will be merged
- Questions? Open a discussion or reach out at contact@baretto.fr
