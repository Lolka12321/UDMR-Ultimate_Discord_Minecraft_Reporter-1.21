package com.reportsystem.listeners;

import com.reportsystem.ReportSystem;
import com.reportsystem.models.ReportSession;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final ReportSystem plugin;

    public ChatListener(ReportSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Проверяем, есть ли у игрока активная сессия
        if (!plugin.getReportManager().hasActiveSession(player.getUniqueId())) {
            return;
        }

        // Отменяем обычное сообщение в чат
        event.setCancelled(true);

        // Получаем текст сообщения
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Обрабатываем ввод синхронно на главном потоке
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getReportManager().handleSessionInput(player, message);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Удаляем сессию при выходе игрока
        Player player = event.getPlayer();
        if (plugin.getReportManager().hasActiveSession(player.getUniqueId())) {
            plugin.getReportManager().cancelSession(player.getUniqueId());
        }
    }
}