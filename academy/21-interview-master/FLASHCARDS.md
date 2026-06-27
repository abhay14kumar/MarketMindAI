# MarketMind AI Flashcards

## Java and Spring

### Question
Why does MarketMind use interfaces such as `Downloader`, `Parser`, and `VectorStore`?

### Answer
They define outbound ports so application services depend on capabilities, not infrastructure implementations.

### Key points
- Enables testing with fakes.
- Keeps Qdrant/Ollama/HTTP details outside core workflows.
- Supports future provider replacement.

### Interview tip
Say “port” and “adapter” only after explaining the practical benefit.

---

## Hexagonal Architecture

### Question
What problem does Hexagonal Architecture solve in MarketMind?

### Answer
It protects business workflows from external systems like HTTP sources, PostgreSQL, Qdrant, Ollama, and local file storage.

### Key points
- Controllers are inbound adapters.
- JDBC/Qdrant/Ollama clients are outbound adapters.
- Application services orchestrate use cases.

### Interview tip
Use one concrete class pair, for example `VectorStore` and `QdrantVectorStore`.

---

## Discovery

### Question
Why can discovery complete successfully with zero documents?

### Answer
The source may be reachable, but the fetched HTML may contain no direct PDF links. Dynamic or protected pages often require source-specific connectors.

### Key points
- Not every zero-result run is a failure.
- Diagnostics should show HTTP status, links scanned, and PDF links found.
- NSE generic crawling is expected to be limited.

### Interview tip
This is a strong production maturity answer: distinguish technical success from business outcome.

---

## Pipeline

### Question
Why does MarketMind need a pipeline orchestrator?

### Answer
Document processing has dependent stages. The orchestrator records status, retries, duration, and events so the system is reliable and visible.

### Key points
- Avoids manual sequencing.
- Supports retry.
- Makes background work observable.

### Interview tip
Mention idempotency when discussing retries.

---

## RAG

### Question
Why not ask the LLM directly?

### Answer
The LLM does not know the latest downloaded MarketMind documents. RAG retrieves relevant document chunks and gives the model grounded context.

### Key points
- Reduces hallucination.
- Enables citations.
- Depends on extraction, chunking, embedding, and retrieval quality.

### Interview tip
Never claim RAG eliminates hallucination; say it reduces risk when implemented well.

---

## Observability

### Question
What is the purpose of `X-Correlation-Id`?

### Answer
It ties a user request to logs, errors, and background processing so engineers can trace what happened across components.

### Key points
- Reuse client-provided ID if present.
- Generate UUID otherwise.
- Store in MDC.
- Return in response header.

### Interview tip
Give the incident debugging story, not just the definition.

---

## Source Intelligence

### Question
Why introduce `SourceConnector` instead of using crawlers directly?

### Answer
Different official sources behave differently. Connectors let MarketMind select source-aware behavior, track capabilities, and prioritize official sources.

### Key points
- NSE/BSE/SEBI/RBI can evolve independently.
- Generic REST/RSS/HTML support remains available.
- Trust and capability metadata become first-class.

### Interview tip
This is an architecture evolution story from generic implementation to enterprise platform.

