# Custom Adapters

RAGUnit's evaluation logic lives entirely in `ragunit-core` — zero framework dependencies.
`OllamaJudge` is the built-in implementation. If your stack uses Spring AI, LangChain4j,
or any other `ChatModel`, you can implement `RagJudge` yourself in ~50 lines.

---

## How it works

`RagJudge` is a plain Java interface. Each method receives domain objects, sends a prompt to
an LLM, and returns a `Verdict` (or `FactualCorrectnessVerdict` for factual correctness).

You build the prompt, call your framework's chat API, and parse the response.
RAGUnit provides `VerdictParser` to parse the structured JSON response.

---

## Minimal example — Spring AI

```java
import org.ragunit.core.domain.*;
import org.ragunit.core.judge.*;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

public final class SpringAiJudge implements RagJudge {

    private final ChatClient chatClient;

    public SpringAiJudge(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Verdict evaluateGeneration(Question question, List<Document> context, Answer answer) {
        String ctx = context.stream()
                .map(d -> "- " + d.content())
                .collect(java.util.stream.Collectors.joining("\n"));
        String prompt = """
                You are a RAG evaluation judge.

                Question: %s
                Context:
                %s
                Answer: %s

                Reply ONLY with valid JSON:
                {"score": <0.0-1.0>, "rationale": "<one sentence>", "statements": []}
                """.formatted(question.text(), ctx, answer.text());

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return VerdictParser.parse(response, "spring-ai");
    }

    // Implement remaining methods the same way.
    // Delegate to OllamaJudge for methods you don't need to customise:
    @Override
    public Verdict evaluateRetrieval(Question q, List<Document> c) {
        throw new UnsupportedOperationException("not implemented");
    }
    // ... other methods
}
```

---

## Minimal example — LangChain4j

```java
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.ragunit.core.domain.*;
import org.ragunit.core.judge.*;
import java.util.List;

public final class LangChain4jJudge implements RagJudge {

    private final ChatLanguageModel model;

    public LangChain4jJudge(ChatLanguageModel model) {
        this.model = model;
    }

    @Override
    public Verdict evaluateGeneration(Question question, List<Document> context, Answer answer) {
        String ctx = context.stream()
                .map(d -> "- " + d.content())
                .collect(java.util.stream.Collectors.joining("\n"));
        String prompt = """
                You are a RAG evaluation judge.

                Question: %s
                Context:
                %s
                Answer: %s

                Reply ONLY with valid JSON:
                {"score": <0.0-1.0>, "rationale": "<one sentence>", "statements": []}
                """.formatted(question.text(), ctx, answer.text());

        String response = model.generate(prompt);
        return VerdictParser.parse(response, "langchain4j");
    }

    // Implement remaining methods the same way.
}
```

---

## Tips

- Use `VerdictParser.parse(response, modelName)` for standard verdicts.
- Use `VerdictParser.parseFactualCorrectness(response, modelName)` for `evaluateFactualCorrectness`.
- Use `VerdictParser.parse(response, modelName, context)` for `evaluateContextPrecision` (includes chunk verdicts).
- Keep each method ≤ 20 lines — delegate prompt-building to private helpers.
- The adapter lives in your own codebase, not in `ragunit-core` — no PR needed.
