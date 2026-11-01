# Organization Admin Module Implementation

## Status

Implemented:

- Owner self-registration with public organization onboarding.
- Transitional direct organization ownership via `organizations.owner_user_id`.
- Separate HostelOS Manage workspace for organization owners.
- Organization dashboard, properties, listings, enquiries, organization settings, and personal account pages.
- Public listing discovery and listing detail pages backed by real public listing APIs.
- Customer enquiry submission from public listing details.
- Owner enquiry inbox with status transitions.
- Owner-aware login landing and route guards.

Verification:

- Backend: 256 tests pass (H2 with MySQL compatibility mode).
- Frontend: 152 tests pass across 24 files.
- Frontend lint: clean.
- Frontend production build: passes.
- MySQL/Flyway integration verification was not run in this environment.

## Ownership Model

The implemented model uses direct ownership:

- `organizations.owner_user_id` identifies the owning account.
- New owner accounts register with the global `ORGANIZATION_ADMIN` role.
- `GET /api/v1/organizations/mine` resolves the current owner's organizations.
- Owner APIs authorize through `OrganizationAccessPolicy` and related ownership services.

Legacy membership tables, services, and some fallback behavior remain in place for compatibility with earlier modules. Full membership removal is still a separate refactor and should not be assumed complete by this module.

## Backend

### Registration and Organization Access

- `POST /api/v1/auth/register/owner`
  - Creates an owner account without a browser-selected role.
  - Assigns the global `ORGANIZATION_ADMIN` role.
- `POST /api/v1/organizations`
  - Creates an organization owned by the current owner account.
- `GET /api/v1/organizations/mine`
  - Returns organizations owned by the current account.
- `PATCH /api/v1/organizations/{organizationId}`
  - Updates owner-managed organization details.
- `GET /api/v1/organizations/{organizationId}/dashboard`
  - Returns property, listing, and enquiry summary counters.

### Properties

Owner-aware property management is implemented through the existing inventory module:

- List, create, update, activate, and deactivate properties for an owned organization.
- Property access is constrained by organization ownership.

### Listings

Organization-scoped APIs:

- `GET /api/v1/organizations/{organizationId}/listings`
- `POST /api/v1/organizations/{organizationId}/listings`
- `PATCH /api/v1/organizations/{organizationId}/listings/{listingId}`
- `PATCH /api/v1/organizations/{organizationId}/listings/{listingId}/status`
- `DELETE /api/v1/organizations/{organizationId}/listings/{listingId}?version={version}`

Public APIs:

- `GET /api/v1/public/listings`
  - Filters: `q`, `city`, `type`, `minPrice`, `maxPrice`.
  - Pagination: `page`, `size`.
- `GET /api/v1/public/listings/{listingId}`

Rules:

- Listing titles are unique per organization, case-insensitive, and permanently reserved after archive.
- A listing may optionally belong to a property.
- Publishing requires description, city, latitude, longitude, and base price.
- Mutations require an optimistic-lock `version`; mismatches return `409`.
- `ARCHIVED` is terminal.

### Enquiries

- `POST /api/v1/listings/{listingId}/enquiries`
  - Customer-only submission.
  - Requires customer name, email, phone, and message.
  - Message is required and limited to 2000 characters.
- Owner APIs:
  - `GET /api/v1/organizations/{organizationId}/enquiries`
  - `PATCH /api/v1/organizations/{organizationId}/enquiries/{enquiryId}`

Rules:

- Statuses: `NEW`, `READ`, `RESPONDED`, `CLOSED`.
- `CLOSED` is terminal.
- Enquiries cannot transition back to `NEW`.
- Any non-closed status may transition to `READ`, `RESPONDED`, or `CLOSED`.
- Updates require `version`.

### Schema

- `V8__organization_direct_ownership.sql`
  - Adds owner linkage and supports direct owner lookup.
- `V9__listing_and_enquiry_foundation.sql`
  - Adds listing and enquiry persistence, constraints, and indexes.

## Frontend

### Public Journey

- `/for-owners`
  - Owner-facing value proposition, workflow steps, and CTAs.
  - Routes to `/register?type=owner` or `/app/overview`.
- `/register?type=owner`
  - Owner-specific registration copy and API mutation.
  - Shows owner onboarding guidance after successful registration.
- `/login`
  - Uses backend-supplied roles plus `/organizations/mine` to choose a landing path.
- `/discover`
  - Public listing discovery backed by `GET /api/v1/public/listings`.
  - URL-synced search, city, type, price, and pagination filters.
- `/discover/:listingId`
  - Public listing detail.
  - Authenticated customers can submit an enquiry.

### Owner Workspace

Routes under `/app` are guarded by authentication, the owner role, and organization ownership. They use a dedicated `OrganizationLayout`.

- `/app` and `/app/overview`
  - Organization dashboard with property, listing, and enquiry metrics.
- `/app/properties`
  - Property cards, create/edit modal, and archive/status actions.
- `/app/listings`
  - Listing filters, create/edit form, publish, unpublish, archive, and public detail links.
- `/app/enquiries`
  - Enquiry filters, contact details, message display, and status transitions.
- `/app/settings`
  - Organization name update.
- `/app/account`, `/app/account/profile`, `/app/account/settings`
  - Owner personal account views and session controls.

### Customer Account Scaffold

Customer account routes exist under `/account` and `/my-enquiries`, guarded by `RequireCustomer`:

- `/account` and `/account/profile`
- `/account/settings`
- `/my-enquiries`

The backend currently exposes no customer profile or customer enquiry-list endpoints. `frontend/src/features/customer/api/customerApi.js` therefore contains speculative endpoints, and `/my-enquiries` is not backed by a real API. These pages should be treated as scaffold rather than a completed customer backend feature.

### Routing and Guards

- `RequireOrganizationOwner`: requires an authenticated owner and resolves `/organizations/mine`.
- `RequireOwnerOnboarding`: allows owners during onboarding and redirects owners with an organization to `/app/overview`.
- `RequireCustomer`: redirects non-customer users using the shared landing resolver.
- `RequirePlatform`: guards the platform workspace.
- `AuthGate`: revalidates the session and allows anonymous public pages.

## Verification Commands

```bash
# Backend
JAVA_HOME="C:\Program Files\Java\jdk-22" ./mvnw test

# Frontend
cd frontend
npm run lint
npm test -- --run
npm run build
```

## Residual Work

- Full removal of legacy membership code/tables is not included.
- MySQL 8 + Flyway migration execution and concurrency verification remain unverified in this environment.
- Customer profile and customer enquiry-list backend APIs are missing.
- Browser-level manual QA against a running backend and MySQL database remains recommended before release.
- Uncommitted changes were intentionally preserved; no commit or push was performed.
