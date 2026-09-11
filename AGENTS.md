# AGENTS.md

## Scope

These instructions apply to the entire `certis-api` repository. More specific
`AGENTS.md` files may add or override rules for their own subdirectories.

When existing code conflicts with this file, follow these rules for all new or
modified code. Do not perform an unrelated repository-wide refactor unless the
task explicitly requires it.

Preserve the existing architecture and public contracts unless the task
explicitly asks to change them. Keep changes focused on the requested behavior.

## Project overview

`certis-api` is a Kotlin/Spring Boot API for personal finance management. The
main stack is:

- Kotlin and Java 21;
- Spring Boot, Spring MVC, Spring Security, and Bean Validation;
- jOOQ and PostgreSQL;
- Liquibase migrations;
- MapStruct and Kotlin mapping extensions;
- MinIO for object storage;
- JUnit 5, MockMvc, Mockito, and AssertJ for tests;
- ktlint, detekt, and JaCoCo for quality checks.

Keep implementation choices consistent with the feature-oriented package
structure under `ru.digitalhustle.certis.features` and the shared HTTP boundary
under `ru.digitalhustle.certis.api`.

## Architecture

Organize business code by feature first. Current top-level features include
`account`, `budget`, `category`, `goal`, `profile`, `security`, and
`transaction`. Do not restore global business-layer packages or place new
feature code into the legacy `features.<feature>.service` or
`features.<feature>.repository` packages.

Use this dependency direction for HTTP use cases:

`controller -> application service or command/query service -> repository`

The application layer owns use-case orchestration. Command and query services
own focused write and read operations respectively. A controller may invoke a
command or query service directly when no orchestration is required.

API DTOs and mappers belong at the HTTP boundary. Feature models, jOOQ records,
security principals, and external API models must not leak into controller
contracts.

### Package responsibilities

Use the existing package structure according to these responsibilities:

- `api.controller` and `api.controller.impl`: HTTP endpoint contracts and their
  thin implementations;
- `api.dto.request`: validated transport input models ending in `Rq`;
- `api.dto.response`: endpoint response models ending in `Rs`;
- `api.mapper`: transport-to-feature and feature-to-transport mapping;
- `features.<feature>.application.service`: use-case coordinators that combine
  several operations or resources;
- `features.<feature>.application.validator`: validation that belongs to an
  orchestrated use case rather than one command;
- `features.<feature>.command.model`: input and result models for state-changing
  operations;
- `features.<feature>.command.service`: state-changing business operations;
- `features.<feature>.command.repository`: persistence used to change state;
- `features.<feature>.command.validator`: reusable business preconditions for
  commands;
- `features.<feature>.query.model`: read projections, filters, pages, and
  analytics models;
- `features.<feature>.query.service`: read-only operations;
- `features.<feature>.query.repository`: read-oriented persistence, including
  projections and analytics queries;
- `features.<feature>.api`: the narrow public API exposed to other features;
- `features.<feature>.model`: models owned by the feature and shared between its
  internal command, query, and application layers;
- `features.<feature>.gateway`: adapters for external systems owned by the
  feature;
- `config` and `config.properties`: Spring configuration and typed properties;
- `exception`: shared exception hierarchy and centralized boundary handlers;
- `util`: small truly cross-feature utilities without business knowledge.

Do not move feature-specific models, validators, constants, or helpers into a
shared package merely because another feature needs similar behavior. Keep
ownership explicit and expose the minimum required capability through the
owning feature's `api` package.

### Feature boundaries

- A feature owns its models, persistence access, business rules, and internal
  services.
- Code outside a feature must not depend on its `command`, `query`,
  `application`, `repository`, validator, or internal model packages.
- Cross-feature calls must go through small interfaces and purpose-built data
  projections in the provider feature's `api` package.
- Name cross-feature APIs after the capability they expose, such as
  `AccountCommandAccess`, `UserPreferencesQuery`, or
  `RecurringTransactionUsage`; do not publish a feature's broad internal
  service interface.
- Keep cross-feature return models minimal. Do not expose an entire entity when
  the consumer needs only an ID, status, balance, currency, or another small
  snapshot.
- Implement cross-feature API interfaces inside the provider feature. The
  consumer must depend only on the interface and its public API models.
