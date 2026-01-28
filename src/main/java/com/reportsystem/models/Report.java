package com.reportsystem.models;

import java.util.Date;
import java.util.UUID;

public class Report {

    private final String id;
    private final UUID reporterUUID;
    private final String reporterName;
    private final String violatorName;
    private final UUID violatorUUID; // UUID нарушителя (может быть null в offline-mode)
    private final String reason;
    private final String comment;
    private final Date createdAt;
    private ReportStatus status;
    private String adminComment;
    private String reviewedBy;
    private String reviewedById; // Discord ID проверяющего для упоминания
    private Date reviewedAt;

    // Конструктор без UUID (обратная совместимость)
    public Report(String id, UUID reporterUUID, String reporterName, String violatorName,
                  String reason, String comment) {
        this(id, reporterUUID, reporterName, violatorName, null, reason, comment);
    }

    // Основной конструктор
    public Report(String id, UUID reporterUUID, String reporterName, String violatorName,
                  UUID violatorUUID, String reason, String comment) {
        this.id = id;
        this.reporterUUID = reporterUUID;
        this.reporterName = reporterName;
        this.violatorName = violatorName;
        this.violatorUUID = violatorUUID;
        this.reason = reason;
        this.comment = comment;
        this.createdAt = new Date();
        this.status = ReportStatus.PENDING;
    }

    // Геттеры
    public String getId() {
        return id;
    }

    public UUID getReporterUUID() {
        return reporterUUID;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getViolatorName() {
        return violatorName;
    }

    public UUID getViolatorUUID() {
        return violatorUUID;
    }

    public String getReason() {
        return reason;
    }

    public String getComment() {
        return comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(String reviewedById) {
        this.reviewedById = reviewedById;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public enum ReportStatus {
        PENDING("pending"),
        APPROVED("approved"),
        REJECTED("rejected");

        private final String key;

        ReportStatus(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static ReportStatus fromKey(String key) {
            for (ReportStatus status : values()) {
                if (status.key.equalsIgnoreCase(key)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
}