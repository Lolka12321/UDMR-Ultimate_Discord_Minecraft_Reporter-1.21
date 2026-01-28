package com.reportsystem.commands;

import com.reportsystem.ReportSystem;
import com.reportsystem.models.Report;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportCommand implements TabExecutor {

    private final ReportSystem plugin;

    public ReportCommand(ReportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Если команда вызвана как /reports - сразу показываем список
        if (label.equalsIgnoreCase("reports")) {
            return handleViewReports(sender);
        }

        // Обработка подкоманд
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "reload":
                    return handleReload(sender);

                case "help":
                    return handleHelp(sender);

                case "confirm":
                    if (sender instanceof Player && args.length > 1) {
                        boolean confirmed = args[1].equalsIgnoreCase("yes");
                        plugin.getReportManager().handleConfirmation((Player) sender, confirmed);
                        return true;
                    }
                    break;

                case "cancel":
                    if (sender instanceof Player && args.length > 1) {
                        boolean shouldCancel = args[1].equalsIgnoreCase("yes");
                        plugin.getReportManager().handleCancelPrompt((Player) sender, shouldCancel);
                        return true;
                    }
                    break;
            }
        }

        // Если нет аргументов или неизвестная подкоманда - создаем жалобу
        return handleCreateReport(sender);
    }

    private boolean handleCreateReport(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is only available for players!");
            return true;
        }

        Player player = (Player) sender;

        // Проверка прав
        if (!player.hasPermission("reportsystem.report")) {
            player.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return true;
        }

        // Проверка на активную сессию
        if (plugin.getReportManager().hasActiveSession(player.getUniqueId())) {
            plugin.getReportManager().showAlreadyCreatingPrompt(player);
            return true;
        }

        // Проверка лимита активных жалоб
        if (!plugin.getReportManager().canCreateReport(player)) {
            int max = plugin.getConfig().getInt("reports.max-active-reports", 5);
            player.sendMessage(plugin.getLocaleManager().getMessage("max-reports-reached",
                    "max", String.valueOf(max)));
            return true;
        }

        // Начинаем процесс создания жалобы
        plugin.getReportManager().startSession(player);
        return true;
    }

    private boolean handleViewReports(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is only available for players!");
            return true;
        }

        Player player = (Player) sender;

        // Проверка прав
        if (!player.hasPermission("reportsystem.reports")) {
            player.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return true;
        }

        // Открываем GUI с жалобами (первая страница)
        plugin.getReportsGUI().openGUI(player, 1);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("reportsystem.admin.reload")) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return true;
        }

        plugin.reloadConfig();
        plugin.getLocaleManager().reload();
        sender.sendMessage(plugin.getLocaleManager().getMessage("admin-reload"));
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== ReportSystem Help ===");
        sender.sendMessage("");

        // Команды для всех игроков
        if (sender.hasPermission("reportsystem.report")) {
            sender.sendMessage("§e/report §7- Create a new report");
        }
        if (sender.hasPermission("reportsystem.reports")) {
            sender.sendMessage("§e/reports §7- View your reports");
        }

        // Административные команды
        if (sender.hasPermission("reportsystem.admin")) {
            sender.sendMessage("");
            sender.sendMessage("§6§lAdmin Commands:");
            if (sender.hasPermission("reportsystem.admin.reload")) {
                sender.sendMessage("§e/report reload §7- Reload configuration");
            }
        }

        sender.sendMessage("");
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Первый аргумент - подкоманды
            List<String> subCommands = new ArrayList<>();

            // Административные команды
            if (sender.hasPermission("reportsystem.admin.reload")) {
                subCommands.add("reload");
            }

            // Всегда показываем help
            subCommands.add("help");

            // Фильтруем по введенному тексту
            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return completions;
    }
}