- Cyclic feature dependencies are forbidden. If two features need each other,
  move orchestration to the feature that owns the use case or introduce a
  deliberately shared abstraction with clear ownership.
- Direct access to another feature's tables is allowed only for read-oriented
  projections and analytics when a database JOIN is the natural implementation.
  Keep such SQL in a purpose-built query repository owned by the feature that
  owns the use case; do not reuse another feature's repository or persistence
  entity.
- State changes to another feature must always go through that feature's public
  API. Do not update another feature's tables directly.

### Controllers

- Keep endpoint declarations in `controller` interfaces and implementations in
  `controller.impl`, following the existing project style.
- Controllers should be thin: validate and accept transport data, obtain the
  authenticated principal, map API models, invoke one application entry point,
  and return the result.
- Do not put repository access, business rules, transaction orchestration, or
  cross-entity workflows in controllers.
- Obtain the current user from `@AuthenticationPrincipal JwtDetails`. Do not
  trust a user or owner ID supplied in a request when it can be derived from the
  authenticated principal.
- Keep HTTP-specific behavior at the controller boundary. Application,
  command, and query services must not depend on servlet response types or HTTP
  headers.

### Command and query services

- Commands create, update, archive, restore, delete, execute, or otherwise
  change state. Put their models, services, repositories, and validators under
  `command`.
- Queries read state without changing it. Put detail lookups, lists, filters,
  pages, projections, and analytics under `query`.
- Query services must not invoke command services or perform hidden writes.
- Command services may read the minimum state needed to enforce a write
  invariant, preferably through a command repository or the owning feature's
  narrow public API. Do not depend on broad query views only to obtain one
  field.
- Do not force command and query persistence into one repository. Use separate
  repositories when their models, joins, or reasons for change differ.
- Name services by responsibility. Split template management, execution,
  analytics, lifecycle, and transfer workflows instead of accumulating them in
  one large feature service.

### Application and orchestration services

- Use an application service when a use case coordinates several command/query
  services, gateways, or cross-feature APIs.
- Split large orchestrators by use case. For example, goal management and goal
  contribution workflows belong to separate application services rather than a
  single `GoalApplicationServiceImpl` with unrelated responsibilities.
- Application services must not access jOOQ or repositories directly. They
  coordinate through command/query services and public feature APIs.
- Place transaction boundaries on the highest application or command workflow
  that must complete atomically.
- Explicitly handle non-transactional side effects such as MinIO writes and
  cleanup on commit or rollback.
- Do not introduce an application service for a simple one-service operation.
  Controllers may call a focused command or query service directly.

### Validators

- Extract reusable business preconditions and state checks into focused
  `...Validator` classes in the owning feature.
- Keep Jakarta transport validation on request DTOs. Validators must not
  duplicate `@Valid` constraints.
- A validator may compare command data with already loaded domain state and may
  use a narrow cross-feature API when the validation itself requires external
  feature state.
- Validators should not persist data, orchestrate workflows, translate database
  write results, or hide not-found lookup behavior that belongs to a service or
  repository.
- Keep validation names explicit, for example `validateActive`,
  `validateCurrencyMatches`, or `validateCanArchive`; avoid generic methods such
  as `validate` when a class checks several independent rules.
- Do not keep duplicated private validation methods in application or command
  services after a focused validator owns the rule.

### Repositories

- Keep repositories focused on persistence queries and mapping database results.
- Put write persistence in `command.repository` and read persistence in
  `query.repository`.
- Use jOOQ and generated table and record classes consistently with existing
  code.
- Do not place HTTP concerns or workflow orchestration in repositories.
- Scope resource queries by owner where access control depends on ownership.
- Avoid N+1 query patterns. Add purpose-built batch or aggregate queries when
  needed.
- Return feature-owned domain models or query projections rather than leaking
  generated jOOQ records to higher layers.
- Prefer declarative jOOQ mapping with `Records.mapping`, `fetchOneInto`, and
  equivalent APIs.
- Give JOIN-heavy read repositories a use-case-oriented name. For example, a
  transaction analytics repository may join transactions and categories, while
  `AccountCommandRepository` must remain focused on account state changes.
