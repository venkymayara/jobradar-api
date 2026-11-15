IF OBJECT_ID('dbo.SavedOpportunities', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.SavedOpportunities (
        user_id INT NOT NULL,
        opportunity_id INT NOT NULL,
        saved_at DATETIMEOFFSET(6) NOT NULL
            CONSTRAINT DF_saved_opportunities_saved_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
        CONSTRAINT PK_saved_opportunities PRIMARY KEY (user_id, opportunity_id),
        CONSTRAINT FK_saved_opportunities_users FOREIGN KEY (user_id)
            REFERENCES dbo.users(id) ON DELETE CASCADE,
        CONSTRAINT FK_saved_opportunities_opportunities FOREIGN KEY (opportunity_id)
            REFERENCES dbo.Opportunities(id) ON DELETE CASCADE
    );

    CREATE INDEX IX_saved_opportunities_user_saved_at
        ON dbo.SavedOpportunities (user_id, saved_at DESC, opportunity_id);
END;
