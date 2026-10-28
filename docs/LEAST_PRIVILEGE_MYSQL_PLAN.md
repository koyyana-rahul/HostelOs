# Least-Privilege MySQL Provisioning Plan

**Status**: Documented for future approval. Not executed in this task.

## Overview

The local MySQL database uses the `root` account (`HOSTELOS_DB_USERNAME=root`,
`HOSTELOS_DB_PASSWORD=#Rahul8620` in `Backend/.env`). This is acceptable for
local development and testing but must not be used in production. A dedicated
least-privilege application user must be created before any production deployment.

## Provisioning Plan

### 1. Create dedicated MySQL user

```sql
-- Create the application user
CREATE USER IF NOT EXISTS 'hostelos_app'@'localhost' IDENTIFIED BY '<strong-password>';

-- Grant precise privileges for the hostelos schema
GRANT SELECT, INSERT, UPDATE ON hostelos.user_accounts TO 'hostelos_app'@'localhost';
GRANT SELECT ON hostelos.roles TO 'hostelos_app'@'localhost';
GRANT SELECT ON hostelos.system_info TO 'hostelos_app'@'localhost';
GRANT LOCK TABLES ON hostelos.* TO 'hostelos_app'@'localhost';
```

### 2. Update backend configuration

Replace `Backend/.env` values:

```
HOSTELOS_DB_USERNAME=hostelos_app
HOSTELOS_DB_PASSWORD=<new-generated-password>
```

**Do not commit the real `.env` file.** The `Backend/.gitignore` already excludes
`.env`. Only `.env.example` is tracked as a template.

### 3. Migration privileges

The Flyway migration process requires different privileges than runtime access:

| Operation | Required Privilege | User |
|---|---|---|
| `Flyway info/migrate` | `SELECT on `information_schema`.`tables`` | `hostelos_app` or `root` |
| Baseline creation | `CREATE, ALTER on mysql.migrations table` | `root` only |
| Production migration | `SELECT, INSERT on roles/user_accounts/user_roles` | `hostelos_app` |

**Recommendation**: Keep Flyway operations performed via `root` in a controlled
CI/CD step, while runtime application connects via `hostelos_app`.

### 4. Schema migration privileges

Future Flyway V4+ migrations require the application user to have:

- `ALTER, CREATE, DROP` on the `flyway_schema_history` table (typically owned by
  the user who baseline'd it)
- Or continue using `root` for Flyway administration steps only

### 5. Deployment flow

1. **Development**: `root` account via `Backend/.env` (already configured, acceptable
   for local dev only)
2. **CI/CD**: Flywall administration via `root` in the CI pipeline
3. **Production**: Application connects via `hostelos_app` with limited privileges;
   Flyway migrations triggered as a separate CI/CD step with `root` privileges

### 6. Credential rotation

- Generate a strong password (>= 20 characters, mixed types)
- Rotate the `Backend/.env` password after initial provisioning
- Never commit the real `.env`; keep it git-untracked (already configured)
- Update the CI workflow secret store with the new password

### 7. Verification (not executed in this task)

After provisioning, verify:

```sql
-- Test application user can read required data
SELECT COUNT(*) FROM hostelos.user_accounts WHERE status = 'ACTIVE';
SELECT role_name FROM hostelos.roles;

-- Test application user cannot perform DDL
ALTER TABLE hostelos.user_accounts ADD COLUMN test_col INT; -- Should fail
```

## Gaps and Follow-up

- **Password generation**: A strong password must be generated and secured in a
  password manager or secret manager. Do not hardcode in documentation.
- **CI secret store**: Update GitHub Actions secrets with the new credentials.
- **Flyway configuration**: Ensure `flyway.conf` or `pom.xml` properties are
  updated to reflect the new user if Flywall is configured to auto-detect the
  runtime user.
- **Root account retirement**: Plan to eventually retire the `root` account from
  application use entirely, with all migration steps moved to a dedicated
  administration pipeline.

---

*This plan documents the recommended next steps for MySQL credential hardening.
It does not execute any database operations and does not modify the existing
`Backend/.env` or database configuration.*