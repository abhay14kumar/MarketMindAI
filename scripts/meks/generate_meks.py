#!/usr/bin/env python3
"""Generate the lightweight MarketMind Engineering Knowledge System skeleton."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]

TOPICS = {
    "engineering-foundation": [
        "engineering-foundation", "design-patterns", "clean-architecture",
        "hexagonal-architecture", "domain-driven-design", "cqrs",
        "event-sourcing", "low-level-design", "software-architecture",
        "system-design",
    ],
    "java-runtime": [
        "java", "jvm", "concurrency", "parallelism", "jvm-performance",
    ],
    "spring": ["spring", "spring-boot", "spring-security"],
    "apis-identity": [
        "rest", "graphql", "openapi", "http", "tls", "oauth2", "oidc", "jwt",
    ],
    "distributed-systems": [
        "microservices", "distributed-systems", "event-driven-architecture",
        "kafka", "rabbitmq", "caching", "scalability", "resilience",
    ],
    "data": ["database", "postgresql", "mongodb", "redis", "flyway"],
    "frontend": ["frontend-engineering", "react", "typescript"],
    "platform-cloud": [
        "docker", "kubernetes", "helm", "linux", "networking", "cloud",
        "aws", "azure", "gcp", "terraform",
    ],
    "delivery": ["ci-cd", "github-actions", "git"],
    "observability-production": [
        "observability", "logging", "tracing", "metrics", "prometheus",
        "grafana", "opentelemetry", "performance-engineering",
        "production-engineering", "incident-management", "postmortems",
        "chaos-engineering",
    ],
    "testing-security": [
        "testing", "junit", "mockito", "integration-testing",
        "testcontainers", "contract-testing", "performance-testing",
        "security-engineering", "security-reviews", "threat-modeling",
    ],
    "ai": [
        "artificial-intelligence", "machine-learning", "deep-learning",
        "transformers", "large-language-models", "prompt-engineering",
        "embeddings", "vector-databases", "qdrant", "rag", "ai-agents",
        "evaluation", "hallucination", "ollama", "llama-3",
    ],
    "finance": [
        "finance", "stock-market", "mutual-funds", "etf", "portfolio",
        "trading", "market-data", "nse", "bse", "sebi",
        "financial-statements", "valuation",
    ],
    "leadership": [
        "leadership", "communication", "architecture-reviews", "code-reviews",
        "engineering-management",
    ],
    "interviews": [
        "interview-preparation", "behavioral-interviews",
        "principal-engineer-interviews", "architecture-interviews",
    ],
    "product-development": ["product-development"],
    "marketmind-modules": [
        "marketmind-modules", "downloader", "scheduler", "portfolio-module",
        "source-registry", "document-processing", "embedding-pipeline",
        "rag-module", "recommendation-engine", "notification-engine",
        "dashboard", "analytics",
    ],
}

DISPLAY_OVERRIDES = {
    "ai": "AI",
    "api": "API",
    "aws": "AWS",
    "bse": "BSE",
    "ci-cd": "CI/CD",
    "cqrs": "CQRS",
    "ddd": "DDD",
    "etf": "ETF",
    "gcp": "GCP",
    "graphql": "GraphQL",
    "http": "HTTP",
    "jvm": "JVM",
    "jwt": "JWT",
    "nse": "NSE",
    "oauth2": "OAuth 2.0",
    "oidc": "OIDC",
    "rag": "RAG",
    "rest": "REST",
    "sebi": "SEBI",
    "tls": "TLS",
}

DOMAIN_MARKETMIND = {
    "engineering-foundation": "Architecture, coding standards, and ADRs",
    "java-runtime": "Spring Boot backend runtime",
    "spring": "Backend services and APIs",
    "apis-identity": "Public and internal API contracts",
    "distributed-systems": "Schedulers, pipelines, providers, and future events",
    "data": "PostgreSQL, Redis, and Qdrant-backed workflows",
    "frontend": "React and TypeScript user experiences",
    "platform-cloud": "Docker, Kubernetes, and Helm deployment",
    "delivery": "Repository workflow and delivery automation",
    "observability-production": "Health, diagnostics, and production readiness",
    "testing-security": "Verification, trust boundaries, and secure delivery",
    "ai": "Document extraction, embeddings, retrieval, and AI research",
    "finance": "Portfolio intelligence and investment research",
    "leadership": "Technical governance and team operating practices",
    "interviews": "Project-based career preparation",
    "product-development": "MarketMind product discovery and delivery",
    "marketmind-modules": "Production module implementation",
}

GLOBAL_FOLDERS = [
    "knowledge", "academy", "architecture", "interview", "system-design",
    "production", "labs", "cheatsheets", "mindmaps", "flashcards", "quizzes",
    "assignments", "templates", "roadmaps", "certifications", "research",
    "notes", "glossary", "references", "decision-records", "best-practices",
    "patterns", "anti-patterns", "case-studies",
]

TEMPLATES = {
    "TOPIC_TEMPLATE.md": [
        "Overview", "Learning Path", "Prerequisites", "Difficulty",
        "Estimated Time", "Concept List", "Implementation Status",
        "Related Topics", "Interview Questions", "Principal Questions",
        "System Design Questions", "Production Scenarios", "Failure Scenarios",
        "Hands-on Labs", "MarketMind References", "Further Reading",
        "Revision Checklist", "Mastery Checklist", "Version History",
    ],
    "INTERVIEW_TEMPLATE.md": [
        "Competency", "Question", "What Good Looks Like", "Follow-ups",
        "Common Weak Answers", "MarketMind Evidence", "Scoring Rubric",
    ],
    "SYSTEM_DESIGN_TEMPLATE.md": [
        "Problem", "Requirements", "Constraints", "Capacity Model",
        "Architecture", "Data Model", "APIs and Events", "Failure Model",
        "Security", "Observability", "Trade-offs", "Evolution",
    ],
    "ARCHITECTURE_REVIEW_TEMPLATE.md": [
        "Decision Scope", "Context", "Quality Attributes", "Proposed Design",
        "Alternatives", "Risks", "Operational Readiness", "Review Outcome",
    ],
    "PRODUCTION_INCIDENT_TEMPLATE.md": [
        "Summary", "Impact", "Timeline", "Detection", "Response",
        "Contributing Conditions", "Recovery", "Evidence", "Follow-up",
    ],
    "CODE_REVIEW_TEMPLATE.md": [
        "Intent", "Correctness", "Design", "Security", "Data",
        "Failure Handling", "Tests", "Operations", "Review Decision",
    ],
    "POSTMORTEM_TEMPLATE.md": [
        "Executive Summary", "Customer Impact", "Timeline", "Root Cause",
        "Contributing Factors", "What Worked", "What Did Not",
        "Corrective Actions", "Learning",
    ],
    "TECHNOLOGY_EVALUATION_TEMPLATE.md": [
        "Problem", "Evaluation Criteria", "Candidates", "Evidence",
        "Security and Compliance", "Operations", "Cost", "Recommendation",
    ],
    "DECISION_RECORD_TEMPLATE.md": [
        "Status", "Context", "Decision", "Alternatives", "Consequences",
        "Validation", "Review Date",
    ],
    "FEATURE_DESIGN_TEMPLATE.md": [
        "Outcome", "Non-goals", "User Flow", "Requirements", "Architecture",
        "Data and APIs", "Security", "Failure Modes", "Test Plan", "Rollout",
    ],
    "LEARNING_NOTE_TEMPLATE.md": [
        "Question", "Mental Model", "Key Concepts", "Experiment",
        "Evidence", "Mistakes", "MarketMind Connection", "Next Revision",
    ],
    "DAILY_LEARNING_TEMPLATE.md": [
        "Focus", "What I Learned", "What I Built", "Evidence",
        "Open Questions", "Tomorrow",
    ],
    "WEEKLY_REVIEW_TEMPLATE.md": [
        "Outcomes", "Concept Progress", "Production Lessons",
        "Interview Practice", "Gaps", "Next Week",
    ],
    "MONTHLY_REVIEW_TEMPLATE.md": [
        "Capability Growth", "Delivered Evidence", "Skill Matrix Changes",
        "Architecture Lessons", "Career Progress", "Next Month",
    ],
}

CAREER_PATHS = [
    "backend-engineer", "senior-backend-engineer", "staff-engineer",
    "principal-engineer", "solution-architect", "engineering-manager",
    "ai-engineer", "ai-architect",
]

STUDY_PATHS = ["30-days", "90-days", "180-days", "365-days", "2-years"]


def title(slug: str) -> str:
    if slug in DISPLAY_OVERRIDES:
        return DISPLAY_OVERRIDES[slug]
    words = []
    for word in slug.split("-"):
        words.append(DISPLAY_OVERRIDES.get(word, word.capitalize()))
    return " ".join(words)


def write_if_missing(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_text(content, encoding="utf-8")


def topic_readme(domain: str, topic: str) -> str:
    topic_title = title(topic)
    domain_title = title(domain)
    return f"""---
