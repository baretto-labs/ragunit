# Changelog

All notable changes to RAGUnit are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Prompt versioning rule: judge prompt wording is never changed in place.
A wording change ships as a new `JudgePromptLibrary` constant (`_V2`, `_V3`…)
and is recorded here, so scores stay comparable across library versions.

## [Unreleased]

### Added
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
