package org.spicord.bungee.server;

import eu.mcdb.universal.command.UniversalCommandSender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

public class BungeeCommandSender extends UniversalCommandSender {

    private final CommandSender sender;

    public BungeeCommandSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasPermission(String permission) {
        return isEmpty(permission) || sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(new TextComponent(message));
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
