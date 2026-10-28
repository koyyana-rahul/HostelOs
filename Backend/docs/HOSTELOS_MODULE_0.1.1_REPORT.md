# HostelOS Backend — Module 0.1.1 Required Report

## Inspection Summary

- Repository root: `D:\Downloads\HostelOs`
- Backend location: `D:\Downloads\HostelOs\Backend`
- Frontend exists separately; **no frontend files were modified**
- No Git repository is initialized: `fatal: not a git repository`
- Existing package convention: `com.rahul.HostelOs`; preserved instead of forcing `com.hostelos`
- MySQL is local, not cloud: service `MySQL80`, database `hostelos`
- Initial database state: empty schema; no domain tables
- Existing defects resolved:
  - `java: error: release version 5 not supported`
  - IntelliJ Maven `ArtifactHandlerManager` Guice failure with Maven 3.10
  - Missing database configuration
  - Plaintext root credentials temporarily present in `application.properties`; removed and externalized

## 1. Files Created or Modified

### Created

| File | Purpose |
|---|---|
| `src/main/java/com/rahul/HostelOs/config/OpenApiConfig.java` | OpenAPI metadata |
| `src/main/java/com/rahul/HostelOs/common/exception/ApiError.java` | Shared API error shape |
| `src/main/java/com/rahul/HostelOs/common/exception/GlobalExceptionHandler.java` | Validation/MVC/unexpected exception handling |
| `src/main/java/com/rahul/HostelOs/security/SecurityConfig.java` | Deny-by-default security chain |
| `src/main/java/com/rahul/HostelOs/security/ApiSecurityErrorHandler.java` | JSON 401/403 responses |
| `src/main/resources/db/migration/V1__baseline.sql` | Initial Flyway baseline |
| `src/test/resources/application-test.properties` | H2 test configuration |
| `src/test/java/com/rahul/HostelOs/HostelOsApplicationTests.java` | Seven integration tests |
| `.env.example` | Shared environment-variable template without real secrets |

### Modified

| File | Change |
|---|---|
| `pom.xml` | Spring Boot dependencies, compiler release 17, build plugins |
| `src/main/resources/application.properties` | Environment-based datasource, Flyway, Actuator, OpenAPI |
| `src/main/resources/application-prod.properties` | Disable OpenAPI/Swagger in production |
| `src/main/java/com/rahul/HostelOs/HostelOsApplication.java` | Exclude default generated security user |
| `.mvn/wrapper/maven-wrapper.properties` | Maven wrapper `3.10.0` → `3.9.9` for IntelliJ compatibility |
| `.gitignore` | Ignore `.env` and environment secret files |

### Local-only, ignored

| File | Purpose |
|---|---|
| `.env` | Local MySQL credentials; not part of source and covered by `.gitignore` |

No shared documentation was changed.

## 2. Package Structure and Responsibilities

```text
com.rahul.HostelOs
├── HostelOsApplication
├── config
│   └── OpenApiConfig
├── common
│   └── exception
│       ├── ApiError
│       └── GlobalExceptionHandler
└── security
    ├── SecurityConfig
    └── ApiSecurityErrorHandler
```

- `HostelOsApplication`: application entry point; excludes generated default user auto-configuration.
- `config/`: cross-cutting application configuration.
- `common/exception/`: consistent JSON error contract and exception translation.
- `security/`: security policy and JSON authentication/authorization failures.
- `db/migration/`: Flyway migrations.
- `src/test/java/`: automated tests.

`common/response/` and `common/audit/` were intentionally not created because there is no current use case. No speculative entities, fake authentication, or empty CRUD endpoints were added.

## 3. Versions

| Component | Version |
|---|---|
| Java target/build | **17** (`17.0.16` Microsoft OpenJDK) |
| Spring Boot | **4.1.2** |
| Spring Framework | **7.0.10** |
| Spring Security | **7.1.2** |
| Spring Web MVC | Boot-managed via `spring-boot-starter-webmvc` |
| Hibernate ORM | **7.4.13.Final** |
| Maven wrapper | **3.9.9** |
| Tomcat | **11.0.27** |
| HikariCP | **7.0.2** |
| Flyway | **12.4.0** |
| MySQL Connector/J | **9.7.0** |
| MySQL server | **8.0.40** |
| Springdoc OpenAPI | **3.2.0** |
| JUnit Jupiter | **6.0.3** |
| Mockito | **5.23.0** |
| H2 test database | **2.4.240** |
| Bean Validation | Jakarta Validation via Boot 4.1.2 |

JUnit Jupiter `6.0.3` is the Boot 4.1.2-managed version and uses the Jupiter API. It supersedes the older JUnit 5 line in this environment. Mockito is available through `spring-boot-starter-test`; the current tests use HTTP integration assertions rather than direct mocks.

## 4. Configuration and Secret Management

`application.properties` now uses environment placeholders:

```properties
spring.config.import=optional:file:.env[.properties]

spring.datasource.url=${HOSTELOS_DB_URL:jdbc:mysql://localhost:3306/hostelos?...}
spring.datasource.username=${HOSTELOS_DB_USERNAME}
spring.datasource.password=${HOSTELOS_DB_PASSWORD}
```

- Local development values are stored in ignored `.env`.
- `.env.example` documents the required variables with placeholder values only.
- No plaintext database password exists in `src/main/resources` or `target/classes`.
- A scan of source/config files found the credential only in ignored `.env`.
- Production should supply the same variables through the deployment environment or secret manager.
- Database credentials are no longer hardcoded in the repository.

