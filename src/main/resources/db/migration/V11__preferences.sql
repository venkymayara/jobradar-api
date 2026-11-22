IF COL_LENGTH('dbo.users', 'email_notifications_enabled') IS NULL
    ALTER TABLE dbo.users ADD email_notifications_enabled BIT NOT NULL CONSTRAINT DF_users_email_notifications_enabled DEFAULT (1);
IF COL_LENGTH('dbo.users', 'job_alerts_enabled') IS NULL
    ALTER TABLE dbo.users ADD job_alerts_enabled BIT NOT NULL CONSTRAINT DF_users_job_alerts_enabled DEFAULT (1);
