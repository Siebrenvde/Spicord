package org.spicord.bukkit.server;

import eu.mcdb.universal.command.UniversalCommandSender;
import org.bukkit.command.CommandSender;

public class BukkitCommandSender extends UniversalCommandSender {

    private final CommandSender sender;

    public BukkitCommandSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasPermission(String permission) {
        return isEmpty(permission) || sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
