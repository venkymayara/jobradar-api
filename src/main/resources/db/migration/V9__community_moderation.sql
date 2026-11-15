ALTER TABLE dbo.OpportunityReports DROP CONSTRAINT CK_opportunity_reports_reason;
ALTER TABLE dbo.OpportunityReports ALTER COLUMN reason VARCHAR(30) NOT NULL;

IF COL_LENGTH('dbo.OpportunityReports', 'version') IS NULL
    ALTER TABLE dbo.OpportunityReports ADD version BIGINT NOT NULL
        CONSTRAINT DF_opportunity_reports_version DEFAULT (0);

ALTER TABLE dbo.OpportunityReports ADD CONSTRAINT CK_opportunity_reports_reason CHECK
    (reason IN ('FAKE_JOB','EXPIRED','WRONG_INFORMATION','DUPLICATE','SUSPICIOUS_PAYMENT',
                'MISLEADING_SALARY','BROKEN_URL','INAPPROPRIATE_CONTENT',
                -- Retained for previously submitted reports; the public request validator rejects these.
                'SPAM','MISLEADING','OTHER'));

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_opportunity_reports_reporter_created')
    CREATE INDEX IX_opportunity_reports_reporter_created
        ON dbo.OpportunityReports (reported_by_user_id, created_at DESC);

CREATE TABLE dbo.moderator_permission_history (
    id BIGINT IDENTITY(1,1) NOT NULL,
    target_user_id INT NOT NULL,
    changed_by_user_id INT NOT NULL,
    enabled BIT NOT NULL,
    changed_at DATETIMEOFFSET(6) NOT NULL
        CONSTRAINT DF_moderator_permission_history_changed_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    CONSTRAINT PK_moderator_permission_history PRIMARY KEY (id),
    CONSTRAINT FK_moderator_permission_history_target FOREIGN KEY (target_user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_moderator_permission_history_actor FOREIGN KEY (changed_by_user_id) REFERENCES dbo.users(id)
);

CREATE INDEX IX_moderator_permission_history_target_time
    ON dbo.moderator_permission_history (target_user_id, changed_at DESC);
