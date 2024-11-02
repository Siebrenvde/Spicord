package org.spicord.bukkit.server;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import eu.mcdb.universal.command.UniversalCommand;

import java.util.List;

public final class BukkitCommandExecutor implements CommandExecutor, TabCompleter {

    private final UniversalCommand command;

    public BukkitCommandExecutor(UniversalCommand command) {
        this.command = command;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
        return command.onCommand(
            sender instanceof Player
                ? new BukkitPlayer((Player) sender)
                : new BukkitCommandSender(sender),
            args
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return command.getSuggestions(
            sender instanceof Player
                ? new BukkitPlayer((Player) sender)
                : new BukkitCommandSender(sender),
            args
        );
    }

    public static void register(JavaPlugin plugin, UniversalCommand command) {
        PluginCommand pluginCommand = plugin.getCommand(command.getName());
        BukkitCommandExecutor executor = new BukkitCommandExecutor(command);

        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);
    }
}
