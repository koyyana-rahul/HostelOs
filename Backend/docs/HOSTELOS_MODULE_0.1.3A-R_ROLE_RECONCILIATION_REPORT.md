# HostelOS Module 0.1.3A-R Report - Platform Administration Role Reconciliation

- Date: 2026-10-28
- Status: Complete
- Module: 0.1.3A-R - platform administration role reconciliation
- Runtime verified with: Java 17.0.16, Maven 3.9.9, Spring Boot 4.1.2,
  Spring Security 7.1.2, MySQL 8.0.40, H2 2.4.240
- Scope: backend identity role model only

## Outcome

The identity foundation now distinguishes three server-controlled roles:

| Role | Spring authority | Scope | Permission boundary |
|---|---|---|---|
| `PLATFORM_ADMIN` | `ROLE_PLATFORM_ADMIN` | Platform-wide | Platform identity and privileged configuration. Not an organization role. |
| `ORGANIZATION_ADMIN` | `ROLE_ORGANIZATION_ADMIN` | Organization, deferred | Reserved for future organization administration. No membership or tenant model exists, so it grants no organization API access and must never authorize platform endpoints. |
| `STAFF` | `ROLE_STAFF` | Operational | Operational staff workflows. Not platform or organization administration. |

There is no implicit role hierarchy. A platform administrator is not
automatically an organization administrator or staff member, and an
organization administrator is never treated as a platform administrator.

The login request still accepts only `loginIdentifier` and `password`. Role
names are never accepted from login or other frontend input. An additional
`roles` field in a login payload is ignored, and the integration suite verifies
that it does not change the authenticated authorities.

## Meaning of the original ADMIN role

Before this module, `V2__identity_foundation.sql` seeded `ADMIN` with the
description "Administrative identity and privileged configuration". The only
bootstrap path, `UserAccountService.createAdminIfAbsent`, assigned that role to
the environment-driven bootstrap account. No organization, membership, hostel,
or tenant table existed, and no organization-scoped authorization was
implemented.

Therefore the existing `ADMIN` role represented platform-wide administrative
authority. It did not represent an ordinary organization administrator. Mapping
it to `PLATFORM_ADMIN` preserves that original meaning rather than expanding it.

## Exact role mapping

| Before | After | Data handling |
|---|---|---|
| `ADMIN` | `PLATFORM_ADMIN` | The existing role row is renamed in place. Its `roles.id` is unchanged, so every `user_roles` row continues to reference the same role and remains effective. |
| none | `ORGANIZATION_ADMIN` | New seeded role row. No user is assigned to it. |
| `STAFF` | `STAFF` | Role row, ID, assignments, and authority remain unchanged. |

The old `ADMIN` authority and role name no longer exist after `V3`. External
consumers that compare role strings must use `PLATFORM_ADMIN`; the API returns
the bare role name and Spring Security uses `ROLE_PLATFORM_ADMIN`.

## Migration plan

`V3__reconcile_platform_admin_role.sql` is a new forward-only migration:

1. Drop the old `chk_roles_name` constraint that only allowed `ADMIN` and
   `STAFF`.
2. Rename `ADMIN` to `PLATFORM_ADMIN` and update its description.
3. Insert `ORGANIZATION_ADMIN` with a description stating that tenant-scoped
   enforcement is deferred.
4. Update the `STAFF` description.
5. Add a replacement check constraint allowing exactly `PLATFORM_ADMIN`,
   `ORGANIZATION_ADMIN`, and `STAFF`.

The migration deletes no identity data and removes no user-role assignments.
`V2` was not edited.

Deployment consequences:

- Every existing `ADMIN` account becomes a platform-wide administrator on
  upgrade. Operators must review privileged accounts before applying `V3`.
- No account automatically gains `ORGANIZATION_ADMIN` or `STAFF`.
- Existing sessions keep their old cached authorities until the next login.
  Account status is still rechecked on every authenticated request.
- The new application must run against `V3`. Rolling the application back to a
  build that expects `ADMIN` while `V3` remains applied is not supported without
  restoring the pre-migration database backup.
- Take a database backup before applying the migration in production.

## Bootstrap behavior

The environment variable names remain `HOSTELOS_BOOTSTRAP_ADMIN_*` for deployment
compatibility. The runner behavior is otherwise unchanged except for the role:

- Disabled by default.
- Runs only when `HOSTELOS_BOOTSTRAP_ADMIN_ENABLED=true` and all required values
  are present.
- Requires a password of at least 16 characters.
- Creates one `PLATFORM_ADMIN` account if the normalized login does not exist.
- Does not edit an existing account and assigns no other role.
- Never logs the password or hash.

## Preserved security behavior

- Server-side Spring Security sessions and the HttpOnly `HOSTELOS_SESSION`
  cookie are unchanged.
- CSRF bootstrap and enforcement are unchanged.
- Login throttling is unchanged.
- Account status enforcement and session invalidation for disabled accounts are
  unchanged.
- Deny-by-default authorization and the explicit route allowlist are unchanged.
- No organization-management API or tenant-isolation claim was added.
- No frontend file was modified.

## Files changed

