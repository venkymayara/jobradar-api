# JobRadar API

Spring Boot backend foundation for the separate JobRadar Angular frontend.

## Phase 1 status

- Java 25 / Spring Boot 3.5 baseline.
- Spring MVC, Spring Data JPA, Jakarta Validation, SQL Server JDBC, Flyway, and Actuator dependencies.
- Development uses the supplied integrated-security SQL Server URL for `ven_db`. Both profiles use Hibernate `validate`; Flyway owns schema changes. Development SQL logging is enabled and formatted.
- Flyway is configured for `classpath:db/migration`; an existing SQL Server schema is baselined at version 0 and V1–V3 upgrade the account and refresh-token tables.
- The standard health endpoint is `/actuator/health`.
- CORS is configurable with `CORS_ALLOWED_ORIGINS`; local development defaults to `http://localhost:4200`.

Phase 3 adds authentication, JWT security, and refresh token rotation. Public opportunity and homepage routes are allowlisted for future modules; no opportunity APIs are implemented yet.

## Local setup

1. Install Maven 3.6.3 or later and use the installed Java 25 runtime.
2. Ensure SQL Server listens on `localhost:1433`, the `ven_db` database exists, and the Windows account running Spring Boot has access.
3. For 64-bit Windows integrated authentication, fetch the DLL matching the JDBC driver version and add it to this process's `PATH`:

   ```powershell
   mvn dependency:copy "-Dartifact=com.microsoft.sqlserver:mssql-jdbc_auth:13.6.0.x64:dll" "-DoutputDirectory=target/native"
   $env:PATH = "$PWD\target\native;$env:PATH"
   ```

4. Run `mvn spring-boot:run` from this folder.
5. Check `http://localhost:8081/actuator/health`.

For production, set `SPRING_PROFILES_ACTIVE=prod` and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 random bytes), and `CORS_ALLOWED_ORIGINS` through the deployment environment or secret manager. Production refresh cookies require HTTPS and use `Secure; HttpOnly; SameSite=Strict`.

No production credentials are stored in this repository. The V1 migration preserves existing users and role assignments, seeds `AppRoles`, adds profile metadata, and creates the refresh token hash table. Existing orphan rows in `user_roles` are retained; the user relationship constraint is added without validating those historical rows.

Authentication endpoints are `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, and `POST /api/v1/auth/logout`. The Angular app first calls `GET /api/v1/auth/csrf`; the XSRF cookie is readable by the app, while the refresh token cookie is HttpOnly. Access tokens are returned in the response body and should remain in application memory.

Sessions last eight hours from sign-in by default. Set `AUTH_SESSION_TTL` to an ISO-8601 duration to configure the maximum lifetime. Refreshing a token preserves the original session deadline.
## Super Admin

Migration V16 creates the Super Admin role. To designate the first Super Admin, a database operator assigns it to a specific existing account. Use that account’s sign-in ID:

```sql
DECLARE @username VARCHAR(100) = 'designated-account@example.com';

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = @username)
    THROW 50000, 'The designated account does not exist.', 1;

INSERT INTO dbo.user_roles (username, role)
SELECT @username, 'ROLE_SUPER_ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.user_roles WHERE username = @username AND role = 'ROLE_SUPER_ADMIN'
);
```

Sign in again to refresh the dashboard permissions. Super Admins can grant and revoke Admin access for other accounts from **Dashboard → User administration**. Each change records the target account, acting account, role, and time in the existing permission history.
