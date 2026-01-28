package com.reportsystem.listeners;

import com.reportsystem.ReportSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final ReportSystem plugin;

    public GUIListener(ReportSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // Проверяем, что это наш GUI
        Component title = event.getView().title();
        String titleString = PlainTextComponentSerializer.plainText().serialize(title);

        // Проверка на оба языка
        if (!titleString.contains("Reports") && !titleString.contains("жалоб")) {
            return;
        }

        // Отменяем взятие предметов
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Определяем текущую страницу из заголовка
        int currentPage = extractPageFromTitle(titleString);

        // Обработка кнопок навигации (стрелки)
        if (clicked.getType() == Material.ARROW) {
            Component displayName = clicked.getItemMeta().displayName();
            String buttonText = PlainTextComponentSerializer.plainText().serialize(displayName);

            // Проверяем на оба языка
            if (buttonText.contains("Предыдущая") || buttonText.contains("Previous")) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getReportsGUI().openGUI(player, currentPage - 1));
            } else if (buttonText.contains("Следующая") || buttonText.contains("Next")) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getReportsGUI().openGUI(player, currentPage + 1));
            }
        }
        // Обработка кнопки обновления (голубой краситель)
        else if (clicked.getType() == Material.CYAN_DYE) {
            Component displayName = clicked.getItemMeta().displayName();
            String buttonText = PlainTextComponentSerializer.plainText().serialize(displayName);

            if (buttonText.contains("Обновить") || buttonText.contains("Refresh")) {
                // Обновляем GUI на текущей странице
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getReportsGUI().openGUI(player, currentPage);
                    // Отправляем сообщение об обновлении
                    boolean isRussian = plugin.getLocaleManager().getCurrentLocale().equals("ru-RU");
                    String message = isRussian ? "Список жалоб обновлен!" : "Report list refreshed!";
                    player.sendMessage(Component.text(message)
                            .color(NamedTextColor.GREEN));
                });
            }
        }
        // Обработка кнопки закрытия
        else if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private int extractPageFromTitle(String title) {
        try {
            // Извлекаем номер страницы из "Ваши жалобы (стр. X/Y)" или "Your Reports (page X/Y)"
            int start, end;

            if (title.contains("стр.")) {
                start = title.indexOf("стр. ") + 5;
                end = title.indexOf("/", start);
            } else if (title.contains("page")) {
                start = title.indexOf("page ") + 5;
                end = title.indexOf("/", start);
            } else {
                return 1;
            }

            if (start > 4 && end > start) {
                return Integer.parseInt(title.substring(start, end).trim());
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        return 1;
    }
}