- A repository may read tables owned by other features for one cohesive query,
  but it must not become a general back door into those features or mutate their
  state.

### Gateways and external APIs

- Access every external API, remote service, cloud SDK, or infrastructure client
  through a dedicated `...Gateway` abstraction in the `gateway` package.
- Follow the existing `MinioGateway` pattern.
- Controllers, application services, and command/query services must not call an
  external SDK or HTTP client directly.
- Keep provider-specific request, response, and exception types inside the
  gateway implementation.
- Translate external failures into meaningful project exceptions at the gateway
  boundary.
- Keep retry, timeout, authentication, and serialization details inside the
  gateway or its configuration unless the workflow explicitly owns them.
- Mock gateways in unit and integration tests when the external system itself is
  not the subject of the test.

### Method ordering

Keep public operations in controllers, service interfaces, and their
implementations in this order:

1. read, get, find, list, or search;
2. create, save, register, or upload;
3. update, refresh, rotate, or modify;
4. delete, archive, revoke, or remove.

Keep the same relative method order in an interface and its implementation.
Place private helpers after public methods, grouped by the public workflow they
support when practical.

Do not reorder unrelated files solely for style unless the task explicitly asks
for a cleanup.

## API contracts and DTO naming

### `ResponseEntity`

- Avoid `ResponseEntity` for ordinary JSON responses, fixed-status endpoints,
  and endpoints without a response body.
- Return DTOs or primitive bodies directly from controller methods.
- Set fixed HTTP statuses with annotations such as `@ResponseStatus`.
- Return `Unit` for endpoints with no response body, normally together with
  `@ResponseStatus(HttpStatus.NO_CONTENT)` when appropriate.
- Use `ResponseEntity` only in exceptional cases where the endpoint genuinely
  requires dynamic HTTP response control, such as dynamic headers, content type,
  content length, cache directives, redirects, conditional responses, or binary
  and file content.
- The existing profile-photo endpoint is an acceptable example because it sets
  the photo content type, content length, and cache control dynamically.
- Keep any justified `ResponseEntity` usage at the controller boundary. Do not
  pass it into services or use it as an application/domain model.
- Do not migrate an existing justified binary endpoint away from
  `ResponseEntity` merely to satisfy a stylistic preference.

### Requests

- Request DTO names must end in `Rq`, for example `CreateAccountRq`.
- Put request DTOs in `dto.request`.
- Declare transport-level validation on request DTOs with Jakarta Validation
  annotations and use `@Valid` at the controller boundary.
- Do not reuse persistence entities or internal command models as request DTOs.

### Responses and shared DTOs

Choose response names using these rules, in this order:

1. Use an endpoint-specific `...Rs` class by default.
2. Use `...Dto` only when the exact same transport representation is
   intentionally returned by two or more endpoints.
3. Before introducing a shared `...Dto`, consider how the endpoints are likely
   to evolve. If their response fields, permissions, detail level, or versioning
   will probably diverge, create separate `...Rs` classes immediately, even if
   that duplicates several fields today.
4. Do not share a DTO merely to reduce code duplication. Stable API contracts
   are more important than avoiding small transport-model duplication.

Examples:

- `CreateProfileRs` for a response specific to profile creation;
- `ProfileDetailsRs` and `ProfileSummaryRs` when detail levels can evolve
  independently;
- `AccountDto` only when several endpoints deliberately expose the same stable
  account representation.

Response DTOs must contain only API-facing data. Do not return database entities,
jOOQ records, internal command models, external API models, or security
principals from endpoints.

Do not return a bare collection from an endpoint. Wrap it in an endpoint-owned
response object with a meaningful field such as `accounts`, `transfers`,
`categoryOptions`, or `recurringTransactions`. This keeps the JSON contract
extensible without changing its root shape when metadata is added later.

### Uploaded files

- Treat client-provided filenames and `Content-Type` headers as untrusted
  metadata.
- Detect the actual media type from file bytes with the feature's format
  detector before processing or storing the file.
- Validate support using the detected media type. Do not require the filename
  extension to match it and do not duplicate the same media-type check in a
  separate extension validator.
- Derive the stored object extension and persisted media type from the detected
  format, using one canonical extension per supported media type.
