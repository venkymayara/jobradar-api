IF OBJECT_ID('dbo.external_job_provider_status', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.external_job_provider_status (
        provider NVARCHAR(80) NOT NULL,
        enabled BIT NOT NULL,
        last_successful_sync DATETIMEOFFSET(6) NULL,
        last_attempted_sync DATETIMEOFFSET(6) NULL,
        last_error NVARCHAR(1000) NULL,
        active_jobs BIGINT NOT NULL DEFAULT (0),
        connection_status NVARCHAR(20) NOT NULL,
        CONSTRAINT PK_external_job_provider_status PRIMARY KEY (provider)
    );
END;
