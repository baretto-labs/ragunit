# Changelog

All notable changes to RAGUnit are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Prompt versioning rule: judge prompt wording is never changed in place.
A wording change ships as a new `JudgePromptLibrary` constant (`_V2`, `_V3`…)
and is recorded here, so scores stay comparable across library versions.

## [Unreleased]

### Added
- `timeout(Duration)` on `OllamaJudge.Builder` and `OpenAiCompatibleJudge.Builder`, a
  `HttpJudge` constructor overload taking the timeout, and the public constant
  `HttpJudge.DEFAULT_TIMEOUT` (60 s — the previous hardcoded value, unchanged).

### Fixed
- The request timeout was a private constant of `HttpJudge`, so a judge could not be
  given more than 60 seconds. Judging a code-retrieval benchmark with `qwen2.5:14b` on a
  laptop timed out on 51 calls out of 60 — the local-first path the library recommends
  first. A call that exceeds the timeout still raises `JudgeException`; it never scores
  zero, because an outage is not a measurement.

No prompt wording changed: scores stay comparable with 0.3.0.

## [0.3.0] - 2026-06

Local-first, your choice: RAGUnit still defaults to fully local judging with
Ollama, and now also evaluates against any OpenAI-compatible API.

### Added
- `ragunit-cloud` module with `OpenAiCompatibleJudge`: one judge for any
  OpenAI-compatible Chat Completions API — OpenAI, Azure OpenAI, Groq, Together,
  OpenRouter, Fireworks, local servers (vLLM, LM Studio), and Anthropic Claude /
  Google Gemini via their OpenAI-compatible endpoints. Configured by
  `baseUrl` + `model` + optional `apiKey` (Bearer; omitted for keyless local
  servers). Zero dependencies beyond `ragunit-core` (JDK HttpClient only).
- `HttpJudge`: public abstract base in `ragunit-core` holding all
  provider-independent machinery (prompts, parsing, query dispatch, HTTP). A new
  provider supplies four wire methods (endpoint, request body, auth headers,
  response parsing) — roughly thirty lines. Documented extension point.

### Changed
- `OllamaJudge` now extends `HttpJudge`; its public API (constructors, builder)
  is unchanged. Response parsing hardened with an escape-aware string extractor
  (`extractJsonString`) shared across providers.
- Positioning: "100% local" → "local-first, your choice". Local Ollama remains
  the default and the privacy/EU-AI-Act story; cloud is opt-in.

## [0.2.1] - 2026-06

Same feature set as 0.2.0 (which was never consumable: the JitPack build
failed before the Maven Wrapper was added). Use this version.

### Fixed
- JitPack build: added Maven Wrapper 3.9.9 (JitPack's default Maven 3.6.3
  failed the enforcer rule requiring Maven ≥ 3.9).

## [0.2.0] - 2026-06

Foundation milestone: RAGUnit becomes a credible measurement tool for
LLM-as-a-judge — visible prompts, variance control, structured results,
and a generic (non-RAG-shaped) evaluation API.

### Added
- Deterministic assertion helpers on `AnswerAssert`: `contains(...)`,
  `containsAll(...)`, `matches(regex)`, `hasMinLength(n)` — hard checks that
  run without any LLM call and chain freely with the judged assertions.
  New `RagAssert.assertThatAnswer(String)` overload for plain-text outputs.
  No external assertion dependency added. (T2.2)
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
