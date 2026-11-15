CREATE TABLE dbo.WalkInDetails (
    opportunity_id INT NOT NULL,
    drive_date DATE NOT NULL,
    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,
    time_zone_id VARCHAR(64) NOT NULL,
    drive_start_at_utc DATETIMEOFFSET(6) NOT NULL,
    drive_end_at_utc DATETIMEOFFSET(6) NOT NULL,
    venue NVARCHAR(180) NOT NULL,
    venue_address NVARCHAR(500) NULL,
    contact_name NVARCHAR(120) NULL,
    contact_phone VARCHAR(32) NULL,
    contact_email VARCHAR(254) NULL,
    instructions NVARCHAR(2000) NULL,
    source_url VARCHAR(2048) NULL,
    version BIGINT NOT NULL CONSTRAINT DF_walk_in_details_version DEFAULT (0),
    CONSTRAINT PK_walk_in_details PRIMARY KEY (opportunity_id),
    CONSTRAINT FK_walk_in_details_opportunities FOREIGN KEY (opportunity_id) REFERENCES dbo.Opportunities(id),
    CONSTRAINT CK_walk_in_details_local_time CHECK (end_time > start_time),
    CONSTRAINT CK_walk_in_details_utc_time CHECK (drive_end_at_utc > drive_start_at_utc),
    CONSTRAINT CK_walk_in_details_timezone CHECK (LEN(time_zone_id) > 0)
);

CREATE INDEX IX_walk_in_details_date_start ON dbo.WalkInDetails (drive_start_at_utc, opportunity_id);
CREATE INDEX IX_walk_in_details_timezone_date ON dbo.WalkInDetails (time_zone_id, drive_date);
CREATE INDEX IX_walk_in_details_expiration ON dbo.WalkInDetails (drive_end_at_utc, opportunity_id);

-- Preserve any Phase 6 walk-in data and assign the India timezone used by the configured deployment.
INSERT INTO dbo.WalkInDetails (opportunity_id, drive_date, start_time, end_time, time_zone_id,
        drive_start_at_utc, drive_end_at_utc, venue, venue_address, source_url)
SELECT o.id, o.drive_date, o.drive_start_time, o.drive_end_time, 'Asia/Kolkata',
       TODATETIMEOFFSET(CONVERT(datetime2, CONVERT(varchar(10), o.drive_date, 23) + ' ' + CONVERT(varchar(8), o.drive_start_time, 108)), '+05:30') AT TIME ZONE 'UTC',
       TODATETIMEOFFSET(CONVERT(datetime2, CONVERT(varchar(10), o.drive_date, 23) + ' ' + CONVERT(varchar(8), o.drive_end_time, 108)), '+05:30') AT TIME ZONE 'UTC',
       o.venue, o.venue_address, o.source_url
FROM dbo.Opportunities o
WHERE o.opportunity_type = 'WALK_IN'
  AND o.drive_date IS NOT NULL AND o.drive_start_time IS NOT NULL AND o.drive_end_time > o.drive_start_time
  AND o.venue IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.WalkInDetails d WHERE d.opportunity_id = o.id);

IF EXISTS (
    SELECT 1 FROM dbo.Opportunities o
    WHERE o.opportunity_type = 'WALK_IN'
      AND NOT EXISTS (SELECT 1 FROM dbo.WalkInDetails d WHERE d.opportunity_id = o.id)
)
    THROW 51000, 'Every WALK_IN opportunity must have WalkInDetails before schema migration can continue.', 1;

ALTER TABLE dbo.Opportunities DROP COLUMN drive_date, drive_start_time, drive_end_time, venue, venue_address;
