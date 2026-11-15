-- Hibernate maps Instant to datetimeoffset on SQL Server. Existing datetime2 values
-- represent UTC instants from V1, so preserve the clock value and assign +00:00.
DROP INDEX IX_refresh_tokens_expiration ON dbo.refresh_tokens;
ALTER TABLE dbo.refresh_tokens ALTER COLUMN created_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE dbo.refresh_tokens ALTER COLUMN expires_at DATETIMEOFFSET(6) NOT NULL;
ALTER TABLE dbo.refresh_tokens ALTER COLUMN revoked_at DATETIMEOFFSET(6) NULL;
CREATE INDEX IX_refresh_tokens_expiration ON dbo.refresh_tokens (expires_at) INCLUDE (revoked_at);
