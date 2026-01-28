package com.reportsystem.gui;

import com.reportsystem.LocaleManager;
import com.reportsystem.ReportSystem;
import com.reportsystem.models.Report;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ReportsGUI {

    private final ReportSystem plugin;
    private static final int REPORTS_PER_PAGE = 18;

    public ReportsGUI(ReportSystem plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, int page) {
        List<Report> reports = plugin.getDataManager().getPlayerReports(player.getUniqueId());
        LocaleManager locale = plugin.getLocaleManager();

        if (reports.isEmpty()) {
            player.sendMessage(locale.getMessage("no-reports"));
            return;
        }

        int maxPage = (int) Math.ceil((double) reports.size() / REPORTS_PER_PAGE);
        page = Math.max(1, Math.min(page, maxPage));

        // Получаем локализованный заголовок
        String titleKey = locale.getCurrentLocale().equals("ru-RU") ?
                "Ваши жалобы (стр. " + page + "/" + maxPage + ")" :
                "Your Reports (page " + page + "/" + maxPage + ")";

        Inventory gui = Bukkit.createInventory(null, 36,
                Component.text(titleKey)
                        .color(NamedTextColor.DARK_GRAY)
                        .decorate(TextDecoration.BOLD));

        // Добавляем репорты
        int startIndex = (page - 1) * REPORTS_PER_PAGE;
        int endIndex = Math.min(startIndex + REPORTS_PER_PAGE, reports.size());

        for (int i = startIndex; i < endIndex; i++) {
            Report report = reports.get(i);
            ItemStack item = createReportItem(report);
            gui.addItem(item);
        }

        addNavigationButtons(gui, page, maxPage, player);
        player.openInventory(gui);
    }

    private ItemStack createReportItem(Report report) {
        LocaleManager locale = plugin.getLocaleManager();
        boolean isRussian = locale.getCurrentLocale().equals("ru-RU");

        Material material;
        NamedTextColor color;

        switch (report.getStatus()) {
            case APPROVED:
                material = Material.LIME_WOOL;
                color = NamedTextColor.GREEN;
                break;
            case REJECTED:
                material = Material.RED_WOOL;
                color = NamedTextColor.RED;
                break;
            default:
                material = Material.YELLOW_WOOL;
                color = NamedTextColor.YELLOW;
                break;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Название
        String reportTitle = isRussian ? "Жалоба #" : "Report #";
        meta.displayName(Component.text(reportTitle + report.getId())
                .color(color)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));

        // Описание
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String violatorLabel = isRussian ? "Нарушитель: " : "Violator: ";
        lore.add(Component.text(violatorLabel)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(report.getViolatorName())
                        .color(NamedTextColor.WHITE)));
        lore.add(Component.empty());

        String reasonLabel = isRussian ? "Причина: " : "Reason: ";
        lore.add(Component.text(reasonLabel)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(report.getReason())
                        .color(NamedTextColor.WHITE)));
        lore.add(Component.empty());

        String commentLabel = isRussian ? "Комментарий:" : "Comment:";
        lore.add(Component.text(commentLabel)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(report.getComment())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Статус
        Component statusText;
        switch (report.getStatus()) {
            case APPROVED:
                String approvedText = isRussian ? "✓ Одобрено" : "✓ Approved";
                statusText = Component.text(approvedText).color(NamedTextColor.GREEN);
                break;
            case REJECTED:
                String rejectedText = isRussian ? "✗ Отклонено" : "✗ Rejected";
                statusText = Component.text(rejectedText).color(NamedTextColor.RED);
                break;
            default:
                String pendingText = isRussian ? "⏳ На рассмотрении" : "⏳ Pending";
                statusText = Component.text(pendingText).color(NamedTextColor.YELLOW);
                break;
        }

        String statusLabel = isRussian ? "Статус: " : "Status: ";
        lore.add(Component.text(statusLabel)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(statusText.decoration(TextDecoration.ITALIC, false)));

        // Дата создания
        String date = locale.getRaw("date-format");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(date);
        String formattedDate = sdf.format(report.getCreatedAt());
        lore.add(Component.empty());

        String createdLabel = isRussian ? "Создано: " : "Created: ";
        lore.add(Component.text(createdLabel + formattedDate)
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        // Информация о проверке
        if (report.getReviewedBy() != null) {
            lore.add(Component.empty());
            String reviewedLabel = isRussian ? "Проверено: " : "Reviewed by: ";
            lore.add(Component.text(reviewedLabel + report.getReviewedBy())
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            if (report.getAdminComment() != null) {
                String adminCommentLabel = isRussian ? "Комментарий администратора:" : "Admin comment:";
                lore.add(Component.text(adminCommentLabel)
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(report.getAdminComment())
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private void addNavigationButtons(Inventory gui, int page, int maxPage, Player player) {
        LocaleManager locale = plugin.getLocaleManager();
        boolean isRussian = locale.getCurrentLocale().equals("ru-RU");

        // Информация
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();

        String infoTitle = isRussian ? "Информация" : "Information";
        infoMeta.displayName(Component.text(infoTitle)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));

        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.empty());

        String pageLabel = isRussian ? "Страница: " : "Page: ";
        infoLore.add(Component.text(pageLabel + page + "/" + maxPage)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        var stats = plugin.getDataManager().getPlayerStatistics(player.getUniqueId());
        infoLore.add(Component.empty());

        String approvedLabel = isRussian ? "✓ Одобрено: " : "✓ Approved: ";
        infoLore.add(Component.text(approvedLabel + stats.getOrDefault(Report.ReportStatus.APPROVED, 0))
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        String rejectedLabel = isRussian ? "✗ Отклонено: " : "✗ Rejected: ";
        infoLore.add(Component.text(rejectedLabel + stats.getOrDefault(Report.ReportStatus.REJECTED, 0))
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        String pendingLabel = isRussian ? "⏳ На рассмотрении: " : "⏳ Pending: ";
        infoLore.add(Component.text(pendingLabel + stats.getOrDefault(Report.ReportStatus.PENDING, 0))
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        infoMeta.lore(infoLore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(27, infoItem);

        // Предыдущая страница
        if (page > 1) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            String prevText = isRussian ? "← Предыдущая страница" : "← Previous page";
            meta.displayName(Component.text(prevText)
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .decorate(TextDecoration.BOLD));
            prevButton.setItemMeta(meta);
            gui.setItem(30, prevButton);
        }

        // Кнопка обновления
        ItemStack refreshButton = new ItemStack(Material.CYAN_DYE);
        ItemMeta refreshMeta = refreshButton.getItemMeta();
        String refreshTitle = isRussian ? "🔄 Обновить" : "🔄 Refresh";
        refreshMeta.displayName(Component.text(refreshTitle)
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));

        List<Component> refreshLore = new ArrayList<>();
        refreshLore.add(Component.empty());
        String refreshLine1 = isRussian ? "Нажмите, чтобы обновить" : "Click to refresh";
        String refreshLine2 = isRussian ? "список жалоб" : "report list";
        refreshLore.add(Component.text(refreshLine1)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        refreshLore.add(Component.text(refreshLine2)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        refreshMeta.lore(refreshLore);
        refreshButton.setItemMeta(refreshMeta);
        gui.setItem(31, refreshButton);

        // Следующая страница
        if (page < maxPage) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            String nextText = isRussian ? "Следующая страница →" : "Next page →";
            meta.displayName(Component.text(nextText)
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .decorate(TextDecoration.BOLD));
            nextButton.setItemMeta(meta);
            gui.setItem(32, nextButton);
        }

        // Закрыть
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        String closeText = isRussian ? "Закрыть" : "Close";
        closeMeta.displayName(Component.text(closeText)
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
        closeButton.setItemMeta(closeMeta);
        gui.setItem(35, closeButton);
    }
}