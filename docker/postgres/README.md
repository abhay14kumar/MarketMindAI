# PostgreSQL Local Configuration

`postgresql.conf` is mounted read-only into the PostgreSQL 16 container.

The configuration:

- listens inside the isolated Compose network;
- emits logs to container standard error for Docker log rotation;
- logs slow statements, connections, disconnections, lock waits, and deadlocks;
- uses UTC;
- selects SCRAM-SHA-256 for newly stored passwords;
- applies conservative memory settings suitable for a developer workstation.

It is not a production database configuration. Production sizing, TLS, replication, backups, auditing, connection pooling, and managed-service settings require environment-specific design and load testing.
