CREATE TABLE dbo.categories (
    id INT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    parent_category_id INT NULL,
    description NVARCHAR(500) NULL,
    icon VARCHAR(80) NULL,
    is_active BIT NOT NULL CONSTRAINT DF_categories_is_active DEFAULT (1),
    opportunity_count INT NOT NULL CONSTRAINT DF_categories_opportunity_count DEFAULT (0),
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_categories_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    updated_at DATETIMEOFFSET(6) NULL,
    created_by NVARCHAR(100) NULL,
    updated_by NVARCHAR(100) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_categories_version DEFAULT (0),
    CONSTRAINT PK_categories PRIMARY KEY (id),
    CONSTRAINT UQ_categories_name UNIQUE (name),
    CONSTRAINT UQ_categories_slug UNIQUE (slug),
    CONSTRAINT CK_categories_opportunity_count CHECK (opportunity_count >= 0),
    CONSTRAINT CK_categories_parent_not_self CHECK (parent_category_id IS NULL OR parent_category_id <> id),
    CONSTRAINT FK_categories_parent FOREIGN KEY (parent_category_id) REFERENCES dbo.categories (id)
);

CREATE TABLE dbo.job_roles (
    id INT IDENTITY(1,1) NOT NULL,
    role_name NVARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    category_id INT NOT NULL,
    is_active BIT NOT NULL CONSTRAINT DF_job_roles_is_active DEFAULT (1),
    opportunity_count INT NOT NULL CONSTRAINT DF_job_roles_opportunity_count DEFAULT (0),
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_job_roles_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    updated_at DATETIMEOFFSET(6) NULL,
    created_by NVARCHAR(100) NULL,
    updated_by NVARCHAR(100) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_job_roles_version DEFAULT (0),
    CONSTRAINT PK_job_roles PRIMARY KEY (id),
    CONSTRAINT UQ_job_roles_name UNIQUE (role_name),
    CONSTRAINT UQ_job_roles_slug UNIQUE (slug),
    CONSTRAINT CK_job_roles_opportunity_count CHECK (opportunity_count >= 0),
    CONSTRAINT FK_job_roles_categories FOREIGN KEY (category_id) REFERENCES dbo.categories (id)
);

CREATE TABLE dbo.skills (
    id INT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    is_active BIT NOT NULL CONSTRAINT DF_skills_is_active DEFAULT (1),
    opportunity_count INT NOT NULL CONSTRAINT DF_skills_opportunity_count DEFAULT (0),
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_skills_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    updated_at DATETIMEOFFSET(6) NULL,
    created_by NVARCHAR(100) NULL,
    updated_by NVARCHAR(100) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_skills_version DEFAULT (0),
    CONSTRAINT PK_skills PRIMARY KEY (id),
    CONSTRAINT UQ_skills_name UNIQUE (name),
    CONSTRAINT UQ_skills_slug UNIQUE (slug),
    CONSTRAINT CK_skills_opportunity_count CHECK (opportunity_count >= 0)
);

CREATE TABLE dbo.locations (
    id INT IDENTITY(1,1) NOT NULL,
    city NVARCHAR(100) NOT NULL,
    state NVARCHAR(100) NULL,
    country NVARCHAR(100) NOT NULL,
    is_active BIT NOT NULL CONSTRAINT DF_locations_is_active DEFAULT (1),
    opportunity_count INT NOT NULL CONSTRAINT DF_locations_opportunity_count DEFAULT (0),
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_locations_created_at DEFAULT (SYSUTCDATETIME() AT TIME ZONE 'UTC'),
    updated_at DATETIMEOFFSET(6) NULL,
    created_by NVARCHAR(100) NULL,
    updated_by NVARCHAR(100) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_locations_version DEFAULT (0),
    CONSTRAINT PK_locations PRIMARY KEY (id),
    CONSTRAINT UQ_locations_city_state_country UNIQUE (city, state, country),
    CONSTRAINT CK_locations_opportunity_count CHECK (opportunity_count >= 0)
);

CREATE INDEX IX_categories_parent_active ON dbo.categories (parent_category_id, is_active, name);
CREATE INDEX IX_job_roles_category_active ON dbo.job_roles (category_id, is_active, role_name);
CREATE INDEX IX_skills_active_popularity ON dbo.skills (is_active, opportunity_count DESC, name);
CREATE INDEX IX_locations_active_country_state ON dbo.locations (is_active, country, state, city);

-- Seed the category tree by stable slugs, never by identity IDs.
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'technology')
    INSERT INTO dbo.categories (name, slug, description) VALUES (N'Technology', 'technology', N'Technology careers and specializations');

IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'backend-development')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Backend Development', 'backend-development', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'frontend-development')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Frontend Development', 'frontend-development', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'full-stack-development')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Full Stack Development', 'full-stack-development', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'data-engineering')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Data Engineering', 'data-engineering', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'cloud-and-devops')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Cloud and DevOps', 'cloud-and-devops', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'testing')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Testing', 'testing', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'database')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Database', 'database', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'cybersecurity')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Cybersecurity', 'cybersecurity', id FROM dbo.categories WHERE slug = 'technology';
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE slug = 'mobile-development')
    INSERT INTO dbo.categories (name, slug, parent_category_id) SELECT N'Mobile Development', 'mobile-development', id FROM dbo.categories WHERE slug = 'technology';

-- Seed representative roles using category slugs rather than IDs.
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'backend-developer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Backend Developer', 'backend-developer', id FROM dbo.categories WHERE slug = 'backend-development';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'java-developer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Java Developer', 'java-developer', id FROM dbo.categories WHERE slug = 'backend-development';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'python-developer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Python Developer', 'python-developer', id FROM dbo.categories WHERE slug = 'backend-development';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'frontend-developer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Frontend Developer', 'frontend-developer', id FROM dbo.categories WHERE slug = 'frontend-development';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'full-stack-developer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Full Stack Developer', 'full-stack-developer', id FROM dbo.categories WHERE slug = 'full-stack-development';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'data-engineer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Data Engineer', 'data-engineer', id FROM dbo.categories WHERE slug = 'data-engineering';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'devops-engineer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'DevOps Engineer', 'devops-engineer', id FROM dbo.categories WHERE slug = 'cloud-and-devops';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'qa-engineer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'QA Engineer', 'qa-engineer', id FROM dbo.categories WHERE slug = 'testing';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'database-administrator')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Database Administrator', 'database-administrator', id FROM dbo.categories WHERE slug = 'database';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'cybersecurity-analyst')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Cybersecurity Analyst', 'cybersecurity-analyst', id FROM dbo.categories WHERE slug = 'cybersecurity';
IF NOT EXISTS (SELECT 1 FROM dbo.job_roles WHERE slug = 'mobile-developer')
    INSERT INTO dbo.job_roles (role_name, slug, category_id) SELECT N'Mobile Developer', 'mobile-developer', id FROM dbo.categories WHERE slug = 'mobile-development';

IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'java') INSERT INTO dbo.skills (name, slug) VALUES (N'Java', 'java');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'spring-boot') INSERT INTO dbo.skills (name, slug) VALUES (N'Spring Boot', 'spring-boot');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'python') INSERT INTO dbo.skills (name, slug) VALUES (N'Python', 'python');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'javascript') INSERT INTO dbo.skills (name, slug) VALUES (N'JavaScript', 'javascript');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'typescript') INSERT INTO dbo.skills (name, slug) VALUES (N'TypeScript', 'typescript');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'angular') INSERT INTO dbo.skills (name, slug) VALUES (N'Angular', 'angular');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'react') INSERT INTO dbo.skills (name, slug) VALUES (N'React', 'react');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'sql-server') INSERT INTO dbo.skills (name, slug) VALUES (N'SQL Server', 'sql-server');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'aws') INSERT INTO dbo.skills (name, slug) VALUES (N'AWS', 'aws');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'azure') INSERT INTO dbo.skills (name, slug) VALUES (N'Azure', 'azure');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'docker') INSERT INTO dbo.skills (name, slug) VALUES (N'Docker', 'docker');
IF NOT EXISTS (SELECT 1 FROM dbo.skills WHERE slug = 'kubernetes') INSERT INTO dbo.skills (name, slug) VALUES (N'Kubernetes', 'kubernetes');

IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Hyderabad' AND state=N'Telangana' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Hyderabad',N'Telangana',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Bengaluru' AND state=N'Karnataka' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Bengaluru',N'Karnataka',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Mumbai' AND state=N'Maharashtra' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Mumbai',N'Maharashtra',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Pune' AND state=N'Maharashtra' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Pune',N'Maharashtra',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Chennai' AND state=N'Tamil Nadu' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Chennai',N'Tamil Nadu',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Delhi' AND state=N'Delhi' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Delhi',N'Delhi',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Gurugram' AND state=N'Haryana' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Gurugram',N'Haryana',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Noida' AND state=N'Uttar Pradesh' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Noida',N'Uttar Pradesh',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Kolkata' AND state=N'West Bengal' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Kolkata',N'West Bengal',N'India');
IF NOT EXISTS (SELECT 1 FROM dbo.locations WHERE city=N'Ahmedabad' AND state=N'Gujarat' AND country=N'India') INSERT INTO dbo.locations (city,state,country) VALUES (N'Ahmedabad',N'Gujarat',N'India');
