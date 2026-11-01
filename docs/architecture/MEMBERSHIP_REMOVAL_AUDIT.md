# MEMBERSHIP REMOVAL AUDIT — HostelOS

## 1. Executive summary
Audit-only phase. No application source files modified. No migrations executed. No database accessed.

Membership infrastructure is deeply embedded across backend domain, repositories, services, controllers, security, frontend role resolution, route guards, RTK Query, and navigation. Removing it requires a coordinated multi-unit refactor. Direct organization ownership via `Organization`/`ownerUserId` must be confirmed and used as replacement.

## 2. Repository and branch baseline
- Branch: `feature/0.1.4-phase-b-organization-foundation`
- Modified tracked files: 31 (backend + frontend). Untracked verification logs and new source files present.
- No commit/push performed.

## 3. Current membership architecture
- Entity: `Backend/.../organization/OrganizationMembership.java`
- Repository: `OrganizationMembershipRepository.java`
- Service: `MembershipManagementService.java`
- Status: `MembershipStatus.java`
- DTOs: `MembershipResponse.java`, `ReactivateMemberRequest.java`, `ChangeMemberRoleRequest.java`
- Exceptions: `MembershipNotFoundException`, `MembershipAlreadyExistsException`, `InvalidMembershipTransitionException`

## 4. Backend dependency inventory
Files referencing membership (17 main source):
- `auth/AuthController.java`
- `common/exception/GlobalExceptionHandler.java`
- `inventory/PropertyManagementService.java`
- `organization/dto/MembershipResponse.java`
- `organization/dto/ReactivateMemberRequest.java`
- `organization/exception/*` (3)
- `organization/MembershipManagementService.java`
- `organization/MembershipStatus.java`
- `organization/OrganizationAuthorizationService.java`
- `organization/OrganizationController.java`
- `organization/OrganizationMembership.java`
- `organization/OrganizationMembershipRepository.java`
- `organization/OrganizationProvisioningService.java`
- `organization/OrgRoleMigrationRunner.java`
- `security/SecurityConfig.java`

Authorization flows through `OrganizationAuthorizationService` and `MembershipManagementService`. `OrganizationProvisioningService` creates an admin membership on org creation.

## 5. Frontend dependency inventory
Files (16):
- `app/routes.js`
- `components/layout/public/PublicHeader.jsx`
- `components/layout/OrgSwitcher.jsx`
- `components/layout/SidebarContent.jsx`
- `features/auth/api/authApi.js` (`getMemberships` -> `/v1/auth/me/memberships`)
- `features/auth/lib/roleResolver.js`
- `features/auth/pages/LoginPage.jsx`
- `features/home/pages/HomeRedirectPage.jsx`
- `features/org/pages/OrgOverviewPage.jsx`
- `features/organizations/api/organizationApi.js`
- `features/organizations/components/RequirePlatformMembership.jsx`
- `features/organizations/hooks/useMemberships.js`
- `features/organizations/pages/CreateOrganizationPage.jsx`
- `features/organizations/pages/OnboardingPage.jsx`
- `features/platform/pages/PlatformPlaceholderPage.jsx`
- `services/api/baseApi.js`

`authApi.js` calls `/v1/auth/me/memberships` which is prohibited in final architecture.

## 6. Database and Flyway impact
- Membership table(s) exist (from `V4__organizations_and_memberships.sql`). Exact FK/index details require investigation of that migration.
- Organization ownership column likely `owner_user_id` — verify in `Organization` entity and V4 migration.
- Removal requires a new Flyway migration; not executed in this phase.

## 7. Authentication and authorization findings
- `/api/v1/auth/me` is the auth source; membership dependency in role resolution must be removed.
- Spring Security authorities derived partly from memberships — verify in `SecurityConfig.java`.
- Cross-org and customer isolation tests exist but rely on membership records.

## 8. Organization ownership assessment
- Need to confirm `Organization.ownerUserId` (or equivalent) exists and is populated by `OrganizationProvisioningService`.
- Provisioning currently creates membership record alongside org; ownership field must become authoritative.

## 9. ORG_STAFF association assessment
- No safe non-membership staff-to-org association identified yet. `ORG_STAFF` currently authorized via membership records. This is a genuine architectural blocker requiring a separate decision (e.g., staff assignment table or org-scoped staff list) without recreating membership.

## 10. Target architecture
User (id, email, globalRole) + Organization (id, name, ownerUserId). Authorization checks against ownerUserId. No membership tables.

## 11. Dependency removal matrix
| Component | Dependency | Removal | Replacement | Risk |
|---|---|---|---|---|
| OrganizationMembership entity | DB table | Yes | ownerUserId | HIGH |
| OrganizationMembershipRepository | JPA | Yes | direct ownership queries | HIGH |
| MembershipManagementService | business logic | Yes | ownership checks | HIGH |
| /auth/me/memberships | endpoint | Yes | /auth/me role | MEDIUM |
| frontend useMemberships | RTK Query | Yes | global role | MEDIUM |
| RequirePlatformMembership | route guard | Yes | role-based guard | MEDIUM |
| OrgSwitcher | UI | Yes | remove | LOW |
| SecurityConfig membership authorities | security | Yes | global role authorities | CRITICAL |
| PropertyManagementService | membership role check | Yes | ownership check | HIGH |

## 12. Removal implementation phases
Unit A: ownership/auth foundation
Unit B: backend membership removal
Unit C: DB compatibility migration (plan only)
Unit D: frontend membership removal
Unit E: regression testing

## 13. Test strategy
Cover all scenarios in task spec; layers: unit, security, integration, frontend route guards, e2e.

## 14. Risks and blockers
- Staff association undefined (blocker).
- SecurityConfig authority derivation.
- Flyway migration data preservation.
- Frontend role resolution heavily membership-based.

## 15. Recommended next implementation unit
Unit A — verify direct ownership field and ownership checks; define staff association strategy.

## 16. Final repository safety verification
No source files changed by this audit. Working tree preserved. No migrations executed.
