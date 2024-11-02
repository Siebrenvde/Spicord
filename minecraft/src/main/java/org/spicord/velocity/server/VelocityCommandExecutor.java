package org.spicord.velocity.server;

import org.spicord.plugin.VelocityPlugin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import eu.mcdb.universal.command.UniversalCommand;

import java.util.List;

public final class VelocityCommandExecutor implements SimpleCommand {

    private final UniversalCommand command;

    public VelocityCommandExecutor(UniversalCommand command) {
        this.command = command;
    }

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        command.onCommand(
            source instanceof Player
                ? new VelocityPlayer((Player) source)
                : new VelocityCommandSender(source),
            invocation.arguments()
        );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        final CommandSource source = invocation.source();
        return command.getSuggestions(
            source instanceof Player
                ? new VelocityPlayer((Player) source)
                : new VelocityCommandSender(source),
            invocation.arguments()
        );
    }

    public static void register(Object plugin, UniversalCommand command) {
        VelocityPlugin.getCommandManager().register(command.getName(), new VelocityCommandExecutor(command), command.getAliases());
    }
}
