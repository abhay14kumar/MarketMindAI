# MarketMind AI Assignments and Labs

These assignments turn the academy into daily practice.

## Daily revision loop

1. Pick one module.
2. Read its overview and code walkthrough.
3. Draw the request flow from memory.
4. Answer five interview questions.
5. Run or inspect one test.
6. Write one improvement idea.

## Lab 1: Trace a document through the system

### Goal

Explain every state transition from discovered document to AI-ready document.

### Output

- Mermaid sequence diagram.
- List of tables touched.
- List of services/classes involved.
- Failure points and retry strategy.

## Lab 2: Debug zero-result discovery

### Goal

Show that `COMPLETED` with zero results can be a valid technical outcome.

### Output

- Explain source reachability vs PDF discovery.
- Inspect crawler diagnostics.
- Write a user-facing recommendation.
- Propose a connector improvement.

## Lab 3: Design a new source connector

### Goal

Extend the connector architecture without breaking discovery.

### Output

- Source capability list.
- Connector selection score.
- Supported formats/document types.
- Failure diagnostics.
- Unit tests.

## Lab 4: Add a new pipeline stage

### Goal

Design a future stage, such as antivirus scanning or OCR, without implementing unsupported behavior.

### Output

- Stage name.
- Inputs and outputs.
- Idempotency rule.
- Retry rule.
- Metrics.
- UI state.

## Lab 5: Production support drill

### Scenario

Pipeline fails at `EMBEDDING` for one document.

### Output

- Logs to query.
- Tables to inspect.
- Dependencies to check.
- Safe retry plan.
- User-facing status message.

## Lab 6: Principal Engineer architecture review

### Prompt

Should MarketMind become microservices now?

### Expected answer

Write a two-page decision memo covering:

- current module boundaries;
- scaling bottlenecks;
- team ownership;
- data consistency;
- deployment complexity;
- extraction seams;
- recommendation.

## Lab 7: Frontend operational UX review

### Goal

Improve one page so users know what happened in the background.

### Checklist

- Is there an empty state?
- Does the page distinguish success from no result?
- Are retry/next actions visible?
- Are statuses color-coded consistently?
- Are notifications persistent?

