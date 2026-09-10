# Feature command/query architecture

All features separate reads from commands within the same application and
PostgreSQL database. HTTP routes, DTOs, validation and status codes remain stable.

| Package | Responsibility |
| --- | --- |
| `command.service` | Owner services for mutations and state reads needed by mutations |
| `command.repository` | One owned table per repository, including locks and idempotency lookups |
| `query.service` | Read-only entry points for lists, details, analytics and previews |
| `query.repository` | Read queries and projections; cross-feature joins are allowed |
| `application.service` | Write workflows coordinating commands, public APIs and response projections |
| `api` | Deliberate cross-feature interfaces and immutable snapshots |
| `api.impl` | Internal adapters implementing those interfaces |
| `model`, `enums` | Immutable local domain data and calculation inputs/results reused by workflows |
| `command.model`, `query.model` | Models specific to one side where domain types need not be duplicated |

Application orchestration preserves mutation responses containing calculated data,
such as an account balance or goal progress. It performs the mutation and obtains
the response projection within the existing transaction. Query services use
REQUIRED propagation and participate in that transaction. Commands do not depend
on query services; controllers still invoke one entry point per request.

This does not introduce event sourcing, asynchronous projections or another DB.

## Feature boundaries

- Accounts expose `AccountCommandAccess` and a minimal immutable `AccountSnapshot`
  containing only the ID, currency and closed state needed by foreign commands.
  Its shared-lock operations require an existing transaction. Balance queries
  aggregate transactions and goal postings.
- Categories expose separate capabilities instead of one broad API:
  `CategoryCommandAccess` for transactional shared locks,
  `DefaultCategoryProvisioner` for registration and
  `ExpenseCategoryEligibility` for budget validation. `CategorySnapshot` contains
  only the ID, type and archive state needed by transaction commands. Cards,
  analytics, options and archive usage are queries.
- Budgets have separate budget, allocation and optimization repositories. They
  use `UserPreferencesCommand` and `ExpenseCategoryEligibility` instead of
  accessing foreign tables. Optimization commands receive the ID of the already
  locked budget.
- Goals keep lifecycle changes and contributions/refunds atomic. Goal management
  and contribution workflows use separate application services; plan construction
  belongs to \`GoalPlanner\`. Decisions needing only the saved amount use a scalar
  sum of owned postings under the goal lock, preserving the previous money rounding.
  Pages, overview and previews are queries.
- Profiles retain photo processing and storage cleanup after commit/rollback in
  write orchestration. Profile and photo GETs use query services.
- Security treats registration, login, rotation, logout and revocation as commands.
  Session lists and authentication lookups are queries. Token/cookie helpers are
  infrastructure. Currency APIs expose no user entity or password hash.
- Transactions retain the pilot and now use account/category public APIs.
  Transaction-specific validation belongs to the transaction command side.

Public enums and domain exceptions are also cross-feature contracts. Another
feature must not import internal models, repositories, services or `api.impl`.
Query projections describe their own data; a join grants no access to foreign code.

## Consistency and verification

State reads for locks, duplicate detection and command validation remain commands.
A SELECT does not automatically belong to a query workflow. Preserve lock order,
transaction propagation and `noRollbackFor`, especially for token reuse and
financial operations. Existing page/count repeatable-read boundaries, owner
filters, currency rules, soft deletion and date boundaries remain intact.

`FeatureArchitectureTest` verifies dependency direction, public boundaries,
read-only services, query writes/locks, repository access and table ownership.
It checks typed jOOQ calls; it does not parse arbitrary SQL or replace DB permissions.
Existing integration tests cover API contracts and transaction failure paths;
the added goal balance check verifies owner scoping.

```bash
./gradlew check --continue -Dspring.profiles.active=test
```

## Existing follow-up notes

These previous inline notes remain outside this change:

- Reconsider verb-based paths in a separate API-contract change.
- Decide whether the goal-page mapper should receive the authenticated user ID.
- Extract shared pagination constants when pagination conventions are reviewed.
