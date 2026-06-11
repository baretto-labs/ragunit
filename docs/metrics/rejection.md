# Rejection

**Module**: `ragunit-core`
**Requires**: `Question`, `Context`, `Answer` (or Context-only variant), `RagJudge`
**Returns**: `Verdict`

---

## What it measures

Rejection evaluates whether a generator's refusal to answer was **justified** given the context
and question. A well-behaved RAG system should refuse when the context is insufficient —
and RAGUnit verifies that refusals are correct, not avoidant.

Two variants are available:

| Variant | API | What it checks |
|---|---|---|
| **Answer-level** | `AnswerAssert.correctlyRefusedToAnswer(t)` | Did the answer correctly decline? And was the context truly insufficient? |
| **Context-level** | `ContextAssert.correctlyRefusedToAnswer(t)` | Is the context insufficient to answer the question? |

---

## Usage

=== "Answer-level"

    ```java
    Question question = new Question("What is the CEO's salary?");
    List<Document> context = List.of(
        new Document("The company was founded in 2010 and is headquartered in Paris.")
    );
    Answer answer = new Answer("I don't have enough information to answer that question.");

    RagAssert.assertThatAnswer(answer)
             .givenContext(context)
             .forQuestion(question)
             .evaluatedBy(judge)
             .correctlyRefusedToAnswer(0.80);
    ```

=== "Context-level"

    ```java
    // Test that the context would justify a refusal, even without an answer
    RagAssert.assertThatContext(context)
             .forQuestion(question)
             .evaluatedBy(judge)
             .correctlyRefusedToAnswer(0.80);
    ```

---

## When to use it

- Testing that your RAG system refuses appropriately when documents don't contain the answer
- Validating that refusals are not over-triggered (high recall systems that refuse too often)
- Red-teaming: ensuring the system refuses on out-of-scope questions

---

## Score interpretation

A **high** score means:
- The context was indeed insufficient (context-level), **or**
- The answer correctly refused AND the refusal was justified (answer-level)

A **low** score means:
- The system refused when it had enough context (false refusal), **or**
- The system attempted to answer when it should have refused
