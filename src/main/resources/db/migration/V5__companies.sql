CREATE TABLE dbo.companies (
    id INT IDENTITY(1,1) NOT NULL,
    company_name NVARCHAR(160) NOT NULL,
    website_url VARCHAR(2048) NULL,
    logo_url VARCHAR(2048) NULL,
    headquarters NVARCHAR(180) NULL,
    industry NVARCHAR(120) NULL,
    description NVARCHAR(2000) NULL,
    opportunity_count INT NOT NULL CONSTRAINT DF_companies_opportunity_count DEFAULT (0),
    is_active BIT NOT NULL CONSTRAINT DF_companies_is_active DEFAULT (0),
    is_verified BIT NOT NULL CONSTRAINT DF_companies_is_verified DEFAULT (0),
    submitted_by NVARCHAR(100) NULL,
    verified_by VARCHAR(100) NULL,
    verified_at DATETIMEOFFSET(6) NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_companies_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    updated_at DATETIMEOFFSET(6) NULL,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_companies_version DEFAULT (0),
    CONSTRAINT PK_companies PRIMARY KEY (id),
    CONSTRAINT UQ_companies_company_name UNIQUE (company_name),
    CONSTRAINT CK_companies_opportunity_count CHECK (opportunity_count >= 0),
    CONSTRAINT CK_companies_verification_metadata CHECK
        ((is_verified = 0 AND verified_at IS NULL AND verified_by IS NULL)
          OR (is_verified = 1 AND verified_at IS NOT NULL AND verified_by IS NOT NULL))
);

CREATE INDEX IX_companies_active_name ON dbo.companies (is_active, company_name);
CREATE INDEX IX_companies_active_verification ON dbo.companies (is_active, is_verified, company_name);
