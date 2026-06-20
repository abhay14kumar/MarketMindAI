# PostgreSQL Initialization

Place idempotent local bootstrap scripts in this directory only when they are required before Flyway can run.

The official PostgreSQL image executes `*.sql`, `*.sql.gz`, and executable `*.sh` files here in lexical order **only when the PostgreSQL data volume is first initialized**.

MarketMind application tables are owned by Flyway in the backend. Do not duplicate `backend/src/main/resources/db/migration` here.

Suitable future uses include:

- creating PostgreSQL extensions required before application migrations;
- creating separate local-only databases;
- assigning narrowly scoped development roles.

Rules:

- Never place passwords, tokens, API keys, or production data here.
- Make scripts repeatable where practical.
- Prefix scripts numerically, for example `10_extensions.sql`.
- Test initialization against an empty volume.
