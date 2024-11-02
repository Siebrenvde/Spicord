package org.spicord.bungee.server;

import eu.mcdb.universal.command.UniversalCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class BungeeCommandExecutor extends Command implements TabExecutor {

    private final UniversalCommand command;

    public BungeeCommandExecutor(final UniversalCommand command) {
        super(command.getName(), command.getPermission(), command.getAliases());
        this.command = command;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        command.onCommand(
            sender instanceof ProxiedPlayer
                ? new BungeePlayer(((ProxiedPlayer) sender))
                : new BungeeCommandSender(sender),
            args
        );
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return command.getSuggestions(
            sender instanceof ProxiedPlayer
                ? new BungeePlayer(((ProxiedPlayer) sender))
                : new BungeeCommandSender(sender),
            args
        );
    }


    public static void register(Plugin plugin, UniversalCommand command) {
        plugin.getProxy().getPluginManager().registerCommand(plugin, new BungeeCommandExecutor(command));
    }
}
