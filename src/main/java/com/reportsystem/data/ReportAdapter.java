package com.reportsystem.data;

import com.reportsystem.models.Report;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.UUID;

public class ReportAdapter implements JsonSerializer<Report>, JsonDeserializer<Report> {

    @Override
    public JsonElement serialize(Report report, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();

        json.addProperty("id", report.getId());
        json.addProperty("reporterUUID", report.getReporterUUID().toString());
        json.addProperty("reporterName", report.getReporterName());
        json.addProperty("violatorName", report.getViolatorName());

        // Сохраняем UUID нарушителя если он есть
        if (report.getViolatorUUID() != null) {
            json.addProperty("violatorUUID", report.getViolatorUUID().toString());
        }

        json.addProperty("reason", report.getReason());
        json.addProperty("comment", report.getComment());
        json.addProperty("createdAt", report.getCreatedAt().getTime());
        json.addProperty("status", report.getStatus().getKey());

        if (report.getAdminComment() != null) {
            json.addProperty("adminComment", report.getAdminComment());
        }
        if (report.getReviewedBy() != null) {
            json.addProperty("reviewedBy", report.getReviewedBy());
        }
        if (report.getReviewedById() != null) {
            json.addProperty("reviewedById", report.getReviewedById());
        }
        if (report.getReviewedAt() != null) {
            json.addProperty("reviewedAt", report.getReviewedAt().getTime());
        }

        return json;
    }

    @Override
    public Report deserialize(JsonElement element, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject json = element.getAsJsonObject();

        String id = json.get("id").getAsString();
        UUID reporterUUID = UUID.fromString(json.get("reporterUUID").getAsString());
        String reporterName = json.get("reporterName").getAsString();
        String violatorName = json.get("violatorName").getAsString();

        // Загружаем UUID нарушителя если он есть
        UUID violatorUUID = null;
        if (json.has("violatorUUID") && !json.get("violatorUUID").isJsonNull()) {
            try {
                violatorUUID = UUID.fromString(json.get("violatorUUID").getAsString());
            } catch (Exception e) {
                // Игнорируем ошибки парсинга UUID
            }
        }

        String reason = json.get("reason").getAsString();
        String comment = json.get("comment").getAsString();

        Report report = new Report(id, reporterUUID, reporterName, violatorName, violatorUUID, reason, comment);

        // Восстанавливаем дату создания
        if (json.has("createdAt")) {
            long timestamp = json.get("createdAt").getAsLong();
            // Используем reflection для установки final поля
            try {
                java.lang.reflect.Field field = Report.class.getDeclaredField("createdAt");
                field.setAccessible(true);
                field.set(report, new Date(timestamp));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Восстанавливаем статус
        if (json.has("status")) {
            report.setStatus(Report.ReportStatus.fromKey(json.get("status").getAsString()));
        }

        // Восстанавливаем комментарий администратора
        if (json.has("adminComment")) {
            report.setAdminComment(json.get("adminComment").getAsString());
        }

        // Восстанавливаем информацию о проверке
        if (json.has("reviewedBy")) {
            report.setReviewedBy(json.get("reviewedBy").getAsString());
        }
        if (json.has("reviewedById")) {
            report.setReviewedById(json.get("reviewedById").getAsString());
        }
        if (json.has("reviewedAt")) {
            report.setReviewedAt(new Date(json.get("reviewedAt").getAsLong()));
        }

        return report;
    }
}