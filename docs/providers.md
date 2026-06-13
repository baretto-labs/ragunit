# Providers

RAGUnit is **local-first**: the default judge, `OllamaJudge`, runs entirely on your
machine — no data leaves your infrastructure, which is what makes the local mode a
fit for regulated or sensitive evaluation. But you are not locked in. When you want a
hosted frontier model as the judge, or you already run an OpenAI-compatible gateway,
one judge covers the whole ecosystem.

| Judge | Module | Backend |
|---|---|---|
| `OllamaJudge` | `ragunit-core` | Local Ollama — private by default |
| `OpenAiCompatibleJudge` | `ragunit-cloud` | **Any OpenAI-compatible Chat Completions API** |

---

## OllamaJudge — local-first (recommended default)

```java
RagJudge judge = OllamaJudge.builder().model("qwen2.5:14b").build();
```

Nothing leaves the host running Ollama. This is the right default for sensitive data
and the only mode that keeps you fully in-house for EU AI Act purposes.

---

## OpenAiCompatibleJudge — one judge, every OpenAI-compatible API

`OpenAiCompatibleJudge` talks to `POST {baseUrl}/chat/completions`. **Any** provider
that speaks the OpenAI Chat Completions format works — you only change the base URL,
the model name, and the API key.

Add the module:

```xml
<dependency>
  <groupId>com.github.baretto-labs.ragunit</groupId>
  <artifactId>ragunit-cloud</artifactId>
  <version>v0.3.0</version>
  <scope>test</scope>
</dependency>
```

```java
OpenAiCompatibleJudge judge = OpenAiCompatibleJudge.builder()
        .baseUrl("https://api.openai.com/v1")
        .apiKey(System.getenv("OPENAI_API_KEY"))   // never hard-code it
        .model("gpt-4o-mini")
        .build();
```

That `judge` is a full `RagJudge`: every metric, `withRuns(n)`, custom prompts,
`JudgeResult`, and `JudgeQuery` work exactly as with the local judge.

### Configuration per provider

It works with anything OpenAI-compatible. A few common base URLs:

| Provider | `baseUrl` | API key env (example) |
|---|---|---|
| OpenAI | `https://api.openai.com/v1` | `OPENAI_API_KEY` |
| Azure OpenAI | `https://{resource}.openai.azure.com/openai/deployments/{deployment}` | `AZURE_OPENAI_KEY` |
| Groq | `https://api.groq.com/openai/v1` | `GROQ_API_KEY` |
| Together | `https://api.together.xyz/v1` | `TOGETHER_API_KEY` |
| OpenRouter | `https://openrouter.ai/api/v1` | `OPENROUTER_API_KEY` |
| Fireworks | `https://api.fireworks.ai/inference/v1` | `FIREWORKS_API_KEY` |
| **Anthropic Claude** | `https://api.anthropic.com/v1` | `ANTHROPIC_API_KEY` |
| **Google Gemini** | `https://generativelanguage.googleapis.com/v1beta/openai` | `GEMINI_API_KEY` |
| Local (vLLM / LM Studio / llama.cpp) | `http://localhost:8000/v1` | *(usually none)* |

Claude and Gemini are reached through their **OpenAI-compatible endpoints** — so a
single `OpenAiCompatibleJudge` covers them too. If a provider needs no key (most local
servers), simply omit `.apiKey(...)` and no `Authorization` header is sent.

```java
// Local OpenAI-compatible server (vLLM, LM Studio…) — no key
OpenAiCompatibleJudge local = OpenAiCompatibleJudge.builder()
        .baseUrl("http://localhost:8000/v1")
        .model("Qwen/Qwen2.5-14B-Instruct")
        .build();
```

---

## Cost & variance with cloud judges

`withRuns(n)` issues **n** API calls per assertion — that is `n ×` the tokens and
counts against rate limits. Two practical notes:

- Frontier models at temperature 0 are usually **more stable** than a local 14B model,
  so you often need fewer runs for the same standard deviation.
- Keep deterministic hard checks (`contains`, `matches`…) **before** the judged
  assertion so malformed outputs fail for free, before any paid call.

---

## Choosing

- **Sensitive / regulated data, reproducible, free** → `OllamaJudge` (local).
- **Strongest possible judge, or you already pay for an LLM API** → `OpenAiCompatibleJudge`.
- **Another backend entirely** → implement `RagJudge`, or extend `HttpJudge` and supply
  four wire methods (endpoint URL, request body, auth headers, response parsing).
  That is exactly how both built-in judges are built.

The prompt library, variance control, and `JudgeResult` sit above the judge — they
behave identically whichever backend you pick.
