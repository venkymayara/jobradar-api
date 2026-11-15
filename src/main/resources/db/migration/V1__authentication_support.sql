-- Extend the existing legacy users table without replacing or deleting account data.
IF COL_LENGTH('dbo.users', 'full_name') IS NULL
BEGIN
    ALTER TABLE dbo.users ADD full_name NVARCHAR(150) NULL;
END;

-- Roles are normalized while existing role assignments remain in dbo.user_roles.
IF OBJECT_ID('dbo.AppRoles', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppRoles (
        role VARCHAR(255) NOT NULL,
        CONSTRAINT PK_AppRoles PRIMARY KEY (role)
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.AppRoles WHERE role = 'ROLE_USER')
    INSERT INTO dbo.AppRoles (role) VALUES ('ROLE_USER');
IF NOT EXISTS (SELECT 1 FROM dbo.AppRoles WHERE role = 'ROLE_MODERATOR')
    INSERT INTO dbo.AppRoles (role) VALUES ('ROLE_MODERATOR');
IF NOT EXISTS (SELECT 1 FROM dbo.AppRoles WHERE role = 'ROLE_ADMIN')
    INSERT INTO dbo.AppRoles (role) VALUES ('ROLE_ADMIN');

-- Keep the two pre-existing orphan assignments. WITH NOCHECK preserves them while
-- enforcing this relationship for new and changed rows.
-- SQL Server requires matching column widths for a string FK. The largest current
-- username in user_roles is 6 bytes; restore the existing composite uniqueness rule.
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_user_roles_username_role')
BEGIN
    ALTER TABLE dbo.user_roles DROP CONSTRAINT UQ_user_roles_username_role;
END;

ALTER TABLE dbo.user_roles ALTER COLUMN username VARCHAR(100) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_user_roles_username_role')
BEGIN
    ALTER TABLE dbo.user_roles ADD CONSTRAINT UQ_user_roles_username_role UNIQUE (username, role);
END;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_user_roles_users_username')
BEGIN
    ALTER TABLE dbo.user_roles WITH NOCHECK
        ADD CONSTRAINT FK_user_roles_users_username
        FOREIGN KEY (username) REFERENCES dbo.users (username);
END;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_user_roles_AppRoles_role')
BEGIN
    ALTER TABLE dbo.user_roles WITH CHECK
        ADD CONSTRAINT FK_user_roles_AppRoles_role
        FOREIGN KEY (role) REFERENCES dbo.AppRoles (role);
END;

CREATE TABLE dbo.refresh_tokens (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id INT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    token_family_id UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIME2(7) NOT NULL,
    expires_at DATETIME2(7) NOT NULL,
    revoked_at DATETIME2(7) NULL,
    replaced_by_id BIGINT NULL,
    version BIGINT NOT NULL CONSTRAINT DF_refresh_tokens_version DEFAULT (0),
    CONSTRAINT PK_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT UQ_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT FK_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT FK_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES dbo.refresh_tokens (id)
);

CREATE INDEX IX_refresh_tokens_user_family ON dbo.refresh_tokens (user_id, token_family_id);
CREATE INDEX IX_refresh_tokens_expiration ON dbo.refresh_tokens (expires_at) INCLUDE (revoked_at);