id: meks-{domain}-{topic}
title: {topic_title}
domain: {domain_title}
status: planned
difficulty: progressive
estimated_time: TBD
current_level: beginner
target_level: principal
completion_percentage: 0
confidence: low
interview_ready: false
production_ready: false
needs_revision: false
hands_on_completed: false
owners: []
last_reviewed: null
---

# {topic_title}

> Canonical MEKS topic manifest. Expand incrementally when this capability is
> designed, implemented, operated, or reviewed in MarketMind AI.

## Overview

Define the purpose, vocabulary, scope, and engineering outcomes for
**{topic_title}**. Keep detailed teaching material in `academy/` and link it
here rather than duplicating it.

## Learning Path

| Level | Expected capability |
|---|---|
| Beginner | Explain core terminology and complete a guided exercise. |
| Intermediate | Implement a bounded feature and test normal behavior. |
| Senior | Design for failure, security, performance, and operations. |
| Staff | Define reusable patterns and guide cross-module adoption. |
| Principal | Set strategy, evaluate trade-offs, and govern evolution. |
| Architect | Connect product, organization, data, platform, and risk. |

## Prerequisites

- See the [Dependency Graph](../../../graphs/DEPENDENCY_GRAPH.md).
- Add topic-specific prerequisites when this manifest enters `active` status.

