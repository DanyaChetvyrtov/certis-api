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

Keep implementation choices consistent with the existing package structure
under `ru.digitalhustle.certis`.

## Architecture

Use this dependency direction:

`controller -> application entry point -> domain service -> repository`

An application entry point may be either a single domain service or a
facade/orchestration service. Facades may also coordinate gateways, processors,
providers, and other lower-level services.

API DTOs and mappers belong at the application boundary. Persistence entities,
jOOQ records, security principals, and external API models must not leak into
controller contracts.

### Package responsibilities

Use the existing package structure according to these responsibilities:

- `controller` and `controller.impl`: HTTP endpoint contracts and their thin
  implementations;
- `dto.request`: validated transport input models ending in `Rq`;
- `dto.response`: endpoint response models ending in `Rs`;
- `mapper`: transport/domain/entity mapping;
- `model`: internal application and workflow models;
- `model.entity`: persistence-facing domain entities;
- `model.security`: authentication and authorization models;
- `service.domain`: entity-focused services;
- `service.aggregation`: facades coordinating multiple lower-level services;
- `service.security`: authentication, token, cookie, and session workflows;
- `service.photo`: photo processing, photo-related helpers, and photo workflows;
- `repository`: jOOQ persistence access;
- `gateway`: adapters for external systems and APIs;
- `provider`: focused data or object providers that do not own a complete
  business workflow;
- `filter`: servlet and security filters;
- `config` and `config.properties`: Spring configuration and typed properties;
- `exception`: exception hierarchy and centralized boundary handlers;
- `util`: small reusable utilities without business orchestration.

The package name alone does not define a component's architectural level. Judge
it by responsibility and dependencies.

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
- Keep HTTP-specific behavior at the controller boundary. Domain and facade
  services must not depend on servlet response types or HTTP headers.

### Domain/entity services

- Put entity-focused services in `service.domain` and their implementations in
  `service.domain.impl`.
- A domain service owns the business operations for one entity or one tightly
  bound persistence aggregate.
- A domain service normally depends on exactly one repository belonging to the
  entity or aggregate it owns.
- Do not inject repositories belonging to unrelated entities into one domain
  service.
- If a genuine persistence aggregate requires multiple repositories, do not
  introduce that exception automatically. The task must explicitly require it,
  and the reason must be documented in the implementation or handoff.
- Keep direct jOOQ access inside repositories.
- A domain service may use focused helpers such as a mapper, validator, or
  injected `Clock`, but it must not become an orchestrator for unrelated
  entities.
- If a use case needs data or mutations from multiple entities or lower-level
  services, coordinate them in a facade/orchestration service.

### Facade and orchestration services

A facade service is defined by its responsibility, not by an `Aggregator`
suffix or by living only in `service.aggregation`.

- Use a facade when a workflow coordinates multiple lower-level services,
  gateways, processors, providers, or persistence aggregates.
- A facade may be named `...Aggregator`, `...Service`, `...Manager`, or another
  domain-appropriate name.
- Place a general cross-entity facade in `service.aggregation` when that package
  best describes it.
- Cohesive workflows may instead live in logical subpackages such as
  `service.security`, `service.photo`, or another domain-specific service
  package. Do not move them into `service.aggregation` merely to satisfy a
  naming convention.
- Facades must not inject repositories directly. They work through domain
  services so persistence rules and entity-level behavior have one owner.
- Place multi-resource transaction boundaries on the facade workflow when
  atomicity is required.
- Explicitly handle non-transactional side effects such as MinIO writes and
  cleanup on commit or rollback.
- Do not introduce a facade when a use case concerns only one entity and can be
  handled cleanly by its domain service.

Examples:

- Allowed: `AccountService -> AccountRepository`.
- Allowed: `TransferService -> AccountService + TransactionService`.
- Allowed: `AuthService -> UserService + RefreshSessionService + JwtTokenProvider`.
- Allowed: `ProfileAggregator -> ProfileService + ProfilePhotoMetaService + MinioGateway`.
- Not allowed: `AccountService -> AccountRepository + TransactionRepository`.
- Not allowed: `TransferService -> AccountRepository + TransactionRepository`.

### Repositories

- Keep repositories focused on persistence queries and mapping database results.
- Use jOOQ and generated table and record classes consistently with existing
  code.
- Do not place HTTP concerns or workflow orchestration in repositories.
- Scope resource queries by owner where access control depends on ownership.
- Avoid N+1 query patterns. Add purpose-built batch or aggregate queries when
  needed.
- Return project domain/entity models rather than leaking generated jOOQ records
  to higher layers.

### Gateways and external APIs

- Access every external API, remote service, cloud SDK, or infrastructure client
  through a dedicated `...Gateway` abstraction in the `gateway` package.
- Follow the existing `MinioGateway` pattern.
- Controllers and domain services must not call an external SDK or HTTP client
  directly.
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

### Mapping and errors

- Keep transport-to-domain and domain-to-transport conversion in mappers or
  focused mapping extensions, not in repositories.
- Reuse the project's exception hierarchy and centralized exception handlers.
- Do not return ad hoc error maps from controllers.
- Do not catch an exception only to hide it. Translate it to a meaningful domain
  exception or handle it at the boundary responsible for recovery.
- Do not expose stack traces, credentials, tokens, database details, or external
  provider error payloads in API responses.

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
- Use an injected `Clock` for new time-dependent business logic so tests remain
  deterministic.

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
- Unit-test domain services in isolation with mocked repositories.
- Unit-test facade/orchestration services with mocked domain services, gateways,
  processors, and providers, especially transaction and external-system failure
  paths.
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
- Keep classes and functions focused. Extract orchestration instead of growing
  entity services across boundaries.
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
- each domain service owns only its entity or aggregate persistence concerns;
- multi-service workflows are placed in an appropriate facade/orchestration
  service and use domain services rather than repositories;
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
