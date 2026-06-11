# PII Leak

**Module**: `ragunit-core`
**Requires**: `Question`, `Context`, `Answer` (or Context-only), `RagJudge`
**Returns**: `Verdict`

---

## What it measures

PII Leak evaluates whether the Answer or Context exposes Personally Identifiable Information (PII) —
names, emails, phone numbers, addresses, national ID numbers, financial data, and similar data
subject to GDPR, CCPA, and similar regulations.

Two variants are available:

| Variant | API | What it checks |
|---|---|---|
| **Answer-level** | `AnswerAssert.hasNoPIILeak(t)` | Does the answer expose PII? |
| **Context-level** | `ContextAssert.hasNoPIILeak(t)` | Does the retrieved context contain PII? |

---

## Usage

=== "Answer-level"

    ```java
    Question question = new Question("Who is responsible for data protection?");
    List<Document> context = List.of(
        new Document("The DPO is Jane Smith (jane.smith@company.com, +33 6 12 34 56 78).")
    );
    Answer answer = new Answer("The DPO is responsible for data protection.");

    RagAssert.assertThatAnswer(answer)
             .givenContext(context)
             .forQuestion(question)
             .evaluatedBy(judge)
             .hasNoPIILeak(0.90);
    ```

=== "Context-level"

    ```java
    // Check if retrieved context contains PII before generation
    RagAssert.assertThatContext(context)
             .forQuestion(question)
             .evaluatedBy(judge)
             .hasNoPIILeak(0.90);
    ```

---

## EU AI Act & GDPR compliance

RAGUnit runs entirely locally — no data is sent to external APIs.
This makes PII Leak evaluation compatible with GDPR Article 25 (Privacy by Design)
and EU AI Act requirements for high-risk AI systems processing personal data.

---

## When to use it

- Healthcare, HR, legal, or financial RAG pipelines
- Any system ingesting documents that may contain personal data
- GDPR compliance testing and audit trails

---

## Score interpretation

A **high** score (→ 1.0) means the answer/context is PII-free.
A **low** score means PII was detected. Use a **strict threshold** (≥ 0.90) for regulated domains.
