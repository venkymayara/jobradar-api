ALTER TABLE dbo.opportunity_status_history ADD review_note NVARCHAR(1000) NULL;

ALTER TABLE dbo.Opportunities DROP CONSTRAINT CK_opportunities_status;
ALTER TABLE dbo.Opportunities WITH CHECK ADD CONSTRAINT CK_opportunities_status
    CHECK (status IN ('DRAFT','PENDING','ACTIVE','PAUSED','FILLED','EXPIRED','REJECTED','REMOVED','NEEDS_DETAILS'));

ALTER TABLE dbo.opportunity_status_history DROP CONSTRAINT CK_opportunity_status_history_status;
ALTER TABLE dbo.opportunity_status_history WITH CHECK ADD CONSTRAINT CK_opportunity_status_history_status
    CHECK ((previous_status IS NULL OR previous_status IN
        ('DRAFT','PENDING','ACTIVE','PAUSED','FILLED','EXPIRED','REJECTED','REMOVED','NEEDS_DETAILS'))
        AND new_status IN ('DRAFT','PENDING','ACTIVE','PAUSED','FILLED','EXPIRED','REJECTED','REMOVED','NEEDS_DETAILS'));

