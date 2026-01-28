package com.reportsystem.discord;

import com.reportsystem.LocaleManager;
import com.reportsystem.ReportSystem;
import com.reportsystem.models.Report;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class DiscordButtonListener extends ListenerAdapter {

    private final ReportSystem plugin;
    private final DiscordBot discordBot;

    public DiscordButtonListener(ReportSystem plugin, DiscordBot discordBot) {
        this.plugin = plugin;
        this.discordBot = discordBot;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        String username = event.getUser().getName();
        String userId = event.getUser().getId();

        if (buttonId.startsWith("approve_")) {
            handleApprove(event, buttonId, username, userId);
        } else if (buttonId.startsWith("reject_")) {
            handleReject(event, buttonId, username, userId);
        } else if (buttonId.startsWith("check_")) {
            handleCheck(event, buttonId, username, userId);
        } else if (buttonId.startsWith("comment_")) {
            handleComment(event, buttonId);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (modalId.startsWith("comment_modal_")) {
            handleCommentModal(event, modalId);
        } else if (modalId.startsWith("punishment_modal_")) {
            handlePunishmentModal(event, modalId);
        }
    }

    private void handleApprove(ButtonInteractionEvent event, String buttonId, String username, String userId) {
        String reportId = buttonId.replace("approve_", "");

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LocaleManager locale = plugin.getLocaleManager();
            Report report = plugin.getDataManager().getReport(reportId);

            if (report == null) {
                event.reply(locale.getRaw("discord.report-not-found")).setEphemeral(true).queue();
                return;
            }

            if (report.getStatus() != Report.ReportStatus.PENDING) {
                event.reply(locale.getRaw("discord.already-reviewed")).setEphemeral(true).queue();
                return;
            }

            // Создаем модальную форму для наказания
            TextInput punishmentType = TextInput.create("punishment_type", "Punishment Type", TextInputStyle.SHORT)
                    .setPlaceholder("ban / mute / kick / warn")
                    .setRequired(true)
                    .setMinLength(3)
                    .setMaxLength(10)
                    .build();

            TextInput duration = TextInput.create("duration", "Duration", TextInputStyle.SHORT)
                    .setPlaceholder("Examples: 30m, 7d, 1y, permanent")
                    .setRequired(false)
                    .setMaxLength(20)
                    .build();

            TextInput reason = TextInput.create("reason", "Reason (optional)", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Additional punishment reason...")
                    .setRequired(false)
                    .setMaxLength(500)
                    .build();

            Modal modal = Modal.create("punishment_modal_" + reportId, "Approve Report #" + reportId)
                    .addActionRow(punishmentType)
                    .addActionRow(duration)
                    .addActionRow(reason)
                    .build();

            event.replyModal(modal).queue();
        });
    }

    private void handlePunishmentModal(ModalInteractionEvent event, String modalId) {
        String reportId = modalId.replace("punishment_modal_", "");
        String punishmentType = event.getValue("punishment_type").getAsString().toLowerCase();
        String duration = event.getValue("duration") != null ? event.getValue("duration").getAsString() : "permanent";
        String reason = event.getValue("reason") != null ? event.getValue("reason").getAsString() : "";

        String username = event.getUser().getName();
        String userId = event.getUser().getId();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LocaleManager locale = plugin.getLocaleManager();
            Report report = plugin.getDataManager().getReport(reportId);

            if (report == null) {
                event.reply(locale.getRaw("discord.report-not-found")).setEphemeral(true).queue();
                return;
            }

            // Обновляем статус репорта
            report.setStatus(Report.ReportStatus.APPROVED);
            report.setReviewedBy(username);
            report.setReviewedById(userId);
            report.setReviewedAt(new Date());

            // Сохраняем информацию о наказании
            String punishmentInfo = String.format("Punishment: %s | Duration: %s", punishmentType, duration);
            if (!reason.isEmpty()) {
                punishmentInfo += " | Reason: " + reason;
            }
            report.setAdminComment(punishmentInfo);

            plugin.getDataManager().saveReport(report);

            // Выполняем наказание
            executePunishment(report.getViolatorName(), punishmentType, duration, report.getReason() + (reason.isEmpty() ? "" : " | " + reason));

            // Обновляем Discord сообщение
            discordBot.updateReportStatus(report);

            // Уведомляем игрока
            notifyPlayerInGame(report, locale.getRaw("status-approved"));

            event.reply("✓ Report #" + reportId + " approved! Punishment: " + punishmentType + " (" + duration + ")")
                    .setEphemeral(true).queue();
        });
    }

    private void executePunishment(String playerName, String type, String duration, String reason) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String command = "";

            switch (type.toLowerCase()) {
                case "ban":
                    if (duration.equalsIgnoreCase("permanent")) {
                        command = String.format("ban %s %s", playerName, reason);
                    } else {
                        command = String.format("tempban %s %s %s", playerName, duration, reason);
                    }
                    break;
                case "mute":
                    if (duration.equalsIgnoreCase("permanent")) {
                        command = String.format("mute %s %s", playerName, reason);
                    } else {
                        command = String.format("tempmute %s %s %s", playerName, duration, reason);
                    }
                    break;
                case "kick":
                    command = String.format("kick %s %s", playerName, reason);
                    break;
                case "warn":
                    command = String.format("warn %s %s", playerName, reason);
                    break;
            }

            if (!command.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info("Executed punishment: " + command);
            }
        });
    }

    private void handleReject(ButtonInteractionEvent event, String buttonId, String username, String userId) {
        String reportId = buttonId.replace("reject_", "");

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LocaleManager locale = plugin.getLocaleManager();
            Report report = plugin.getDataManager().getReport(reportId);

            if (report == null) {
                event.reply(locale.getRaw("discord.report-not-found")).setEphemeral(true).queue();
                return;
            }

            if (report.getStatus() != Report.ReportStatus.PENDING) {
                event.reply(locale.getRaw("discord.already-reviewed")).setEphemeral(true).queue();
                return;
            }

            report.setStatus(Report.ReportStatus.REJECTED);
            report.setReviewedBy(username);
            report.setReviewedById(userId);
            report.setReviewedAt(new Date());
            plugin.getDataManager().saveReport(report);

            discordBot.updateReportStatus(report);
            notifyPlayerInGame(report, locale.getRaw("status-rejected"));

            event.reply(locale.getRaw("discord.rejected-success").replace("{id}", reportId))
                    .setEphemeral(true).queue();
        });
    }

    private void handleCheck(ButtonInteractionEvent event, String buttonId, String username, String userId) {
        String reportId = buttonId.replace("check_", "");

        // Проверяем, находится ли админ в голосовом канале ДО перехода в синхронный контекст
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
            event.reply("❌ You must be in a voice channel to call a player for check!")
                    .setEphemeral(true).queue();
            return;
        }

        AudioChannelUnion voiceChannel = member.getVoiceState().getChannel();
        String channelName = voiceChannel.getName();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LocaleManager locale = plugin.getLocaleManager();
            Report report = plugin.getDataManager().getReport(reportId);

            if (report == null) {
                event.reply(locale.getRaw("discord.report-not-found")).setEphemeral(true).queue();
                return;
            }

            // Находим игрока на сервере
            Player violator = Bukkit.getPlayer(report.getViolatorName());
            if (violator == null || !violator.isOnline()) {
                event.reply("❌ Player " + report.getViolatorName() + " is not online!")
                        .setEphemeral(true).queue();
                return;
            }

            // Замораживаем игрока
            freezePlayer(violator, true);

            // Отправляем сообщение игроку
            boolean isRussian = locale.getCurrentLocale().equals("ru-RU");
            String message = isRussian
                    ? String.format("§c§l[ПРОВЕРКА] §fВы вызваны на проверку администратором!\n§eЗайдите в голосовой канал Discord: §a%s", channelName)
                    : String.format("§c§l[CHECK] §fYou have been called for a check by an administrator!\n§eJoin the Discord voice channel: §a%s", channelName);

            violator.sendMessage(message);
            violator.sendTitle(
                    isRussian ? "§c§lПРОВЕРКА" : "§c§lCHECK",
                    isRussian ? "§eЗайдите в Discord" : "§eJoin Discord",
                    10, 100, 20
            );

            // Добавляем комментарий к репорту
            String checkComment = String.format("Called for check by %s. Voice channel: %s", username, channelName);
            report.setAdminComment(checkComment);
            plugin.getDataManager().saveReport(report);

            discordBot.updateReportComment(report);

            event.reply(String.format("✅ Player %s has been frozen and notified to join voice channel: %s",
                            report.getViolatorName(), channelName))
                    .setEphemeral(true).queue();
        });
    }

    private void freezePlayer(Player player, boolean freeze) {
        if (freeze) {
            // Замораживаем игрока
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
            player.setAllowFlight(false);
            player.setInvulnerable(true);
        } else {
            // Размораживаем игрока
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.setInvulnerable(false);
        }
    }

    private void handleComment(ButtonInteractionEvent event, String buttonId) {
        String reportId = buttonId.replace("comment_", "");
        LocaleManager locale = plugin.getLocaleManager();

        TextInput commentInput = TextInput.create("comment_input",
                        locale.getRaw("discord.modal-input-label"),
                        TextInputStyle.PARAGRAPH)
                .setPlaceholder(locale.getRaw("discord.modal-input-placeholder"))
                .setMinLength(1)
                .setMaxLength(500)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("comment_modal_" + reportId,
                        locale.getRaw("discord.modal-title").replace("{id}", reportId))
                .addActionRow(commentInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void handleCommentModal(ModalInteractionEvent event, String modalId) {
        String reportId = modalId.replace("comment_modal_", "");
        String comment = event.getValue("comment_input").getAsString();
        String username = event.getUser().getName();
        String userId = event.getUser().getId();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LocaleManager locale = plugin.getLocaleManager();
            Report report = plugin.getDataManager().getReport(reportId);

            if (report == null) {
                event.reply(locale.getRaw("discord.report-not-found")).setEphemeral(true).queue();
                return;
            }

            report.setAdminComment(comment);
            report.setReviewedBy(username);
            report.setReviewedById(userId);
            report.setReviewedAt(new Date());
            plugin.getDataManager().saveReport(report);

            discordBot.updateReportComment(report);
            notifyPlayerInGame(report, locale.getRaw("status-pending"));

            event.reply(locale.getRaw("discord.comment-added").replace("{id}", reportId))
                    .setEphemeral(true).queue();
        });
    }

    private void notifyPlayerInGame(Report report, String action) {
        UUID playerUUID = report.getReporterUUID();
        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getLocaleManager().getMessage("report-reviewed",
                    "id", report.getId(),
                    "action", action));
        }
    }
}