## Concept List

- [ ] Fundamentals and vocabulary
- [ ] Design trade-offs
- [ ] Implementation patterns
- [ ] Testing and verification
- [ ] Security and failure modes
- [ ] Performance and scalability
- [ ] Operations and observability

## Implementation Status

| Field | Value |
|---|---|
| Status | Planned |
| MarketMind area | {DOMAIN_MARKETMIND[domain]} |
| Evidence | Add code, ADR, test, dashboard, or incident links |

## Related Topics

- [Master Index](../../../indexes/MASTER_INDEX.md)
- [Technology Index](../../../indexes/TECHNOLOGY_INDEX.md)
- [Knowledge Graph](../../../graphs/KNOWLEDGE_GRAPH.md)

## Interview Questions

- Explain the problem this topic solves and its principal trade-offs.
- Describe a production failure caused by misunderstanding this topic.

## Principal Questions

- When should MarketMind standardize this capability, and when should teams
  retain local autonomy?
- Which decisions are expensive to reverse?

## System Design Questions

- Design this capability for MarketMind at ten times current scale.
- Identify data, trust, ownership, and failure boundaries.

## Production Scenarios

- Define availability, latency, capacity, recovery, and observability needs.

## Failure Scenarios

- Document dependency failure, malformed input, overload, and unsafe defaults.

## Hands-on Labs

- Link a lab under `labs/` when implementation begins.

## MarketMind References

- [Architecture](../../../indexes/ARCHITECTURE_INDEX.md)
- [MarketMind Index](../../../indexes/MARKETMIND_INDEX.md)
- [Repository documentation](../../../../docs/)

## Further Reading

- Add primary documentation, standards, papers, and selected books.

## Revision Checklist

- [ ] Metadata is current.
- [ ] Links resolve.
- [ ] Product evidence is attached.
- [ ] Failure and security sections reflect production experience.

## Mastery Checklist

- [ ] I can explain this topic without framework-specific language.
- [ ] I can implement, test, operate, and troubleshoot it.
- [ ] I can compare alternatives and defend a decision.
- [ ] I can teach it and review another engineer's design.

## Version History

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-06-20 | Initial MEKS topic skeleton |
"""


def folder_readme(folder: str) -> str:
    label = title(folder)
    return f"""# {label}

This directory is part of the **MarketMind Engineering Knowledge System
(MEKS)**.

## Purpose

Store concise, cross-linked {label.lower()} assets. Detailed concepts should
reference canonical topic manifests under [`knowledge/topics`](../knowledge/topics/)
and avoid duplicating academy or product documentation.

## Contribution Rules

