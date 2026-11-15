package com.jobradar.opportunity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "\"WalkInDetails\"")
public class WalkInDetailsEntity {
    @Id
    @Column(name = "opportunity_id")
    private Integer opportunityId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId
    @JoinColumn(name = "opportunity_id", nullable = false)
    private OpportunityEntity opportunity;
    @Column(name = "drive_date", nullable = false)
    private LocalDate driveDate;
    @Column(name = "start_time", nullable = false, columnDefinition = "time(0)")
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false, columnDefinition = "time(0)")
    private LocalTime endTime;
    @Column(name = "time_zone_id", nullable = false, length = 64)
    private String timeZoneId;
    @Column(name = "drive_start_at_utc", nullable = false)
    private Instant driveStartAtUtc;
    @Column(name = "drive_end_at_utc", nullable = false)
    private Instant driveEndAtUtc;
    @Column(nullable = false, length = 180, columnDefinition = "nvarchar(180)")
    private String venue;
    @Column(name = "venue_address", length = 500, columnDefinition = "nvarchar(500)")
    private String venueAddress;
    @Column(name = "contact_name", length = 120, columnDefinition = "nvarchar(120)")
    private String contactName;
    @Column(name = "contact_phone", length = 32)
    private String contactPhone;
    @Column(name = "contact_email", length = 254)
    private String contactEmail;
    @Column(length = 2000, columnDefinition = "nvarchar(2000)")
    private String instructions;
    @Column(name = "source_url", length = 2048)
    private String sourceUrl;
    @Version @Column(nullable = false)
    private long version;

    protected WalkInDetailsEntity() {}
    public WalkInDetailsEntity(OpportunityEntity opportunity, OpportunityRequest request) {
        this.opportunity = opportunity;
        update(request);
    }
    public Integer getOpportunityId() { return opportunityId; }
    public OpportunityEntity getOpportunity() { return opportunity; }
    public LocalDate getDriveDate() { return driveDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getTimeZoneId() { return timeZoneId; }
    public Instant getDriveStartAtUtc() { return driveStartAtUtc; }
    public Instant getDriveEndAtUtc() { return driveEndAtUtc; }
    public String getVenue() { return venue; }
    public String getVenueAddress() { return venueAddress; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public String getInstructions() { return instructions; }
    public String getSourceUrl() { return sourceUrl; }

    public void update(OpportunityRequest request) {
        this.driveDate = request.driveDate();
        this.startTime = request.driveStartTime().withNano(0);
        this.endTime = request.driveEndTime().withNano(0);
        this.timeZoneId = request.timeZoneId().trim();
        this.driveStartAtUtc = WalkInSchedule.toUtc(driveDate, startTime, timeZoneId);
        this.driveEndAtUtc = WalkInSchedule.toUtc(driveDate, endTime, timeZoneId);
        if (!driveEndAtUtc.isAfter(driveStartAtUtc))
            throw new com.jobradar.common.exception.BadRequestException("Drive end must be after drive start");
        if (!driveStartAtUtc.isAfter(Instant.now()))
            throw new com.jobradar.common.exception.BadRequestException("Walk-in drive must be scheduled in the future");
        this.venue = request.venue().trim();
        this.venueAddress = clean(request.venueAddress());
        this.contactName = clean(request.contactName());
        this.contactPhone = clean(request.contactPhone());
        this.contactEmail = clean(request.contactEmail());
        this.instructions = clean(request.instructions());
        this.sourceUrl = request.sourceUrl().trim();
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
