package com.jobradar.taxonomy.common;

import com.jobradar.common.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class TaxonomyEntity extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Column(name = "opportunity_count", nullable = false)
    private int opportunityCount;
    @Version @Column(nullable = false)
    private long version;
    public Integer getId() { return id; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getOpportunityCount() { return opportunityCount; }
}
