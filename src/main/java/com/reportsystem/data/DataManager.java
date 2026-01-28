package com.reportsystem.data;

import com.reportsystem.ReportSystem;
import com.reportsystem.models.Report;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final ReportSystem plugin;
    private final Gson gson;
    private final File dataFile;
    private final Map<String, Report> reports;
    private final Map<UUID, List<String>> playerReports; // UUID -> List of Report IDs

    public DataManager(ReportSystem plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Report.class, new ReportAdapter())
                .create();
        this.dataFile = new File(plugin.getDataFolder(), "reports.json");
        this.reports = new ConcurrentHashMap<>();
        this.playerReports = new ConcurrentHashMap<>();

        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("Файл данных не найден, создаём новый...");
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Report>>(){}.getType();
            Map<String, Report> loaded = gson.fromJson(reader, type);

            if (loaded != null) {
                reports.putAll(loaded);

                // Индексируем жалобы по игрокам
                for (Report report : loaded.values()) {
                    playerReports.computeIfAbsent(report.getReporterUUID(), k -> new ArrayList<>())
                            .add(report.getId());
                }

                plugin.getLogger().info("Загружено жалоб: " + reports.size());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка загрузки данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        try {
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }

            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(reports, writer);
                plugin.getLogger().info("Данные сохранены: " + reports.size() + " жалоб");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveReport(Report report) {
        String reportId = report.getId();
        UUID playerUUID = report.getReporterUUID();

        // Сохраняем репорт
        reports.put(reportId, report);

        // ИСПРАВЛЕНО: Добавляем в индекс только если его там еще нет
        List<String> playerReportIds = playerReports.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        if (!playerReportIds.contains(reportId)) {
            playerReportIds.add(reportId);
        }

        saveAllData();
    }

    public Report getReport(String id) {
        return reports.get(id);
    }

    public List<Report> getPlayerReports(UUID playerUUID) {
        List<String> reportIds = playerReports.getOrDefault(playerUUID, new ArrayList<>());
        List<Report> result = new ArrayList<>();

        for (String id : reportIds) {
            Report report = reports.get(id);
            if (report != null) {
                result.add(report);
            }
        }

        // Сортируем по дате создания (новые первые)
        result.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));

        return result;
    }

    public List<Report> getAllReports() {
        return new ArrayList<>(reports.values());
    }

    public int getActiveReportsCount(UUID playerUUID) {
        return (int) getPlayerReports(playerUUID).stream()
                .filter(r -> r.getStatus() == Report.ReportStatus.PENDING)
                .count();
    }

    public Map<Report.ReportStatus, Integer> getStatusStatistics() {
        Map<Report.ReportStatus, Integer> stats = new HashMap<>();
        for (Report.ReportStatus status : Report.ReportStatus.values()) {
            stats.put(status, 0);
        }

        for (Report report : reports.values()) {
            stats.merge(report.getStatus(), 1, Integer::sum);
        }

        return stats;
    }

    public Map<Report.ReportStatus, Integer> getPlayerStatistics(UUID playerUUID) {
        Map<Report.ReportStatus, Integer> stats = new HashMap<>();
        for (Report.ReportStatus status : Report.ReportStatus.values()) {
            stats.put(status, 0);
        }

        for (Report report : getPlayerReports(playerUUID)) {
            stats.merge(report.getStatus(), 1, Integer::sum);
        }

        return stats;
    }
}