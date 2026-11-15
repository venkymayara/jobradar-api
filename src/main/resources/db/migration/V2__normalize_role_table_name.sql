-- Hibernate's Spring physical naming strategy maps the AppRoleEntity table to app_roles.
-- Rename only the table created in V1; SQL Server preserves its rows, indexes, and FKs.
IF OBJECT_ID('dbo.AppRoles', 'U') IS NOT NULL AND OBJECT_ID('dbo.app_roles', 'U') IS NULL
BEGIN
    EXEC sp_rename 'dbo.AppRoles', 'app_roles';
END;
