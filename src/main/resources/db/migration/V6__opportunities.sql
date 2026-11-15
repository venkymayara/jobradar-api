CREATE TABLE dbo.Opportunities (
    id INT IDENTITY(1,1) NOT NULL,
    title NVARCHAR(200) NOT NULL,
    company_id INT NOT NULL,
    location_id INT NOT NULL,
    work_mode VARCHAR(20) NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    min_experience INT NOT NULL,
    max_experience INT NOT NULL,
    min_salary DECIMAL(15,2) NULL,
    max_salary DECIMAL(15,2) NULL,
    currency_code VARCHAR(3) NOT NULL CONSTRAINT DF_opportunities_currency DEFAULT ('INR'),
    opportunity_type VARCHAR(20) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    application_url VARCHAR(2048) NULL,
    source_url VARCHAR(2048) NULL,
    application_deadline DATE NULL,
    drive_date DATE NULL,
    drive_start_time TIME(0) NULL,
    drive_end_time TIME(0) NULL,
    venue NVARCHAR(180) NULL,
    venue_address NVARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_opportunities_status DEFAULT ('PENDING'),
    is_verified BIT NOT NULL CONSTRAINT DF_opportunities_verified DEFAULT (0),
    published_at DATETIMEOFFSET(6) NULL,
    created_by_user_id INT NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_opportunities_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    updated_at DATETIMEOFFSET(6) NULL,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_opportunities_version DEFAULT (0),
    CONSTRAINT PK_opportunities PRIMARY KEY (id),
    CONSTRAINT FK_opportunities_companies FOREIGN KEY (company_id) REFERENCES dbo.companies(id),
    CONSTRAINT FK_opportunities_locations FOREIGN KEY (location_id) REFERENCES dbo.locations(id),
    CONSTRAINT FK_opportunities_users FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(id),
    CONSTRAINT CK_opportunities_type CHECK (opportunity_type IN ('JOB','WALK_IN','INTERNSHIP','REFERRAL')),
    CONSTRAINT CK_opportunities_status CHECK (status IN ('DRAFT','PENDING','ACTIVE','PAUSED','FILLED','EXPIRED','REJECTED','REMOVED')),
    CONSTRAINT CK_opportunities_work_mode CHECK (work_mode IN ('ON_SITE','HYBRID','REMOTE')),
    CONSTRAINT CK_opportunities_employment_type CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','TEMPORARY')),
    CONSTRAINT CK_opportunities_experience CHECK (min_experience >= 0 AND max_experience >= min_experience),
    CONSTRAINT CK_opportunities_salary CHECK ((min_salary IS NULL OR min_salary >= 0) AND (max_salary IS NULL OR max_salary >= 0) AND (min_salary IS NULL OR max_salary IS NULL OR max_salary >= min_salary)),
    CONSTRAINT CK_opportunities_verified CHECK (is_verified = 0 OR status = 'ACTIVE'),
    CONSTRAINT CK_opportunities_published CHECK (status <> 'ACTIVE' OR published_at IS NOT NULL)
);

CREATE TABLE dbo.OpportunityRoles (
    opportunity_id INT NOT NULL,
    job_role_id INT NOT NULL,
    CONSTRAINT PK_opportunity_roles PRIMARY KEY (opportunity_id, job_role_id),
    CONSTRAINT FK_opportunity_roles_opportunities FOREIGN KEY (opportunity_id) REFERENCES dbo.Opportunities(id),
    CONSTRAINT FK_opportunity_roles_job_roles FOREIGN KEY (job_role_id) REFERENCES dbo.job_roles(id)
);

CREATE TABLE dbo.OpportunitySkills (
    opportunity_id INT NOT NULL,
    skill_id INT NOT NULL,
    CONSTRAINT PK_opportunity_skills PRIMARY KEY (opportunity_id, skill_id),
    CONSTRAINT FK_opportunity_skills_opportunities FOREIGN KEY (opportunity_id) REFERENCES dbo.Opportunities(id),
    CONSTRAINT FK_opportunity_skills_skills FOREIGN KEY (skill_id) REFERENCES dbo.skills(id)
);

CREATE TABLE dbo.opportunity_status_history (
    id BIGINT IDENTITY(1,1) NOT NULL,
    opportunity_id INT NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_by NVARCHAR(100) NOT NULL,
    changed_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_opportunity_status_history_changed_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    CONSTRAINT PK_opportunity_status_history PRIMARY KEY (id),
    CONSTRAINT FK_opportunity_status_history_opportunities FOREIGN KEY (opportunity_id) REFERENCES dbo.Opportunities(id),
    CONSTRAINT CK_opportunity_status_history_status CHECK
        ((previous_status IS NULL OR previous_status IN ('DRAFT','PENDING','ACTIVE','PAUSED','FILLED','EXPIRED','REJECTED','REMOVED'))
         AND new_status IN ('DRAFT','PENDING','ACTIVE','PAUSED','FILLED','EXPIRED','REJECTED','REMOVED'))
);

CREATE TABLE dbo.OpportunityReports (
    id INT IDENTITY(1,1) NOT NULL,
    opportunity_id INT NOT NULL,
    reported_by_user_id INT NOT NULL,
    reason VARCHAR(20) NOT NULL,
    details NVARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_opportunity_reports_status DEFAULT ('OPEN'),
    reviewed_by NVARCHAR(100) NULL,
    reviewed_at DATETIMEOFFSET(6) NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_opportunity_reports_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    CONSTRAINT PK_opportunity_reports PRIMARY KEY (id),
    CONSTRAINT FK_opportunity_reports_opportunities FOREIGN KEY (opportunity_id) REFERENCES dbo.Opportunities(id),
    CONSTRAINT FK_opportunity_reports_users FOREIGN KEY (reported_by_user_id) REFERENCES dbo.users(id),
    CONSTRAINT CK_opportunity_reports_reason CHECK (reason IN ('SPAM','MISLEADING','EXPIRED','DUPLICATE','OTHER')),
    CONSTRAINT CK_opportunity_reports_status CHECK (status IN ('OPEN','REVIEWED','DISMISSED')),
    CONSTRAINT CK_opportunity_reports_review CHECK
        ((status = 'OPEN' AND reviewed_by IS NULL AND reviewed_at IS NULL)
         OR (status <> 'OPEN' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL))
);

CREATE INDEX IX_opportunities_status_published ON dbo.Opportunities (status, published_at DESC)
    INCLUDE (title, company_id, location_id, opportunity_type, work_mode, employment_type);
CREATE INDEX IX_opportunities_company_status ON dbo.Opportunities (company_id, status, published_at DESC);
CREATE INDEX IX_opportunities_location_status ON dbo.Opportunities (location_id, status, published_at DESC);
CREATE INDEX IX_opportunity_roles_role ON dbo.OpportunityRoles (job_role_id, opportunity_id);
CREATE INDEX IX_opportunity_skills_skill ON dbo.OpportunitySkills (skill_id, opportunity_id);
CREATE INDEX IX_opportunity_status_history_timeline ON dbo.opportunity_status_history (opportunity_id, changed_at);
CREATE UNIQUE INDEX UX_opportunity_reports_open_reporter ON dbo.OpportunityReports (opportunity_id, reported_by_user_id) WHERE status = 'OPEN';
CREATE INDEX IX_opportunity_reports_queue ON dbo.OpportunityReports (status, created_at);
