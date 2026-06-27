# MarketMind Comparative Case Studies

These case studies compare implemented MarketMind capabilities with well-known systems. They are intentionally conceptual and do not claim proprietary internal details.

## Bloomberg-style financial intelligence

| Dimension | Bloomberg-like platform | MarketMind today |
|---|---|---|
| Data sources | Many official and licensed feeds | Source registry, source intelligence, discovery |
| Documents | Filings, reports, news, analytics | Official document discovery/download/extraction |
| Search | Structured and semantic search | Qdrant-backed RAG over document chunks |
| Operations | Enterprise monitoring | Local Grafana/Loki/Promtail and correlation IDs |

Learning: financial intelligence systems are source-governance systems before they are AI systems.

## Zerodha-style market feeds

| Dimension | Brokerage platform | MarketMind today |
|---|---|---|
| Portfolio | User holdings | Zerodha XLSX import |
| Prices | Real-time/near-real-time feeds | Real market price refresh provider abstraction |
| Reliability | Provider fallback and freshness | price provider status and refresh jobs |

Learning: market data needs freshness, provider health, and clear user-facing status.

## Perplexity-style RAG

| Dimension | AI answer engine | MarketMind today |
|---|---|---|
| Retrieval | Web/source retrieval | Qdrant over extracted financial documents |
| Grounding | Citations | citation DTOs and retrieved chunks |
| Freshness | Continuous crawling | Discovery + pipeline, not full web crawling |
| Model | Hosted LLMs | Local Ollama abstraction |

Learning: RAG quality depends more on ingestion/retrieval discipline than prompt cleverness alone.

## GitHub Copilot-style AI assistant

| Dimension | Copilot-like system | MarketMind today |
|---|---|---|
| Context | Codebase snippets | Financial document chunks |
| Retrieval | Code embeddings | Document embeddings |
| Output | Code suggestions | Answers with citations |

Learning: the same retrieve-then-generate pattern can serve very different domains.

## Datadog-style logging

| Dimension | Observability product | MarketMind today |
|---|---|---|
| Log ingestion | Agents/collectors | Promtail |
| Log store | Managed backend | Loki |
| Query UI | Dashboards/search | Grafana |
| Correlation | Trace/log IDs | `correlationId` in MDC |

Learning: searchable logs change support from “scroll terminal” to “trace incident.”

## Netflix-style observability culture

| Dimension | Mature production org | MarketMind today |
|---|---|---|
| Failure mindset | Failures expected | retries and stage statuses |
| Visibility | Events, dashboards, alerts | pipeline events, notifications, dashboard |
| Resilience | Bulkheads/circuit breakers | bounded retries exist; circuit breakers future |

Learning: production-grade systems explain failure and recovery paths.

