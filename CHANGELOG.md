# Changelog

All notable changes to RAGUnit are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Prompt versioning rule: judge prompt wording is never changed in place.
A wording change ships as a new `JudgePromptLibrary` constant (`_V2`, `_V3`…)
and is recorded here, so scores stay comparable across library versions.

## [Unreleased]

### Added
- Generic evaluation API: `Judge.evaluate(JudgeQuery)` returns a `JudgeResult`.
  `JudgeQuery` (builder-built) carries a `Criterion` — the named evaluation
  question — plus arbitrary named inputs, so any (input / output / reference)
  triplet can be judged, not only RAG. `MetricType` implements `Criterion`;
  queries with a built-in metric criterion are dispatched to the typed
  `RagJudge` methods automatically (custom RagJudge implementations get query
  support for free). `RagAssert` now constructs `JudgeQuery` objects under the
  hood; its public API is unchanged. New example:
  `SummaryJudgeQueryExampleTest` judges a summary against a source and a
  reference with ad-hoc criteria. (T2.1)
- `JudgeResult`: public record exposing `score`, `justification`, `promptUsed`,
  `rawResponse`, and `model` for one judge evaluation. Retrieved via
  `lastJudgeResult()` on `AnswerAssert` and `ContextAssert` (also populated when
  the assertion failed). (T1.3)
- `Verdict` now optionally carries the judge exchange: `promptUsed` and
  `rawResponse`, populated by `OllamaJudge` on every evaluation. The
  v0.1 five-argument constructor and `Verdict.of(...)` are unchanged. (T1.3)
- `AssertionError` messages now include the judge's justification (first
  500 characters), so a failing CI run is diagnosable without re-running. (T1.3)
- Variance control on assertions: `withRuns(n)` runs the judge n times and asserts
  on the mean score; `withMaxStddev(x)` (default 0.15) additionally fails the
  assertion when the judge is too unstable, with a failure message that
  distinguishes "score below threshold" from "judge too unstable". Available on
  both `AnswerAssert` and `ContextAssert`. (T1.2)
- `ScoreStatistics`: public record exposing mean, population stddev, and run
  count over repeated judge runs. (T1.2)
- `OllamaJudge` now sends `temperature: 0` by default (recommended setting for
  reproducible judging); configurable via `builder().temperature(x)`. (T1.2)
- `JudgePromptLibrary`: every built-in judge prompt is now a public, versioned
  `JudgePromptTemplate` constant (one per `MetricType`), plus `defaults()` exposing
  the exact map a judge uses when nothing is overridden. (T1.1)
- `OllamaJudge.builder()`: fluent configuration with per-metric prompt overrides —
  `faithfulnessPrompt(...)`, `answerRelevancyPrompt(...)`, `factualCorrectnessPrompt(...)`,
  and the generic `prompt(MetricType, JudgePromptTemplate)`. (T1.1)
- `MetricType`: new entries `CONTEXT_REJECTION`, `CONTEXT_PROMPT_INJECTION`,
  `CONTEXT_PII_LEAK` so the context-side variants of rejection, injection, and PII
  metrics can be overridden independently of the answer-side ones. (T1.1)
- `PromptContext`: now carries `reference` (`Optional<ReferenceAnswer>`) and
  `trajectory` (`List<ToolCall>`), with factories `forContextRecall`,
  `forFactualCorrectness`, and `forToolTrajectory`. (T1.1)

### Changed
- Custom `JudgePromptTemplate` overrides are now honored for **all** metrics
  (previously only RETRIEVAL, GENERATION, and FACTUAL_CORRECTNESS consulted the
  template map). (T1.1)
- `FACTUAL_CORRECTNESS` custom templates now receive the `ReferenceAnswer` via
  `PromptContext.reference()` (previously rendered with no reference). (T1.1)

### Prompt versions
- All metric prompts introduced at **V1** (`RETRIEVAL_V1`, `FAITHFULNESS_V1`,
  `ANSWER_RELEVANCY_V1`, `CONTEXT_PRECISION_V1`, `CONTEXT_RECALL_V1`,
  `FACTUAL_CORRECTNESS_V1`, `CONTEXT_REJECTION_V1`, `REJECTION_V1`,
  `CONTEXT_PROMPT_INJECTION_V1`, `PROMPT_INJECTION_V1`, `CONTEXT_PII_LEAK_V1`,
  `PII_LEAK_V1`, `TOOL_TRAJECTORY_V1`). Wording identical to the v0.1 built-in
  prompts — scores are comparable with v0.1.

## [0.1] - 2026-06

Initial release: `ragunit-core` (domain records, `RagJudge` + `OllamaJudge`,
`RagAssert` fluent API, JSON reporter, testset generation) and `ragunit-embedding`.
