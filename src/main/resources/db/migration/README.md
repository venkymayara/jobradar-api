# Flyway migrations

Versioned SQL Server migrations own all schema changes. The existing database is baselined at version 0 in development; new migrations begin with `V1`. Never edit a migration after it has been applied to a shared database.