Security warning: the local `.env` currently points at the local MySQL `root` account because no dedicated application user exists yet. A least-privilege `hostelos` user should be created before collaboration or non-local deployment. If the previous plaintext credential was copied or shared, rotate it.

## 5. Flyway and MySQL Status

- MySQL80 service: **Running**
- Port `3306`: reachable
- Database: `hostelos`
- Server: **MySQL 8.0.40**
- Flyway config:
  - enabled
  - `baseline-on-migrate=false`
  - `clean-disabled=true`
  - `validate-on-migrate=true`
  - `out-of-order=false`
- `V1__baseline.sql`: applies `SELECT 1;`
- Flyway history table created: `hostelos.flyway_schema_history`
- Current schema version: **v1**
- No domain tables exist yet
- No schema reset, repair, clean, or destructive migration was performed

Evidence from the new externalized-config runtime check:

```text
Database: jdbc:mysql://localhost:3306/hostelos... (MySQL 8.0)
Successfully validated 1 migration
Current version of schema `hostelos`: 1
Schema `hostelos` is up to date. No migration necessary.
```

H2 test execution also applies `V1` successfully. Flyway warns that H2 `2.4.240` is newer than its verified version `2.3.232`; tests still pass.

## 6. Health and OpenAPI Endpoints

| Endpoint | Access | Verified result |
|---|---|---|
| `/actuator/health` | Public | **200**, `{"status":"UP"}` |
| `/actuator/health/**` | Public | Probes enabled |
| `/v3/api-docs` | Public | **200**, contains `HostelOS API` |
| `/swagger-ui.html` | Public | Redirects to Swagger UI |
| `/swagger-ui/**` | Public | **200** Swagger UI |
| `/error` | Public | Allowed |
| Any other route | Denied | **401** JSON |

- Only the health Actuator endpoint is exposed.
- Health details are never shown.
- OpenAPI title: `HostelOS API`, version: `v1`.
- `application-prod.properties` disables OpenAPI and Swagger UI.
- Springdoc logs startup warnings in the default profile because documentation is enabled by design in development.

## 7. Build and Test Commands with Actual Results

Command:

```cmd
set "JAVA_HOME=C:\Users\RAHUL\.jdks\ms-17.0.16"
set "PATH=C:\Users\RAHUL\.jdks\ms-17.0.16\bin;%PATH%"
mvnw.cmd -B clean verify
```

Actual result:

```text
BUILD SUCCESS
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
Total time: 27.134 s
Jar: target\HostelOs-0.0.1-SNAPSHOT.jar
Jar size: 66,714,500 bytes
```

Test coverage:

- Context startup
- Health endpoint public and sanitized
- OpenAPI endpoint public
- Swagger UI public
- Unlisted endpoint returns JSON `401`
- Validation failures return the shared error shape
- Unexpected exceptions return generic `500` without internal details

Additional runtime verification:

- Packaged jar started on an isolated port with the new `.env` configuration
- Connected to MySQL `hostelos`
- Flyway validated v1 and reported schema up to date
- `/actuator/health` returned **200**
- Process stopped after verification

The existing `8080` IntelliJ process is the earlier run. Restart it to load the externalized configuration. The new configuration was independently verified on port `8081`.

## 8. Known Limitations and Security Considerations

- Local `.env` uses the MySQL `root` account; replace it with a dedicated least-privilege user.
- IntelliJ currently runs the app with Java `22.0.1`; set Project SDK to `ms-17` for runtime parity.
- Global `JAVA_HOME` is invalid; Maven commands require the explicit JDK 17 override.
- No authentication or authorization provider exists yet. All non-public routes remain denied.
- No domain entities, repositories, or business APIs exist yet.
- No MySQL integration tests are automated; MySQL was verified manually. Tests use H2.
- No `common/response` or `common/audit` packages exist yet; they should be added only when required.
- OpenAPI is public in development and disabled by the `prod` profile.
- No Git repository exists, so there is no commit history or diff. `.gitignore` is prepared.
- C: drive has approximately **2.8 GB** free; Maven builds are possible but disk usage should be monitored.
- No frontend code or shared documentation was modified.

## 9. Next Module and Acceptance Criteria

**Proposed next module: Module 0.1.2 — Identity and Authorization Foundation**

Acceptance criteria:

1. Approve the authentication model before implementation: session-based or token-based.
2. Add a reviewed, non-destructive `V2` migration for users, roles, and authorities with proper constraints and indexes.
3. Implement password hashing using Spring Security's supported encoder; no plaintext passwords.
4. Provide one approved bootstrap/admin mechanism using environment variables or a one-time command; no seeded default credentials.
5. Replace blanket deny-all only for explicitly approved authentication routes.
6. Add OpenAPI security documentation.
7. Add tests for successful login, rejected login, disabled/locked users, unauthorized access, and secret non-disclosure.
8. Validate Flyway `V2` on H2 and MySQL only after database approval.
9. Keep `mvnw.cmd -B clean verify` green.
10. Create the dedicated MySQL application user and remove root usage from `.env`.

### Current Module 0.1.1 Acceptance Status

| Criterion | Status |
|---|---|
| Runnable Spring Boot application | Passed |
| Java 17 Maven build | Passed |
| MySQL connectivity | Passed |
| Flyway validation | Passed |
| Environment-based configuration | Passed |
| No hardcoded secret in source | Passed |
| Health endpoint | Passed |
| OpenAPI documentation | Passed |
| Automated tests | Passed, 7/7 |
| No fake authentication/entities | Passed |
| Frontend untouched | Passed |