- Keep filename, size, and decoded-content constraints in a focused validator;
  keep format detection and canonical format mapping in a focused detector.

### Mapping and errors

- Keep transport-to-domain and domain-to-transport conversion in mappers or
  focused mapping extensions, not in repositories.
- Reuse the project's exception hierarchy and centralized exception handlers.
- Do not return ad hoc error maps from controllers.
- Do not catch an exception only to hide it. Translate it to a meaningful domain
  exception or handle it at the boundary responsible for recovery.
- Do not expose stack traces, credentials, tokens, database details, or external
  provider error payloads in API responses.
- Use MapStruct to map dto to entity and entity to dto. Mapper method should use `convert` name 

### Ownership and access responses

- Apply ownership checks consistently and preserve each endpoint's established
  contract.
- The existing `@OwnProfileOnly` flow returns `403 Forbidden` for an
  authenticated user accessing another user's profile. Preserve that behavior
  unless the task explicitly changes the contract and its tests.
- Prefer owner-scoped lookups such as `findByIdAndUserId` when resource existence
  should remain undisclosed. In that case, return the project's normal `404`
  response.
- Do not change an endpoint from `403` to `404`, or from `404` to `403`, without
  an explicit contract change and corresponding tests.

## Security invariants

Preserve these security behaviors unless the task explicitly changes them:

- access and refresh JWTs are transported through secure `HttpOnly` cookies;
- an access token must not be accepted as a refresh token;
- refresh tokens are single-use and rotation must remain enforced;
- reuse of an already rotated refresh token revokes its token family;
- logout and session-revocation flows invalidate the server-side refresh
  session;
- cookie removal must use attributes compatible with cookie creation, especially
  path and domain;
- login responses must not reveal whether the email or password was incorrect;
- normalize email before lookup and persistence;
- return consistent JSON bodies for `401 Unauthorized` and `403 Forbidden`;
- scope session reads and revocations to the authenticated user;
- never log raw passwords, access tokens, refresh tokens, signing secrets, or
  sensitive cookie values.

## Environment variables and configuration

Whenever a repository change introduces a new environment variable:

1. Add it to `.env.example` when that file exists.
2. In `.env.example`, use only the variable name and an ellipsis placeholder:
   `VARIABLE_NAME=...`.
3. Preserve existing comments and grouping in `.env.example`.
4. Update every relevant configuration binding, Docker or deployment definition,
   CI configuration, and documentation reference in the same change.
5. Never copy real credentials, tokens, secrets, or machine-specific values into
   `.env.example`, source files, tests, logs, or commits.

Do not create, modify, or commit `.env` as part of a repository change unless the
user explicitly asks for local environment setup. Mention any required local
`.env` update in the handoff notes.

Apply these rules even when `application.yml` provides a local default. Do not
create a missing `.env.example` solely for this rule unless the task explicitly
asks for it.

## Database and migrations

- Treat Liquibase changesets as append-only after they have been applied or
  merged. Add a new changeset instead of rewriting database history.
- Keep the master changelog updated when adding a changeset.
- Preserve existing schema, foreign-key, uniqueness, and check-constraint
  conventions unless a task explicitly changes the data model.
- Never edit generated jOOQ sources manually. Regenerate them after schema
  changes.
- Keep direct SQL and jOOQ DSL usage inside repositories or migration tooling.
- Keep monetary values as `BigDecimal`; do not use floating-point types for
  balances or amounts.
- Use the injected `ApplicationClock` for time-dependent application logic so
  tests remain deterministic and the configured application zone is applied
  consistently. Inject the underlying `Clock` only into `ApplicationClock` and
  its Spring configuration.

## Financial domain invariants

- Do not perform arithmetic across different currencies without an explicit
  conversion workflow and exchange-rate source.
- Define rounding and scale explicitly when a monetary calculation requires
  them. Do not rely on implicit floating-point or database rounding.
- Keep balance changes and their corresponding transaction records atomic when
  they represent one business operation.
- Preserve financial history. Do not hard-delete transactions or referenced
  financial records unless the task and data-retention policy explicitly require
  it.
- Respect archive semantics. Archived resources must not silently behave as
  active resources.
