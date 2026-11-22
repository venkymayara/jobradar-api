IF COL_LENGTH('dbo.users', 'phone_number') IS NULL ALTER TABLE dbo.users ADD phone_number NVARCHAR(30) NULL;
IF COL_LENGTH('dbo.users', 'location') IS NULL ALTER TABLE dbo.users ADD location NVARCHAR(160) NULL;
IF COL_LENGTH('dbo.users', 'professional_headline') IS NULL ALTER TABLE dbo.users ADD professional_headline NVARCHAR(180) NULL;
IF COL_LENGTH('dbo.users', 'skills') IS NULL ALTER TABLE dbo.users ADD skills NVARCHAR(1000) NULL;
IF COL_LENGTH('dbo.users', 'experience_years') IS NULL ALTER TABLE dbo.users ADD experience_years INT NULL;
IF COL_LENGTH('dbo.users', 'linkedin_url') IS NULL ALTER TABLE dbo.users ADD linkedin_url VARCHAR(2048) NULL;
IF COL_LENGTH('dbo.users', 'github_url') IS NULL ALTER TABLE dbo.users ADD github_url VARCHAR(2048) NULL;
IF COL_LENGTH('dbo.users', 'avatar_url') IS NULL ALTER TABLE dbo.users ADD avatar_url VARCHAR(2048) NULL;

IF OBJECT_ID('dbo.notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.notifications (
        id BIGINT IDENTITY(1,1) NOT NULL,
        user_id INT NOT NULL,
        title NVARCHAR(180) NOT NULL,
        message NVARCHAR(1000) NOT NULL,
        notification_type VARCHAR(40) NOT NULL,
        reference_type VARCHAR(40) NULL,
        reference_id INT NULL,
        is_read BIT NOT NULL CONSTRAINT DF_notifications_is_read DEFAULT (0),
        created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_notifications_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
        read_at DATETIMEOFFSET(6) NULL,
        CONSTRAINT PK_notifications PRIMARY KEY (id),
        CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
    );
    CREATE INDEX IX_notifications_user_created ON dbo.notifications(user_id, created_at DESC);
    CREATE INDEX IX_notifications_user_unread ON dbo.notifications(user_id, is_read, created_at DESC);
END;
