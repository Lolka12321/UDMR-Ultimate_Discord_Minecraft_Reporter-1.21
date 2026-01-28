package com.reportsystem.managers;

import com.reportsystem.LocaleManager;
import com.reportsystem.ReportSystem;
import com.reportsystem.models.Report;
import com.reportsystem.models.ReportSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {

    private final ReportSystem plugin;
    private final Map<UUID, ReportSession> activeSessions;
    private int reportCounter;

    public ReportManager(ReportSystem plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.reportCounter = 0;
    }

    public boolean hasActiveSession(UUID playerUUID) {
        return activeSessions.containsKey(playerUUID);
    }

    public ReportSession getSession(UUID playerUUID) {
        return activeSessions.get(playerUUID);
    }

    public void startSession(Player player) {
        // Проверяем доступность Discord бота
        if (plugin.getDiscordBot() == null || !plugin.getDiscordBot().isConnected()) {
            player.sendMessage(plugin.getLocaleManager().getMessage("report-system-unavailable"));
            return;
        }

        ReportSession session = new ReportSession(player);
        activeSessions.put(player.getUniqueId(), session);

        // Отправляем первый вопрос
        player.sendMessage(plugin.getLocaleManager().getMessage("report-start"));
        player.sendMessage(plugin.getLocaleManager().getMessageWithoutPrefix("question-nickname"));
    }

    public void cancelSession(UUID playerUUID) {
        activeSessions.remove(playerUUID);
    }

    public void handleSessionInput(Player player, String input) {
        UUID uuid = player.getUniqueId();
        ReportSession session = activeSessions.get(uuid);

        if (session == null) {
            return;
        }

        // Проверка на отмену
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("отмена")) {
            cancelSession(uuid);
            player.sendMessage(plugin.getLocaleManager().getMessage("report-cancelled"));
            return;
        }

        // Проверка таймаута
        int timeout = plugin.getConfig().getInt("reports.form-timeout", 300);
        if (session.isExpired(timeout)) {
            cancelSession(uuid);
            player.sendMessage(plugin.getLocaleManager().getMessage("report-timeout"));
            return;
        }

        switch (session.getCurrentStep()) {
            case VIOLATOR_NAME:
                handleViolatorName(player, session, input);
                break;
            case REASON:
                handleReason(player, session, input);
                break;
            case COMMENT:
                handleComment(player, session, input);
                break;
            case CONFIRM:
                // Игнорируем текстовый ввод на этапе подтверждения
                // Используются только кнопки
                break;
        }
    }

    private void handleViolatorName(Player player, ReportSession session, String input) {
        // Проверка на самого себя
        if (input.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getLocaleManager().getMessage("cannot-report-self"));
            return;
        }

        // Проверка на существование игрока на сервере
        if (!isValidPlayerName(input)) {
            player.sendMessage(plugin.getLocaleManager().getMessage("player-not-found"));
            return;
        }

        session.setViolatorName(input);
        session.nextStep();
        player.sendMessage(plugin.getLocaleManager().getMessageWithoutPrefix("question-reason"));
    }

    private boolean isValidPlayerName(String playerName) {
        if (playerName.length() < 3 || playerName.length() > 16) {
            return false;
        }

        if (!playerName.matches("^[a-zA-Z0-9_]+$")) {
            return false;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        return offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline();
    }

    @SuppressWarnings("deprecation")
    private UUID getViolatorUUID(String playerName) {
        if (!Bukkit.getOnlineMode()) {
            return null;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            return offlinePlayer.getUniqueId();
        }

        return null;
    }

    private void handleReason(Player player, ReportSession session, String input) {
        session.setReason(input);
        session.nextStep();
        player.sendMessage(plugin.getLocaleManager().getMessageWithoutPrefix("question-comment"));
    }

    private void handleComment(Player player, ReportSession session, String input) {
        session.setComment(input);
        session.nextStep();

        // Показываем интерактивное подтверждение
        showConfirmation(player, session);
    }

    private void showConfirmation(Player player, ReportSession session) {
        LocaleManager locale = plugin.getLocaleManager();

        // Верхний разделитель
        player.sendMessage(locale.getMessageWithoutPrefix("confirm-header"));
        // Данные репорта
        player.sendMessage(locale.getMessageRaw("confirm-violator", "violator", session.getViolatorName()));
        player.sendMessage(locale.getMessageRaw("confirm-reason", "reason", session.getReason()));
        player.sendMessage(locale.getMessageRaw("confirm-comment", "comment", session.getComment()));
        // Нижний разделитель
        player.sendMessage(locale.getMessageWithoutPrefix("confirm-footer"));
        // Заголовок вопроса
        player.sendMessage(locale.getMessageWithoutPrefix("confirm-title"));

        // Создаём интерактивные кнопки
        Component yesButton = locale.getMessageWithoutPrefix("confirm-button-yes")
                .hoverEvent(HoverEvent.showText(Component.text("Click to confirm")))
                .clickEvent(ClickEvent.runCommand("/report confirm yes"));

        Component noButton = locale.getMessageWithoutPrefix("confirm-button-no")
                .hoverEvent(HoverEvent.showText(Component.text("Click to cancel")))
                .clickEvent(ClickEvent.runCommand("/report confirm no"));

        Component buttons = yesButton.append(Component.text("  ")).append(noButton);
        player.sendMessage(buttons);
    }

    public void handleConfirmation(Player player, boolean confirmed) {
        UUID uuid = player.getUniqueId();
        ReportSession session = activeSessions.get(uuid);

        if (session == null || session.getCurrentStep() != ReportSession.SessionStep.CONFIRM) {
            return;
        }

        if (confirmed) {
            // Проверяем доступность Discord бота
            if (plugin.getDiscordBot() == null || !plugin.getDiscordBot().isConnected()) {
                player.sendMessage(plugin.getLocaleManager().getMessage("report-system-unavailable"));
                cancelSession(uuid);
                return;
            }

            // Создаём жалобу
            String reportId = generateReportId();
            UUID violatorUUID = getViolatorUUID(session.getViolatorName());

            Report report = new Report(
                    reportId,
                    player.getUniqueId(),
                    player.getName(),
                    session.getViolatorName(),
                    violatorUUID,
                    session.getReason(),
                    session.getComment()
            );

            // Сохраняем
            plugin.getDataManager().saveReport(report);

            // Отправляем в Discord через бота
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getDiscordBot().sendReport(report);
                } catch (Exception e) {
                    plugin.getLogger().severe("Ошибка отправки жалобы #" + reportId + " в Discord: " + e.getMessage());
                }
            });

            // Уведомляем игрока
            player.sendMessage(plugin.getLocaleManager().getMessage("report-success", "id", reportId));

            // Удаляем сессию
            cancelSession(uuid);
        } else {
            cancelSession(uuid);
            player.sendMessage(plugin.getLocaleManager().getMessage("report-cancelled"));
        }
    }

    public void showAlreadyCreatingPrompt(Player player) {
        LocaleManager locale = plugin.getLocaleManager();

        // Показываем сообщение
        player.sendMessage(locale.getMessageWithoutPrefix("already-creating-header"));
        player.sendMessage(locale.getMessageWithoutPrefix("already-creating-title"));
        player.sendMessage(locale.getMessageWithoutPrefix("already-creating-prompt"));
        player.sendMessage(Component.empty());

        // Создаём интерактивные кнопки
        Component yesButton = locale.getMessageWithoutPrefix("already-creating-button-yes")
                .hoverEvent(HoverEvent.showText(Component.text("Click to cancel current report")))
                .clickEvent(ClickEvent.runCommand("/report cancel yes"));

        Component noButton = locale.getMessageWithoutPrefix("already-creating-button-no")
                .hoverEvent(HoverEvent.showText(Component.text("Click to continue")))
                .clickEvent(ClickEvent.runCommand("/report cancel no"));

        Component buttons = yesButton.append(Component.text("  ")).append(noButton);
        player.sendMessage(buttons);
        player.sendMessage(locale.getMessageWithoutPrefix("already-creating-footer"));
    }

    public void handleCancelPrompt(Player player, boolean shouldCancel) {
        if (shouldCancel) {
            cancelSession(player.getUniqueId());
            player.sendMessage(plugin.getLocaleManager().getMessage("report-cancelled"));
        }
        // Если нет - просто ничего не делаем, продолжаем создание
    }

    private String generateReportId() {
        reportCounter++;
        return String.format("REP-%d-%d", System.currentTimeMillis() / 1000, reportCounter);
    }

    public boolean canCreateReport(Player player) {
        UUID uuid = player.getUniqueId();
        int activeCount = plugin.getDataManager().getActiveReportsCount(uuid);
        int maxReports = plugin.getConfig().getInt("reports.max-active-reports", 5);

        return activeCount < maxReports;
    }
}