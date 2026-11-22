INSERT INTO dbo.app_roles (role)
SELECT 'ROLE_SUPER_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM dbo.app_roles WHERE role = 'ROLE_SUPER_ADMIN');

ALTER TABLE dbo.moderator_permission_history
    ADD changed_role VARCHAR(32) NOT NULL
        CONSTRAINT DF_moderator_permission_history_role DEFAULT 'ROLE_MODERATOR';
GO

ALTER VIEW dbo.admin_user_directory
AS
SELECT u.id AS user_id,
       u.username,
       COALESCE(NULLIF(TRIM(u.full_name), ''), u.username) AS display_name,
       CAST(CASE WHEN EXISTS (
           SELECT 1 FROM dbo.user_roles r WHERE r.username = u.username AND r.role = 'ROLE_ADMIN'
       ) THEN 1 ELSE 0 END AS bit) AS administrator,
       CAST(CASE WHEN EXISTS (
           SELECT 1 FROM dbo.user_roles r WHERE r.username = u.username AND r.role = 'ROLE_MODERATOR'
       ) THEN 1 ELSE 0 END AS bit) AS moderator,
       CAST(CASE WHEN EXISTS (
           SELECT 1 FROM dbo.user_roles r WHERE r.username = u.username AND r.role = 'ROLE_SUPER_ADMIN'
       ) THEN 1 ELSE 0 END AS bit) AS super_administrator
FROM dbo.users u;
