package org.spicord.sponge.server;

import eu.mcdb.universal.command.UniversalCommandSender;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.CommandCause;

public class SpongeCommandSender extends UniversalCommandSender {

    private final CommandCause cause;

    public SpongeCommandSender(CommandCause cause) {
        this.cause = cause;
    }

    @Override
    public boolean hasPermission(String permission) {
        return isEmpty(permission) || cause.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        cause.sendMessage(Identity.nil(), Component.text(message));
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