1. Start from an appropriate file in [`templates/`](../templates/).
2. Link evidence from MarketMind AI code, tests, ADRs, or operations.
3. Record owner, status, review date, and related topics.
4. Prefer primary sources and mark time-sensitive guidance.
5. Update relevant indexes when adding an asset.
"""


def template_document(name: str, sections: list[str]) -> str:
    body = "\n".join(
        f"""## {section}

<!-- Add concise, evidence-backed content. Link canonical MEKS topics and
MarketMind implementation artifacts. -->
"""
        for section in sections
    )
    return f"""---
template: {name.removesuffix(".md").lower()}
version: 1.0
status: active
---

# {title(name.removesuffix(".md").lower())}

> Copy this file; do not edit the canonical template for one-off work.

{body}
"""


def career_path(slug: str) -> str:
    return f"""# {title(slug)} Career Path

## Target Outcome

Define the production capabilities, influence, and evidence expected for this
role. Progress is demonstrated through MarketMind AI outcomes, not course
completion alone.

## Capability Stages

| Stage | Focus | Required evidence |
|---|---|---|
| Foundation | Core language, data, APIs, and delivery | Guided labs and tested features |
| Independent | Own bounded production capabilities | Design, implementation, and operations |
| Cross-system | Resolve ambiguity across modules | Architecture reviews and incidents |
| Organizational | Improve standards and team leverage | Reusable patterns and mentorship |
| Strategic | Shape technical direction and risk | Multi-year decisions and measurable outcomes |

## Required Domains

- Select role-specific topics from the
  [Skill Matrix](../../knowledge/indexes/SKILL_MATRIX.md).
- Follow the [Dependency Graph](../../knowledge/graphs/DEPENDENCY_GRAPH.md).
- Attach evidence through the
  [Progress Tracker](../../knowledge/indexes/PROGRESS_TRACKER.md).

## MarketMind Capstone

Own a coherent capability from product outcome through architecture,
implementation, testing, deployment, observability, and operational review.

## Review Questions

- What decisions can the learner make independently?
- What is the largest failure domain they can safely own?
- How do they improve the effectiveness of other engineers?
"""


def study_path(slug: str) -> str:
    return f"""# {title(slug)} Study Path

## Purpose

Provide a time-boxed route through MEKS while using MarketMind AI as the
continuous implementation laboratory.

## Operating Rhythm

1. Select prerequisite topics from the dependency graph.
2. Learn one mental model.
3. Build or inspect one MarketMind vertical slice.
4. Produce evidence: test, diagram, ADR, benchmark, or review.
5. Practice explanation and interview follow-ups.
6. Update confidence and readiness metadata.

## Milestones

| Phase | Learning | Product evidence | Review |
|---|---|---|---|
| Orient | Map current capability and gaps | Repository walkthrough | Baseline skill matrix |
| Build | Complete selected topic paths | Working vertical slices | Weekly review |
| Operate | Study failures and runtime signals | Runbook or incident exercise | Production-readiness review |
| Integrate | Connect multiple domains | System design or architecture review | Principal-question panel |
| Consolidate | Teach and revise | Published note and capstone | Final progress update |

## Exit Criteria

- Required topics have evidence links.
- Hands-on, interview, and production readiness are assessed separately.
- Remaining gaps are recorded rather than hidden by a completion percentage.
"""


def main() -> None:
    for folder in GLOBAL_FOLDERS:
        write_if_missing(ROOT / folder / "README.md", folder_readme(folder))

    for domain, topics in TOPICS.items():
        domain_root = ROOT / "knowledge" / "topics" / domain
        links = "\n".join(
            f"- [{title(topic)}](./{topic}/README.md)" for topic in topics
        )
        write_if_missing(
            domain_root / "README.md",
            f"# {title(domain)} Topics\n\n{links}\n",
        )
        for topic in topics:
            write_if_missing(
                domain_root / topic / "README.md",
                topic_readme(domain, topic),
            )

    for name, sections in TEMPLATES.items():
        write_if_missing(
            ROOT / "templates" / name,
            template_document(name, sections),
        )

    for path in CAREER_PATHS:
        write_if_missing(
            ROOT / "roadmaps" / "career-paths" / f"{path}.md",
            career_path(path),
        )

    for path in STUDY_PATHS:
        write_if_missing(
            ROOT / "roadmaps" / "study-paths" / f"{path}.md",
            study_path(path),
        )


if __name__ == "__main__":
    main()
