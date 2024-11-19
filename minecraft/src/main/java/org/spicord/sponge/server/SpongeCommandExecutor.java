package org.spicord.sponge.server;

import eu.mcdb.universal.command.UniversalCommand;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.registrar.CommandRegistrar;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpongeCommandExecutor implements Command.Raw {

    private final UniversalCommand command;

    public SpongeCommandExecutor(final UniversalCommand command) {
        this.command = command;
    }

    @Override
    public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
        command.onCommand(
            cause.subject() instanceof ServerPlayer
                ? new SpongePlayer((ServerPlayer) cause.subject())
                : new SpongeCommandSender(cause),
            arguments.input().isEmpty()
                ? new String[0]
                : arguments.input().split(" ")
        );
        return CommandResult.success();
    }

    @Override
    public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
        return command.getSuggestions(
            cause.subject() instanceof ServerPlayer
                ? new SpongePlayer((ServerPlayer) cause.subject())
                : new SpongeCommandSender(cause),
            arguments.input().split(" ", -1) // Include trailing spaces
        ).stream().map(CommandCompletion::of).collect(Collectors.toList());
    }

    @Override
    public boolean canExecute(CommandCause cause) {
        String permission = command.getPermission();
        return
            permission == null
            || permission.isEmpty()
            || cause.hasPermission(permission);
    }

    @Override
    public Optional<Component> shortDescription(CommandCause cause) { return Optional.empty(); }

    @Override
    public Optional<Component> extendedDescription(CommandCause cause) { return Optional.empty(); }

    @Override
    public Component usage(CommandCause cause) { return null; }

    public static void register(Object pluginInstance, UniversalCommand command) {
        final Optional<PluginContainer> container = Sponge.game().pluginManager().fromInstance(pluginInstance);

        if (container.isPresent()) {
            final Optional<CommandRegistrar<Raw>> registrar = Sponge.server().commandManager().registrar(Command.Raw.class);

            if (registrar.isPresent()) {
                registrar.get().register(container.get(), new SpongeCommandExecutor(command), command.getName(), command.getAliases());
            }
        }
    }

}
