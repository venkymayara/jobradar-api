CREATE VIEW dbo.admin_user_directory
AS
SELECT u.id AS user_id,
       u.username AS username,
       COALESCE(NULLIF(TRIM(u.full_name), ''), u.username) AS display_name,
       CAST(CASE WHEN EXISTS (
           SELECT 1 FROM dbo.user_roles r WHERE r.username = u.username AND r.role = 'ROLE_ADMIN'
       ) THEN 1 ELSE 0 END AS bit) AS administrator,
       CAST(CASE WHEN EXISTS (
           SELECT 1 FROM dbo.user_roles r WHERE r.username = u.username AND r.role = 'ROLE_MODERATOR'
       ) THEN 1 ELSE 0 END AS bit) AS moderator
FROM dbo.users u;

