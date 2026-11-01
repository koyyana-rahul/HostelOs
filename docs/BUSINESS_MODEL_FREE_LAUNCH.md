# HostelOS — Free Access Model (₹0 Membership)

## 1. Policy

HostelOS is currently operating under a **FREE ACCESS MODEL**.

| Item | Price |
|---|---:|
| Customer registration | ₹0 |
| Hostel owner registration | ₹0 |
| Organization creation | ₹0 |
| Organization membership | ₹0 |
| Monthly subscription | ₹0 |
| Annual subscription | ₹0 |
| Joining fee | ₹0 |
| Platform access | ₹0 |
| Renewal fee | ₹0 |
| Activation fee | ₹0 |

No payment is required to use any currently implemented HostelOS functionality.

## 2. Current subscription architecture

As of this phase, HostelOS does **not** contain a subscription entity, plan, payment
gateway, or entitlement system.

- No `Subscription` entity exists in the backend.
- No `Plan`, `Invoice`, `Payment`, or `Billing` module exists.
- No checkout page or payment redirect exists.
- The only references to subscriptions are:
  - `frontend/src/app/routes.js` — a `/platform/subscriptions` route that renders
    `PlatformPlaceholderPage`.
  - `frontend/src/features/organizations/lib/roleNav.js` — a nav item pointing to
    `/platform/subscriptions`.
  - `docs/ROADMAP.md` — billing planned for module `0.1.6`.

Because no payment infrastructure exists, no payment code was removed or disabled.
The free model is the **default state** of the application.

## 3. Organization onboarding

Customer visits HostelOS
→ Registers a CUSTOMER account
→ Logs in
→ Opens organization onboarding
→ Enters organization details
→ Clicks Create Organization
→ Backend creates the organization (`OrganizationProvisioningService`)
→ Backend creates an active `ORG_ADMIN` membership
→ No payment request is generated
→ Organization becomes usable according to normal authorization rules
→ User is redirected to `/app`.

The organization is persisted in the actual database. No simulated success.

## 4. Role and membership distinctions

The free access model does **not** change authorization.

- `CUSTOMER` — global account role; no automatic organization membership.
- `ORG_ADMIN` — organization membership role; granted only by organization creation
  or explicit membership management.
- `ORG_STAFF` — organization membership role; limited permissions.
- `PLATFORM_ADMIN` — global authority; not granted by organization membership.

A customer must not become `PLATFORM_ADMIN`. A user must not gain access to another
organization's data. Active/revoked/suspended membership rules and tenant isolation
remain fully enforced.

## 5. Frontend changes

- `PlatformPlaceholderPage` now displays the current plan as `FREE · ₹0` with an
  honest "not yet implemented" state for subscription management.
- No payment buttons or checkout redirects were introduced.

## 6. Backend changes

No backend changes were required. Organization creation
(`OrganizationProvisioningService.provisionOrganization`) already creates the
organization and an active `ORG_ADMIN` membership in a single transaction. No payment
status is used as an access prerequisite anywhere in the existing security or
authorization configuration.

## 7. Database safety

No Flyway migrations were executed. No production database was accessed. No schema
changes were made.

## 8. Future paid-plan considerations

When paid plans are introduced (module `0.1.6`):

- A `Subscription` entity and `Plan` enum should be added.
- A `FREE` plan with price `₹0`, currency `INR`, billing interval `NONE`,
  `paymentRequired = false`, and no expiry should be the default for all existing
  organizations.
- Existing authorization rules must remain independent of subscription state during
  the free-launch period.

## 9. Features intentionally deferred

- Billing, invoices, payments, dues, and receipts (module `0.1.6`).
- Subscription management UI (placeholder only).
- Payment gateway integration.