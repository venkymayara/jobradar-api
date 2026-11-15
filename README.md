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
# jobradar-api
