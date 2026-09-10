# Transaction command/query pilot

This is an in-process command/query separation with one PostgreSQL database.
It does not introduce event sourcing, asynchronous projections, another database,
or changes to the HTTP API. The package remains `features.transaction`.

| Area | Responsibilities |
| --- | --- |
| `command.service` | Write workflows, validation, atomic transfers, category assignment, recurring execution and retries |
| `command.repository` | Writes and state reads needed by commands, against the repository's owned table only |
| `command.model` | Command input and execution-result models |
| `query.service` | Read-only application entry points for details, lists and analytics |
| `query.repository` | Read queries, including cross-feature joins and aggregation |
| `query.model` | Filters, pages and query-owned projections |
| `model`, `enums` | Local immutable transaction, transfer and recurring-template models and value types |
| `api.RecurringTransactionUsage` | Narrow cross-feature contract used when closing an account |

## Ownership and consistency

- `TransactionRepository` owns `transactions`.
- `TransferRepository` owns `transfers`.
- `RecurringTransactionTemplateRepository` owns `recurring_transaction_templates`.
- Queries may read other features' tables, but never change or lock rows.
  Their projections must not contain another feature's internal domain model.
  `TransactionAccountView` is the transaction query's own representation of an
  account's ID, name and type; the transport mapper retains the existing JSON.
- Commands may read their own state, including `FOR UPDATE`, duplicate-occurrence
  lookup and reversal lookup. These are not display queries and remain in the
  command transaction. Moving them to a read-only service would be incorrect.
- Command workflows call account/category public APIs for validation and shared
  locks. The extension to all features is described in `feature-command-query.md`.
- Transaction list and uncategorized list reads use read-only `REPEATABLE_READ`
  transactions so page items and total counts share one snapshot. Existing date
  boundaries, sorting, ownership filters, transfer exclusions and soft-deletion
  behavior are preserved.
- Command responses still return the written local model directly. They do not
  depend on query services just to construct the existing POST/PUT response.

The HTTP controllers remain at their existing location. GET methods call query
services; mutations call command services. Request/response DTOs, paths, status
codes and validation remain unchanged. `CategoryAggregator` belongs to category,
so it is no longer placed inside transaction.

## Verification

`FeatureArchitectureTest` checks the command/query dependency direction,
absence of query writes/locks and foreign feature models, repository access and
command table ownership. These checks cover the typed jOOQ calls used here; they
are not a SQL parser or a database permission boundary. Keep raw SQL and mutable
record APIs out of query code.

Existing endpoint tests cover filtering, pagination, ownership, soft deletion,
transfers, category assignment, analytics and recurring idempotency. Read service
unit tests are moved to the query services; write tests retain locking, rollback
and idempotency coverage. Run the focused tests first, then:

```bash
./gradlew check --continue -Dspring.profiles.active=test
```

The pre-existing PR contained stale references to the former `dto`/`controller`
packages and moved transaction error constants. Their references are repaired
mechanically as part of wiring the pilot; the API declarations are unchanged.
