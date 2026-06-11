# Integrations

RAGUnit ships `OllamaJudge` out of the box — zero extra dependencies, 100% local.

| Module | Class | Backend | Extra dependencies |
|---|---|---|---|
| `ragunit-core` | `OllamaJudge` | Local Ollama | None |

Need Spring AI, LangChain4j, or another framework? Implement `RagJudge` yourself — it's ~50 lines.
See [Custom Adapters](adapters.md) for a step-by-step example.

---

## Supported features

- All 10 metrics (Faithfulness, AnswerRelevancy, ContextPrecision, ContextRecall, FactualCorrectness, Rejection, PromptInjection, PIILeak, ToolTrajectory, and Retrieval)
- Custom prompts via `JudgePromptTemplate`
- The same `Verdict` and `FactualCorrectnessVerdict` return types