- A transfer between accounts must update both sides consistently within one
  transaction and must not leave a partial result.

## Testing requirements

- Add or update tests for every behavior change and bug fix.
- Put focused service tests under `src/test/kotlin/.../units` and endpoint and
  database tests under `src/test/kotlin/.../integrations`, matching the existing
  structure.
- Use the existing `given / when / then` layout and descriptive Kotlin test names.
- Unit-test command and query services in isolation with mocked repositories.
- Unit-test application/orchestration services with mocked command/query
  services, public feature APIs, gateways, processors, and providers, especially
  transaction and external-system failure paths.
- Integration-test endpoint status, JSON contract, validation, authentication,
  ownership isolation, persistence effects, and rollback behavior where
  relevant.
- Every integration test class must inherit from `AbstractIntegrationTest`.
- `AbstractIntegrationTest` must contain the dependencies required by the
  integration-test suite, declared with `protected` visibility.
- Reuse dependencies, fixtures, helpers, mocked gateways, and infrastructure from
  `AbstractIntegrationTest`; do not duplicate common Spring injection in
  individual integration test classes.
- Add a dependency directly to one integration test only when it is genuinely
  specific to that test and does not belong to the shared integration setup.
- Prefer the existing fixtures and helpers over creating parallel test
  infrastructure.
- A resource owned by another user must have an explicit negative integration
  test for access isolation.
- Do not call real external APIs from the regular integration-test suite. Mock
  the corresponding gateway unless the test is explicitly an external-system or
  end-to-end test.

Before handing off a change, run the narrowest relevant tests first, then the
full applicable checks. The normal verification target is:

```bash
./gradlew test ktlintCheck detekt -Dspring.profiles.active=test
./gradlew check -Dspring.profiles.active=test
```

Gradle configuration requires local database properties. If
`gradle.properties` is missing, create a local ignored copy from
`gradle.properties.example` and do not commit secrets. Start required local
services with Docker Compose and apply migrations when the tested change needs
them.

## Code quality and change discipline

- Follow the repository's ktlint and detekt configuration.
- Prefer constructor injection and immutable `val` properties.
- Keep classes and functions focused. Split application services by use case
  instead of growing one feature-wide orchestrator.
- Avoid wildcard imports, unchecked casts, `!!`, magic strings, and duplicated
  path or error constants when an existing project constant applies.
- Do not add `TODO` or `FIXME` comments; detekt forbids them. Use the issue tracker
  for deferred work.
- Preserve public API compatibility unless the requested change intentionally
  modifies the contract.
- Keep changes scoped. Do not reformat or rename unrelated code.
- Do not modify generated files or commit local configuration, build output,
  secrets, IDE metadata, or `.env`.

## Completion checklist

Before considering a task complete, confirm that:

- controllers remain thin and do not access repositories directly;
- `ResponseEntity` is used only when dynamic HTTP metadata or response control
  genuinely requires it;
- every request DTO ends in `Rq`;
- every response model follows the `Rs` versus shared `Dto` decision rules;
- feature internals are not imported directly by other features;
- cross-feature calls use narrow interfaces and minimal projections from the
  provider feature's `api` package;
- commands and queries remain separated and query paths do not perform hidden
  writes;
- multi-service workflows are placed in focused application services and do not
  access repositories directly;
- reusable business checks live in focused validators without persistence or
  orchestration side effects;
- JOIN-heavy cross-feature reads live in purpose-built query repositories and do
  not mutate another feature's tables;
- collection endpoints return named response wrappers rather than bare lists;
- uploaded-file type and canonical extension are derived from file content;
- every external API or infrastructure integration is accessed through a
  `...Gateway` abstraction;
- interface and implementation method ordering remains consistent;
- every integration test inherits from `AbstractIntegrationTest` and shared
  dependencies are declared there as `protected`;
- every new environment variable is reflected in `.env.example` with
  `VARIABLE_NAME=...`, while `.env` remains untouched unless explicitly
  requested;
- ownership and authenticated-user boundaries are preserved;
- security invariants remain intact;
- migrations and generated jOOQ usage are correct;
- monetary operations preserve currency, rounding, atomicity, and history
  rules;
- relevant tests and quality checks pass.
