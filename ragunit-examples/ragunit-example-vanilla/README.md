# ragunit-example-vanilla

Minimal RAGUnit example: **plain Java 17 + Ollama, no framework**.

Demonstrates:
- `SimpleRagEvaluationTest` — evaluate faithfulness and context precision on a fixed pipeline
- `TestsetGenerationExample` — generate a synthetic testset from 3 documents and evaluate each case

## Prerequisites

1. [Install Ollama](https://ollama.ai/download)
2. Pull the model:
   ```bash
   ollama pull qwen2.5:14b
   ```
3. Ollama must be running on `localhost:11434`

## Run

```bash
# From the repo root — examples require Ollama; they are tagged @RagTest and skipped by default
mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval test

# Run a single example class
mvn -pl ragunit-examples/ragunit-example-vanilla -Dgroups=rag-eval -Dtest=SimpleRagEvaluationTest test
```
