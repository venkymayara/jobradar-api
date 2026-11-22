IF OBJECT_ID('dbo.external_jobs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.external_jobs (
        external_id NVARCHAR(200) NOT NULL,
        title NVARCHAR(300) NOT NULL,
        company_name NVARCHAR(200) NOT NULL,
        company_logo_url VARCHAR(2048) NULL,
        description NVARCHAR(MAX) NULL,
        location NVARCHAR(300) NULL,
        city NVARCHAR(120) NULL,
        country NVARCHAR(120) NULL,
        employment_type NVARCHAR(80) NULL,
        experience_level NVARCHAR(80) NULL,
        salary NVARCHAR(200) NULL,
        skills NVARCHAR(2000) NULL,
        work_mode NVARCHAR(40) NULL,
        job_url VARCHAR(2048) NULL,
        application_url VARCHAR(2048) NOT NULL,
        provider NVARCHAR(80) NOT NULL,
        posted_at DATETIMEOFFSET(6) NULL,
        fetched_at DATETIMEOFFSET(6) NOT NULL,
        expires_at DATE NULL,
        CONSTRAINT PK_external_jobs PRIMARY KEY (external_id)
    );
    CREATE INDEX IX_external_jobs_provider_posted ON dbo.external_jobs(provider, posted_at DESC);
END;
