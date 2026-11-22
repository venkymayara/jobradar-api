# Flyway migrations

Versioned SQL Server migrations own all schema changes. The existing database is baselined at version 0 in development; new migrations begin with `V1`. Never edit a migration after it has been applied to a shared database.

The application uses the account and role tables, the catalog, opportunities and their detail and association tables, saved items, reports and review history, notifications, the external job cache and provider status. Flyway tracks migrations in `flyway_schema_history`. The admin user directory is a view over the existing users and roles.

The local database also contains a legacy `dbo.details` table. JobRadar has no entity, query, or migration dependency on it. Leave its existing records in place when deploying or upgrading JobRadar.
