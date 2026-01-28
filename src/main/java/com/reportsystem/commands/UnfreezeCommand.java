package com.reportsystem.commands;

import com.reportsystem.ReportSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnfreezeCommand implements CommandExecutor {

    private final ReportSystem plugin;

    public UnfreezeCommand(ReportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("reportsystem.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /unfreeze <player>");
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found or not online.");
            return true;
        }

        // Размораживаем игрока
        target.setWalkSpeed(0.2f);
        target.setFlySpeed(0.1f);
        target.setInvulnerable(false);

        boolean isRussian = plugin.getLocaleManager().getCurrentLocale().equals("ru-RU");

        String adminMessage = isRussian
                ? "§aИгрок §f" + target.getName() + " §aразморожен!"
                : "§aPlayer §f" + target.getName() + " §aunfrozen!";

        String playerMessage = isRussian
                ? "§aВы были разморожены администратором."
                : "§aYou have been unfrozen by an administrator.";

        sender.sendMessage(adminMessage);
        target.sendMessage(playerMessage);
        target.sendTitle("§a✓", isRussian ? "§aРазморожен" : "§aUnfrozen", 10, 40, 10);

        return true;
    }
}