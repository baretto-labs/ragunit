# Contributing

RAGUnit follows strict TDD and DDD principles. Before contributing, read
[`DOMAIN.md`](../DOMAIN.md) and [`CLAUDE.md`](../CLAUDE.md).

---

## Development Setup

```bash
git clone https://github.com/your-org/ragunit
cd ragunit

# Build and run unit tests (fast — no LLM required)
mvn test

# Run integration tests (requires Ollama)
mvn test -Dgroups=rag-eval

# Full build + mutation testing + audit
mvn clean verify
```

---

## Module Structure

```
ragunit/
├── ragunit-core/               # Zero production deps. Pure Java 17.
│   └── src/main/java/org/ragunit/core/
│       ├── domain/             # Records, value objects
│       ├── judge/              # RagJudge interface + OllamaJudge
│       ├── assertion/          # RagAssert fluent API
│       └── report/             # RagReporter + JsonFileReporter
├── ragunit-embedding/          # Optional: OllamaEmbeddingProvider
└── ragunit-examples/           # Runnable examples
```

---

## Rules

### TDD (strict)
- Write the failing test first, then the minimum code to pass it
- Test naming: `should_[expectedBehavior]_when_[condition]`
- One logical assertion per test
- `@RagTest` on every test that calls a judge

### DDD
- Every public name must come from [`DOMAIN.md`](../DOMAIN.md)
- No `Manager`, `Helper`, `Utils`, `Service` in `ragunit-core`
- Propose new terms in your PR if needed

### Supply Chain
- `ragunit-core` has zero production dependencies — this is non-negotiable
- New dependencies in any module require an OWASP CVE check and explicit justification

### Code Style
- Java 17+: use records, sealed interfaces, switch expressions
- Method ≤ 20 lines, class ≤ 200 lines
- No `null` in public APIs — use `Optional` or throw at the boundary

---

## Adding a New Metric

1. Add the metric term to `DOMAIN.md`
2. Add a `MetricType` constant
3. Add the method to `RagJudge` interface
4. Implement in `OllamaJudge`
5. Add fluent assertion method(s) to `AnswerAssert` or `ContextAssert`
6. Write tests (domain unit tests + integration stub tests)
7. Create `docs/metrics/your-metric.md`
8. Add to the `mkdocs.yml` nav

---

## Definition of Done

A task is done when all three components are complete:

- **Dev**: TDD (red → green → refactor), `mvn verify` passes, mutation score ≥ 80%
- **Tests**: naming convention, one assertion per test, `@RagTest` on LLM tests
- **Documentation**: Javadoc on all public methods, `docs/metrics/` page, `DOMAIN.md` updated

---

## PR Checklist

```bash
mvn verify                       # all tests + checkstyle + spotbugs
mvn javadoc:aggregate            # 0 warnings
mvn -pl ragunit-core \
    test-compile \
    org.pitest:pitest-maven:mutationCoverage   # ≥ 80%
```
