# Ollama

`OllamaJudge` is the default implementation — zero extra dependencies, runs entirely on your machine.

---

## Setup

**1. Install Ollama:**

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh
```

**2. Pull a model:**

```bash
ollama pull qwen2.5:14b
```

**3. Start the server:**

```bash
ollama serve   # runs on localhost:11434
```

**4. Add the dependency:**

```xml
<dependency>
  <groupId>org.ragunit</groupId>
  <artifactId>ragunit-core</artifactId>
  <version>0.1</version>
  <scope>test</scope>
</dependency>
```

---

## Usage

```java
// Default: localhost:11434
RagJudge judge = new OllamaJudge("qwen2.5:14b");

// Custom host/port
RagJudge judge = new OllamaJudge("qwen2.5:14b", "192.168.1.10", 11434);

// With custom prompts
RagJudge judge = new OllamaJudge("qwen2.5:14b",
    Map.of(MetricType.GENERATION, myCustomTemplate));
```

---

## Recommended models

| Use case | Model | RAM required |
|---|---|---|
| All metrics | `qwen2.5:14b` | ~9 GB |
| Fast CI | `qwen2.5:7b` | ~5 GB |
| Embeddings | `nomic-embed-text` | ~1 GB |

!!! warning
    Models below 7B are not recommended for RAG evaluation — they produce inconsistent JSON
    and unreliable claim decompositions.

---

## Docker / CI

```yaml
# docker-compose.yml
services:
  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_MODELS=qwen2.5:14b

volumes:
  ollama_data:
```

In CI, pull the model as a setup step:

```yaml
- name: Pull Ollama model
  run: ollama pull qwen2.5:14b
```
