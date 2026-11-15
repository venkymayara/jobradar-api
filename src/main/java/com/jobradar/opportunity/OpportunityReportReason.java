package com.jobradar.opportunity;

public enum OpportunityReportReason {
    FAKE_JOB, EXPIRED, WRONG_INFORMATION, DUPLICATE, SUSPICIOUS_PAYMENT,
    MISLEADING_SALARY, BROKEN_URL, INAPPROPRIATE_CONTENT,
    /** Legacy values retained so existing report rows remain readable. */
    SPAM, MISLEADING, OTHER
}