| File | Change |
|---|---|
| `src/main/java/com/rahul/HostelOs/identity/RoleName.java` | Replaced `ADMIN` with `PLATFORM_ADMIN`, added `ORGANIZATION_ADMIN`, kept `STAFF`. |
| `src/main/java/com/rahul/HostelOs/identity/UserAccountService.java` | Bootstrap creation renamed to `createPlatformAdminIfAbsent` and assigns only `PLATFORM_ADMIN`. |
| `src/main/java/com/rahul/HostelOs/identity/AdminBootstrapRunner.java` | Calls the platform-administrator bootstrap operation. |
| `src/main/java/com/rahul/HostelOs/auth/AuthUserResponse.java` | OpenAPI example updated to `PLATFORM_ADMIN`. |
| `src/main/java/com/rahul/HostelOs/auth/LoginRequest.java` | OpenAPI login example updated; request shape unchanged. |
| `src/main/resources/db/migration/V3__reconcile_platform_admin_role.sql` | Forward-only role reconciliation migration. |
| `src/test/java/com/rahul/HostelOs/AuthIntegrationTests.java` | Three-role fixtures, seeding test, role-injection test, and platform/organization/staff boundary probes. |
| `src/test/java/com/rahul/HostelOs/identity/AdminBootstrapRunnerTests.java` | Bootstrap mock updated to the platform-administrator operation. |
| `src/test/java/com/rahul/HostelOs/identity/RoleMigrationTests.java` | New fresh-schema and V2-to-V3 upgrade tests. |
| `docs/API.md` | Role model, bootstrap role, examples, and V3 migration documentation. |
| `docs/IMPLEMENTATION_LOG.md` | Module implementation entry. |
| `docs/HOSTELOS_MODULE_0.1.3A_REPORT.md` | Pointer to this role reconciliation. |
| `docs/HOSTELOS_MODULE_0.1.3A-R_ROLE_RECONCILIATION_REPORT.md` | This report. |

Shared documentation updated at the repository root:

- `docs/ADR-0001-IDENTITY-AND-AUTHENTICATION.md`
- `docs/CURRENT_STATE.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/IMPLEMENTATION_LOG.md`

`Backend/.env.example` was not changed. No frontend files were changed.

## Automated verification

Command:

```powershell
$env:JAVA_HOME = 'C:\Users\RAHUL\.jdks\ms-17.0.16'
$env:Path = "C:\Users\RAHUL\.jdks\ms-17.0.16\bin;$env:Path"
.\mvnw.cmd -B clean verify
```

Actual result:

```text
BUILD SUCCESS
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,752,614 bytes
```

Test breakdown:

| Test class | Tests | Result |
|---|---:|---|
| `AuthIntegrationTests` | 15 | PASS |
| `ErrorHandlingIntegrationTests` | 2 | PASS |
| `HostelOsApplicationTests` | 12 | PASS |
| `AdminBootstrapRunnerTests` | 4 | PASS |
| `LoginIdentifierTests` | 2 | PASS |
| `RoleMigrationTests` | 2 | PASS |
| `ProductionDocumentationTests` | 7 | PASS |
| **Total** | **44** | **PASS** |

New coverage includes:

- Exactly three seeded roles with no legacy `ADMIN`.
- Bootstrap creation assigns exactly `PLATFORM_ADMIN` and remains idempotent.
- A login payload containing `roles` cannot elevate a `STAFF` account.
- `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, and `STAFF` probes are mutually
  distinct; in particular, `ORGANIZATION_ADMIN` is denied platform access.
- V3 upgrades a V2 database with existing users and role assignments without
  losing or changing them.
- The post-V3 check constraint rejects the legacy `ADMIN` name.

## Real MySQL verification

A packaged jar was run against disposable MySQL 8.0.40 schemas. The normal
`hostelos` database was not modified or dropped.

Observed upgrade sequence:

```text
1. Created disposable schema.
2. Ran Flyway with spring.flyway.target=2: V1 and V2 applied.
3. Inserted a user with the legacy ADMIN assignment.
4. Ran the current jar: V3 applied.
5. Roles: ORGANIZATION_ADMIN, PLATFORM_ADMIN, STAFF.
6. Legacy user assignment: PLATFORM_ADMIN.
7. Enabled the bootstrap variables: created bootstrap-verify@example.com
   with PLATFORM_ADMIN only.
8. Flyway history: 1 baseline, 2 identity foundation,
   3 reconcile platform admin role; all successful.
9. Dropped the disposable schema.
```

Hibernate `ddl-auto=validate` passed on the migrated MySQL schema. The temporary
schema name, temporary bootstrap password, and database credentials were not
recorded in this report.

## Unresolved risks

1. `ORGANIZATION_ADMIN` is a role definition only. Organization membership,
   tenant boundaries, and organization-scoped APIs do not exist, so the role
   grants no organization access today.
2. A deployed frontend build that still expects legacy `ADMIN`/`STAFF` must be
   updated alongside `V3`. The in-repo frontend was reconciled separately by
   Module 0.1.3B-R; frontend files were outside this module and were not changed.
3. Existing `ADMIN` assignments become platform-wide administrator authority.
   Privileged accounts must be reviewed before applying `V3`.
4. Authenticated sessions keep old role names until re-login after the upgrade.
5. Any integration that parsed the literal `ADMIN` role string must migrate to
   `PLATFORM_ADMIN`.
6. MySQL verification is a repeatable manual procedure, not an automated
   Testcontainers/MySQL suite.
7. There is no role-management API or audit trail; role assignment is still a
   server/operator concern.
8. No application endpoint currently uses these roles beyond authentication and
   test probes. Future endpoints must explicitly choose the correct authority
   and must add tenant checks separately from global roles.

## Repository state

- No commit was created.
- No frontend source file was modified.
- The role reconciliation remains uncommitted with the existing Module 0.1.3A/B
  working-tree changes.
