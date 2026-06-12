# Deterministic Assertions

The right evaluation pattern is **hard checks + soft judgment**: cheap, exact,
reproducible string checks first, LLM-judged quality second. RAGUnit makes both
first-class on the same fluent builder so you don't go all-in on the LLM judge.

---

## Helpers

`RagAssert.assertThatAnswer(...)` (accepting an `Answer` or a plain `String`)
exposes four deterministic helpers. They run instantly, make no LLM call, and
never touch the reporters:

```java
RagAssert.assertThatAnswer(answer)
        .contains("/auth/token")               // substring present
        .containsAll("POST", "client_id")      // every substring present
        .matches(".*expires in \\d+ seconds.*") // whole text matches the regex
        .hasMinLength(50);                      // at least n characters
```

Failure messages quote the full answer text, and `containsAll` lists exactly
which substrings are missing.

## Chaining hard checks with the judge

Deterministic helpers return the same builder, so they chain freely with the
judged assertions — in either order:

```java
RagAssert.assertThatAnswer(answer)
        .contains("/auth/token")        // hard check: fail fast, zero cost
        .hasMinLength(50)
        .givenContext(context)
        .forQuestion(question)
        .evaluatedBy(judge)
        .withRuns(3)
        .isFaithfulToContext(0.80);     // soft judgment: LLM-as-judge
```

Putting the hard checks first means a malformed answer fails in microseconds,
before any LLM tokens are spent.

## Why not AssertJ / Hamcrest?

`ragunit-core` has **zero production dependencies** — that is a supply-chain
guarantee, not an accident. The four helpers cover the common hard checks
without pulling in an assertion framework. For anything fancier, extract
`answer.text()` and use your test framework of choice alongside RAGUnit.
