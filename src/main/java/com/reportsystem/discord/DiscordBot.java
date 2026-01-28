package com.reportsystem.discord;

import com.reportsystem.LocaleManager;
import com.reportsystem.ReportSystem;
import com.reportsystem.models.Report;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordBot {

    private final ReportSystem plugin;
    private JDA jda;
    private TextChannel reportChannel;
    private final Map<String, String> reportMessages; // reportId -> messageId

    public DiscordBot(ReportSystem plugin) {
        this.plugin = plugin;
        this.reportMessages = new ConcurrentHashMap<>();
    }

    public void start() {
        String token = plugin.getConfig().getString("discord.bot-token", "");
        String channelId = plugin.getConfig().getString("discord.channel-id", "");

        if (token == null || token.isEmpty() || token.contains("YOUR_BOT_TOKEN")) {
            plugin.getLogger().severe("Discord bot token is not configured!");
            return;
        }

        if (channelId == null || channelId.isEmpty() || channelId.contains("YOUR_CHANNEL_ID")) {
            plugin.getLogger().severe("Discord channel ID is not configured!");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
                    .disableCache(CacheFlag.EMOJI, CacheFlag.STICKER)
                    .setActivity(Activity.watching("reports"))
                    .addEventListeners(new DiscordButtonListener(plugin, this))
                    .build();

            jda.awaitReady();

            reportChannel = jda.getTextChannelById(channelId);
            if (reportChannel == null) {
                plugin.getLogger().severe("Cannot find Discord channel with ID: " + channelId);
                jda = null;
                return;
            }

            plugin.getLogger().info("Discord bot connected successfully!");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start Discord bot: " + e.getMessage());
            jda = null;
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info("Discord бот остановлен!");
        }
    }

    public void sendReport(Report report) {
        if (reportChannel == null) {
            plugin.getLogger().warning("Канал для отправки репортов не настроен!");
            return;
        }

        LocaleManager locale = plugin.getLocaleManager();
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(locale.getRaw("discord.new-report-title") + " #" + report.getId());
        embed.setColor(Color.ORANGE);

        StringBuilder description = new StringBuilder();
        description.append("**").append(locale.getRaw("discord.reporter")).append(":** ")
                .append(report.getReporterName()).append("\n\n");

        description.append("**").append(locale.getRaw("discord.violator")).append(":** ")
                .append(report.getViolatorName()).append("\n");

        if (report.getViolatorUUID() != null) {
            description.append("**").append(locale.getRaw("discord.violator-uuid")).append(":** `")
                    .append(report.getViolatorUUID().toString()).append("`\n");
        }

        description.append("**").append(locale.getRaw("discord.reason")).append(":** ")
                .append(report.getReason()).append("\n");
        description.append("**").append(locale.getRaw("discord.comment")).append(":** ")
                .append(report.getComment()).append("\n");
        description.append("\n**").append(locale.getRaw("discord.status")).append(":** ")
                .append(locale.getRaw("discord.status-pending"));

        embed.setDescription(description.toString());

        SimpleDateFormat sdf = new SimpleDateFormat(locale.getRaw("date-format"));
        embed.setFooter(locale.getRaw("discord.created") + ": " + sdf.format(report.getCreatedAt()));
        embed.setTimestamp(report.getCreatedAt().toInstant());

        reportChannel.sendMessageEmbeds(embed.build())
                .setActionRow(
                        Button.success("approve_" + report.getId(), locale.getRaw("discord.button-approve")),
                        Button.danger("reject_" + report.getId(), locale.getRaw("discord.button-reject")),
                        Button.secondary("check_" + report.getId(), locale.getRaw("discord.button-check")),
                        Button.primary("comment_" + report.getId(), locale.getRaw("discord.button-comment"))
                )
                .queue(message -> {
                    reportMessages.put(report.getId(), message.getId());
                    plugin.getLogger().info("Жалоба #" + report.getId() + " отправлена в Discord");
                }, error -> {
                    plugin.getLogger().severe("Ошибка отправки жалобы в Discord: " + error.getMessage());
                });
    }

    public void updateReportComment(Report report) {
        String messageId = reportMessages.get(report.getId());
        if (messageId == null || reportChannel == null) {
            return;
        }

        reportChannel.retrieveMessageById(messageId).queue(message -> {
            LocaleManager locale = plugin.getLocaleManager();
            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle(locale.getRaw("discord.new-report-title") + " #" + report.getId());
            embed.setColor(Color.ORANGE);

            StringBuilder description = new StringBuilder();
            description.append("**").append(locale.getRaw("discord.reporter")).append(":** ")
                    .append(report.getReporterName()).append("\n\n");

            description.append("**").append(locale.getRaw("discord.violator")).append(":** ")
                    .append(report.getViolatorName()).append("\n");

            if (report.getViolatorUUID() != null) {
                description.append("**").append(locale.getRaw("discord.violator-uuid")).append(":** `")
                        .append(report.getViolatorUUID().toString()).append("`\n");
            }

            description.append("**").append(locale.getRaw("discord.reason")).append(":** ")
                    .append(report.getReason()).append("\n");
            description.append("**").append(locale.getRaw("discord.comment")).append(":** ")
                    .append(report.getComment()).append("\n");
            description.append("\n**").append(locale.getRaw("discord.status")).append(":** ")
                    .append(locale.getRaw("discord.status-pending"));

            if (report.getAdminComment() != null) {
                description.append("\n\n**💬 ").append(locale.getRaw("discord.admin-comment"))
                        .append(":** ").append(report.getAdminComment());
            }

            if (report.getReviewedById() != null) {
                description.append("\n**").append(locale.getRaw("discord.comment-left-by"))
                        .append(":** <@").append(report.getReviewedById()).append(">");
            } else if (report.getReviewedBy() != null) {
                description.append("\n**").append(locale.getRaw("discord.comment-left-by"))
                        .append(":** ").append(report.getReviewedBy());
            }

            embed.setDescription(description.toString());

            SimpleDateFormat sdf = new SimpleDateFormat(locale.getRaw("date-format"));
            embed.setFooter(locale.getRaw("discord.created") + ": " + sdf.format(report.getCreatedAt()));
            embed.setTimestamp(report.getCreatedAt().toInstant());

            message.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.success("approve_" + report.getId(), locale.getRaw("discord.button-approve")),
                            Button.danger("reject_" + report.getId(), locale.getRaw("discord.button-reject")),
                            Button.secondary("check_" + report.getId(), locale.getRaw("discord.button-check")),
                            Button.primary("comment_" + report.getId(), locale.getRaw("discord.button-comment"))
                    )
                    .queue(
                            success -> plugin.getLogger().info("Комментарий к жалобе #" + report.getId() + " обновлён в Discord"),
                            error -> plugin.getLogger().warning("Ошибка обновления жалобы в Discord: " + error.getMessage())
                    );
        }, error -> {
            plugin.getLogger().warning("Не удалось найти сообщение для жалобы #" + report.getId());
        });
    }

    public void updateReportStatus(Report report) {
        String messageId = reportMessages.get(report.getId());
        if (messageId == null || reportChannel == null) {
            return;
        }

        reportChannel.retrieveMessageById(messageId).queue(message -> {
            LocaleManager locale = plugin.getLocaleManager();
            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle(locale.getRaw("discord.new-report-title") + " #" + report.getId());

            Color color;
            String statusText;

            switch (report.getStatus()) {
                case APPROVED:
                    color = Color.GREEN;
                    statusText = locale.getRaw("discord.status-approved");
                    break;
                case REJECTED:
                    color = Color.RED;
                    statusText = locale.getRaw("discord.status-rejected");
                    break;
                default:
                    color = Color.ORANGE;
                    statusText = locale.getRaw("discord.status-pending");
                    break;
            }

            embed.setColor(color);

            StringBuilder description = new StringBuilder();
            description.append("**").append(locale.getRaw("discord.reporter")).append(":** ")
                    .append(report.getReporterName()).append("\n\n");

            description.append("**").append(locale.getRaw("discord.violator")).append(":** ")
                    .append(report.getViolatorName()).append("\n");

            if (report.getViolatorUUID() != null) {
                description.append("**").append(locale.getRaw("discord.violator-uuid")).append(":** `")
                        .append(report.getViolatorUUID().toString()).append("`\n");
            }

            description.append("**").append(locale.getRaw("discord.reason")).append(":** ")
                    .append(report.getReason()).append("\n");
            description.append("**").append(locale.getRaw("discord.comment")).append(":** ")
                    .append(report.getComment()).append("\n");
            description.append("\n**").append(locale.getRaw("discord.status")).append(":** ")
                    .append(statusText);

            if (report.getReviewedById() != null) {
                description.append("\n**").append(locale.getRaw("discord.reviewed-by"))
                        .append(":** <@").append(report.getReviewedById()).append(">");
            } else if (report.getReviewedBy() != null) {
                description.append("\n**").append(locale.getRaw("discord.reviewed-by"))
                        .append(":** ").append(report.getReviewedBy());
            }

            if (report.getAdminComment() != null) {
                description.append("\n**").append(locale.getRaw("discord.admin-comment"))
                        .append(":** ").append(report.getAdminComment());
            }

            embed.setDescription(description.toString());

            SimpleDateFormat sdf = new SimpleDateFormat(locale.getRaw("date-format"));
            embed.setFooter(locale.getRaw("discord.created") + ": " + sdf.format(report.getCreatedAt()));
            embed.setTimestamp(report.getCreatedAt().toInstant());

            message.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.success("approve_" + report.getId(), locale.getRaw("discord.button-approve")).asDisabled(),
                            Button.danger("reject_" + report.getId(), locale.getRaw("discord.button-reject")).asDisabled(),
                            Button.secondary("check_" + report.getId(), locale.getRaw("discord.button-check")).asDisabled(),
                            Button.primary("comment_" + report.getId(), locale.getRaw("discord.button-comment")).asDisabled()
                    )
                    .queue(
                            success -> plugin.getLogger().info("Статус жалобы #" + report.getId() + " обновлён в Discord"),
                            error -> plugin.getLogger().warning("Ошибка обновления жалобы в Discord: " + error.getMessage())
                    );
        }, error -> {
            plugin.getLogger().warning("Не удалось найти сообщение для жалобы #" + report.getId());
        });
    }

    public boolean isConnected() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED && reportChannel != null;
    }

    public JDA getJDA() {
        return jda;
    }
}