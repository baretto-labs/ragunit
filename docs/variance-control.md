# Variance Control

A single LLM-judge call has inherent variance — typically **±10–15%** on the score,
even at temperature 0. Without averaging, any score-based assertion is noise:
a test that passes at `0.86` today may fail at `0.79` tomorrow with no code change.

RAGUnit bakes variance control into the assertion API so the library, not the user,
is responsible for making scores reproducible.

---

## `withRuns(n)` — average over N judge runs

```java
RagAssert.assertThatAnswer(answer)
        .givenContext(context)
        .forQuestion(question)
        .evaluatedBy(judge)
        .withRuns(5)                  // call the judge 5 times
        .isFaithfulToContext(0.80);   // assert on the MEAN of the 5 scores
```

With `withRuns(n)`, every assertion:

1. calls the judge **n** times,
2. computes the **mean** and the **population standard deviation** of the scores,
3. fails if `mean < threshold` **or** `stddev > maxStddev`.

The failure message distinguishes the two cases:

- `Faithfulness mean score 0.72 over 5 runs is below threshold 0.8 (stddev 0.04)`
  → the pipeline really is below the bar.
- `Faithfulness: judge too unstable: stddev 0.2 over 5 runs exceeds max 0.15 (mean 0.7)`
  → the *measurement* is unreliable; fix the judge (better model, clearer prompt)
  before trusting any score.

Both `AnswerAssert` and `ContextAssert` support `withRuns(n)` / `withMaxStddev(x)`.

## `withMaxStddev(x)` — stability bound

The default bound is **0.15** (`ScoreStatistics.DEFAULT_MAX_STDDEV`). Tighten it for
metrics where you need high confidence:

```java
.withRuns(5)
.withMaxStddev(0.05)   // demand a very stable judge
.isFaithfulToContext(0.80);
```

A judge whose scores deviate more than the bound across identical runs is failed
explicitly rather than letting a lucky mean slip through.

---

## Temperature 0 — the recommended judge default

`OllamaJudge` sends `temperature: 0` by default. Zero is the recommended setting
for an LLM-as-judge: it minimizes run-to-run variance, making scores as
reproducible as the model allows.

Raise it only to probe judge stability:

```java
OllamaJudge judge = OllamaJudge.builder()
        .model("qwen2.5:14b")
        .temperature(0.7)   // deliberately noisy, e.g. to test withRuns()
        .build();
```

!!! note
    Temperature 0 does not make an LLM deterministic — GPU non-determinism and
    serving-side batching still produce variation. That is why `withRuns(n)`
    exists even with temperature 0.

---

## `seed(n)` — replay the same measurement

Temperature 0 makes the sampling greedy; it does not fix the seed Ollama draws for each
request. On `qwen2.5:14b`, a retrieval query judged four times shows what that costs:

| Configuration | Two consecutive runs |
|---|---|
| temperature 0, no seed | identical (0.85, then 0.85) |
| temperature 0.8, `seed(7)` | identical, down to the wording of the rationale |
| temperature 0.8, no seed | **1.0, then 0.95** |

So at temperature 0 the seed buys nothing observable — greedy sampling is already stable
on a single host. It earns its place the moment the temperature is not zero, which is
exactly the setting `withRuns(n)` is meant to probe, and it makes the judge configuration
explicit rather than implicit. Set a seed when a score has to be **comparable to itself
over time** — a baseline captured before a change and re-measured after it:

```java
OllamaJudge judge = OllamaJudge.builder()
        .model("qwen2.5:14b")
        .seed(42)           // same prompt, same model, same seed → same verdict
        .build();
```

No seed is sent unless you set one, so the default behaviour is unchanged.

A seed only holds the judge still. It does not make two models comparable, and it does
not survive a model upgrade or a prompt edit: both invalidate a baseline exactly as
changing the seed would. Record the model, the prompt version, and the seed alongside
every score you intend to compare later.

!!! warning
    A seed narrows the variance, it does not remove it — the GPU and batching caveat
    above still applies. Keep `withRuns(n)` for anything that gates a decision.

---

## How many runs?

| Runs | Use case |
|---|---|
| 1 (default) | Smoke tests, fast CI — accept the noise |
| 3 | Good cost/stability trade-off for nightly evaluation |
| 5+ | Gating decisions (release criteria, regression detection) |

Each run is one LLM call per assertion — budget accordingly.
