package org.spicord.velocity.server;

import com.velocitypowered.api.command.CommandSource;
import eu.mcdb.universal.command.UniversalCommandSender;
import net.kyori.adventure.text.Component;

public class VelocityCommandSender extends UniversalCommandSender {

    private final CommandSource source;

    public VelocityCommandSender(CommandSource source) {
        this.source = source;
    }

    @Override
    public boolean hasPermission(String permission) {
        return isEmpty(permission) || source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        source.sendMessage(Component.text(message));
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
