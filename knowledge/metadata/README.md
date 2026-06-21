# MEKS Metadata Standard

Every canonical topic begins with YAML front matter.

| Field | Allowed value |
|---|---|
| `id` | Stable `meks-<domain>-<topic>` identifier |
| `status` | `planned`, `active`, `validated`, `operational`, `needs-revision`, `retired` |
| `difficulty` | `beginner`, `intermediate`, `advanced`, `progressive` |
| `current_level`, `target_level` | `beginner`, `intermediate`, `senior`, `staff`, `principal`, `architect` |
| `completion_percentage` | Integer from 0 to 100 |
| `confidence` | `low`, `medium`, `high` |
| readiness fields | Boolean |
| `owners` | Repository identities or team names |
| `last_reviewed` | ISO-8601 date or `null` |

## Progress Semantics

Completion measures coverage, not mastery. Confidence measures self-assessment,
not correctness. Interview readiness and production readiness require separate
evidence. A topic may be interview-ready but not production-ready, or the
reverse.

## Review Cadence

- Active technology topics: every six months.
- Security, compliance, provider, and AI-model guidance: every three months.
- Stable principles: annually or after a major incident.
- MarketMind module topics: after material architecture or operational change.
