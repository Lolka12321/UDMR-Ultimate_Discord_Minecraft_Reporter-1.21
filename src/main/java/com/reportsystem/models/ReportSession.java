package com.reportsystem.models;

import org.bukkit.entity.Player;

import java.util.UUID;

public class ReportSession {

    private final UUID playerUUID;
    private SessionStep currentStep;
    private String violatorName;
    private String reason;
    private String comment;
    private long createdAt;

    public ReportSession(Player player) {
        this.playerUUID = player.getUniqueId();
        this.currentStep = SessionStep.VIOLATOR_NAME;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public SessionStep getCurrentStep() {
        return currentStep;
    }

    public void nextStep() {
        switch (currentStep) {
            case VIOLATOR_NAME:
                currentStep = SessionStep.REASON;
                break;
            case REASON:
                currentStep = SessionStep.COMMENT;
                break;
            case COMMENT:
                currentStep = SessionStep.CONFIRM;
                break;
            case CONFIRM:
                currentStep = SessionStep.COMPLETED;
                break;
        }
    }

    public String getViolatorName() {
        return violatorName;
    }

    public void setViolatorName(String violatorName) {
        this.violatorName = violatorName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(int timeoutSeconds) {
        if (timeoutSeconds <= 0) return false;
        long elapsed = (System.currentTimeMillis() - createdAt) / 1000;
        return elapsed > timeoutSeconds;
    }

    public boolean isCompleted() {
        return currentStep == SessionStep.COMPLETED;
    }

    public enum SessionStep {
        VIOLATOR_NAME,
        REASON,
        COMMENT,
        CONFIRM,
        COMPLETED
    }
}