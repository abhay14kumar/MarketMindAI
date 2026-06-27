# Background Work Visibility

Discovery and ingestion are intentionally separate:

- **Discovery** scans a trusted source and stores document-link metadata.
- **Ingestion** downloads an approved document and runs extraction, chunking,
  embedding, indexing, summary generation, and AI-readiness stages.

A discovery job can complete successfully with zero results. This means the
source was processed without an infrastructure failure, but no direct PDF
links were present in the fetched HTML. The UI shows this as an amber
`NO_RESULTS` state with crawler diagnostics and recommended next steps.

The Pipeline Monitor exposes the current stage, progress, attempts, duration,
events, errors, and retry action. It refreshes every five seconds and publishes
completion or failure events to the notification center.
