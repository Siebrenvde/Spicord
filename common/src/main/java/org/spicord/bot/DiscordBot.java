/*
 * Copyright (C) 2020  OopsieWoopsie
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spicord.bot;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.spicord.Spicord;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.api.bot.SimpleBot;
import org.spicord.api.bot.command.BotCommand;
import org.spicord.bot.command.DiscordBotCommand;
import org.spicord.bot.command.DiscordCommand;
import org.spicord.bot.command.SlashCommand;
import org.spicord.bot.command.SlashCommandGroup;
import org.spicord.bot.command.SlashCommandHandler;
import org.spicord.bot.command.SlashCommandOption;

import com.google.common.base.Preconditions;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.ApplicationTeam;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TeamMember;
import net.dv8tion.jda.api.entities.TeamMember.MembershipState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordBot extends SimpleBot {

    private final boolean enabled;

    protected final Collection<SimpleAddon> loadedAddons;
    protected final Map<String, Consumer<DiscordBotCommand>> commands;

    @Getter private final Collection<String> addons;

    @Deprecated @Getter private final boolean commandSupportEnabled;

    @Deprecated @Getter private final String commandPrefix;    

    private boolean initialCommandCleanup;

    @Getter private JDA jda;
    @Getter protected BotStatus status;

    private final Spicord spicord;
    private final Logger logger;
    @Getter private Presence presence;

    @Getter private long botId;

    /**
     * Create a new DiscordBot.<br>
     * 
     * @param spicord               the Spicord instance
     * @param name                  the bot name
     * @param token                 the bot token
     * @param enabled               true if the bot should start
     * @param addons                the list of addons IDs
     * @param initialCommandCleanup true if all the previously registered commands should be unregistered
     * @param commandSupportEnabled true if this bot should support commands
     * @param prefix                the command prefix for this bot
     * @see DiscordBotLoader#startBot(DiscordBot)
     */
    public DiscordBot(
        Spicord spicord,
        String name,
        String token,
        boolean enabled,
        List<String> addons,
        boolean initialCommandCleanup,
        boolean commandSupportEnabled,
        String prefix
    ) {
        super(name, token);

        this.spicord = spicord;
        this.logger = spicord.getLogger();

        this.enabled = enabled;
        this.addons = Collections.unmodifiableSet(new HashSet<>(addons));
        this.loadedAddons = new HashSet<SimpleAddon>();
        this.commandPrefix = prefix.trim();
        this.commands = new HashMap<String, Consumer<DiscordBotCommand>>();
        this.status = BotStatus.OFFLINE;
        this.presence = new Presence();

        this.initialCommandCleanup = initialCommandCleanup;

        if (commandSupportEnabled) {
            if (prefix.isEmpty()) {
                this.commandSupportEnabled = false;

                logger.severe(
                        "The command prefix cannot be empty. The command-support feature is now disabled on bot '"
                                + name + "'.");
                return;
            }
        }

        this.commandSupportEnabled = commandSupportEnabled;
    }

    @Override
    protected boolean start() {
        if (!enabled) return false;

        EnumSet<GatewayIntent> intents = EnumSet.allOf(GatewayIntent.class);
        // All privileged intents will be disabled in the future,
        // unless explicitly required by an addon.
        intents.remove(GatewayIntent.GUILD_PRESENCES);
        //intents.remove(GatewayIntent.GUILD_MEMBERS);
        //intents.remove(GatewayIntent.MESSAGE_CONTENT);

        Set<SimpleAddon> theAddons = spicord.getAddonManager().getAddons(this);

        for (SimpleAddon addon : theAddons) {
            intents.addAll(addon.getRequiredIntents());
        }

        try {
            this.status = BotStatus.STARTING;

            final JDABuilder builder = JDABuilder.create(token, intents)
                    .setAutoReconnect(true)
                    .addEventListeners(new BotStatusListener());

            ScheduledExecutorService threadPool = Spicord.getInstance().getThreadPool();

            builder.setAudioPool(threadPool, false);
            builder.setCallbackPool(threadPool, false);
            builder.setEventPool(threadPool, false);
            builder.setGatewayPool(threadPool, false);
            builder.setRateLimitScheduler(threadPool, false);

            for (CacheFlag flag : CacheFlag.values()) {
                if (flag.getRequiredIntent() == null) {
                    continue;
                }
                if (!intents.contains(flag.getRequiredIntent())) {
                    builder.disableCache(flag);
                }
            }

            this.jda = builder.build();

            if (commandSupportEnabled)
                jda.addEventListener(new BotCommandListener());

            theAddons.forEach(this::loadAddon);

            return true;
        } catch (InvalidTokenException e) {
            this.status = BotStatus.OFFLINE;
            this.jda = null;
            logger.severe("An error ocurred while starting the bot '" + getName() + "'. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private void warnMissingIntents() {
        logger.severe("=============================================");
        logger.severe("      OPEN THE DISCORD DEVELOPER PORTAL      ");
        logger.severe("       AND ENABLE THE GATEWAY INTENTS        ");
        logger.severe("                FOR YOUR BOT                 ");
        logger.severe(" https://discord.com/developers/applications ");
        logger.severe("=============================================");
    }

    /**
     * Check if the given user is the bot owner or member of the bot application team.
     * 
     * @param user the user
     * @return true if the user is privileged
     */
    public boolean isPrivilegedUser(User user) {
        ApplicationInfo appInfo = jda.retrieveApplicationInfo().complete();
        User owner = appInfo.getOwner();
        if (owner.getIdLong() == user.getIdLong()) {
            return true;
        }
        ApplicationTeam team = appInfo.getTeam();
        if (team != null) {
            for (TeamMember m : team.getMembers()) {
                if (m.getMembershipState() == MembershipState.ACCEPTED) {
                    if (m.getUser().getIdLong() == user.getIdLong()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void onReady(ReadyEvent event) {
        final SelfUser self = jda.getSelfUser();

        this.botId = self.getIdLong();

        logger.info(String.format("Logged in as %s#%s (id: %s)", self.getName(), self.getDiscriminator(), self.getId()));

        logger.info("Available Guilds:");
        for (Guild guild : jda.getGuilds()) {
            logger.info(String.format(" - %s (id: %s)", guild.getName(), guild.getId()));
        }

        for (SimpleAddon addon : loadedAddons) {
            addon.onReady(this);
        }
    }

    private void onMessageReceived(MessageReceivedEvent event) {
        loadedAddons.forEach(addon -> addon.onMessageReceived(this, event));
    }

    /**
     * Register a JDA event listener.
     * 
     * @param listener the event listener
     */
    public void addEventListener(ListenerAdapter listener) {
        jda.addEventListener(listener);
    }

    private Map<Long, Map<String, SlashCommandHandler>> commandHandlers = new HashMap<>();

    /**
     * Create a new SlashCommand instance.
     * It has to be later registered with the registerCommand() method.
     * The command will only be available after its registration.
     * 
     * @param name the command name
     * @param description the command description
     * @return the command instance
     */
    public SlashCommand commandBuilder(String name, String description) {
        return new SlashCommand(name, description);
    }

    /**
     * Register the given SlashCommand to the specified Guild.
     * 
     * @param command the command
     * @param guild the guild
     */
    public void registerCommand(SlashCommand command, Guild guild) {
        CommandCreateAction createAction = guild.upsertCommand(command.getName(), command.getDescription());
        registerCommand(command, createAction);
    }

    /**
     * Register the given SlashCommand globally.
     * 
     * @param command the command
     */
    public void registerCommand(SlashCommand command) {
        CommandCreateAction createAction = jda.upsertCommand(command.getName(), command.getDescription());
        registerCommand(command, createAction);
    }

    private void registerCommand(SlashCommand command, CommandCreateAction createAction) {
        Map<String, SlashCommandHandler> handlers = new LinkedHashMap<>();

        for (SlashCommandOption option : command.getOptions()) {
            createAction.addOptions(option.toJdaOption());
        }

        if (command.isSingle()) {
            final SlashCommandHandler handler = new SlashCommandHandler(
                command.getExecutor(),
                command.getCompleter()
            );
            handlers.put(command.getName(), handler);
        } else {
            for (SlashCommandGroup subcommandGroup : command.getSubcommandGroups()) {
                createAction.addSubcommandGroups(subcommandGroup.buildGroup());

                for (SlashCommand subcommand : subcommandGroup.getSubcommands()) {
                    final String id = String.format(
                        "%s %s %s",
                        command.getName(),
                        subcommandGroup.getName(),
                        subcommand.getName()
                    );
                    final SlashCommandHandler handler = new SlashCommandHandler(
                        subcommand.getExecutor(),
                        subcommand.getCompleter()
                    );
                    handlers.put(id, handler);
                }
            }

            for (SlashCommand subcommand : command.getSubcommands()) {
                createAction.addSubcommands(subcommand.toJdaSubcommand());

                final String id = String.format(
                    "%s %s",
                    command.getName(),
                    subcommand.getName()
                );
                final SlashCommandHandler handler = new SlashCommandHandler(
                    subcommand.getExecutor(),
                    subcommand.getCompleter()
                );
                handlers.put(id, handler);
            }
        }

        createAction.setNSFW(createAction.isNSFW());
        createAction.setGuildOnly(createAction.isGuildOnly());
        createAction.setDefaultPermissions(createAction.getDefaultPermissions());

        createAction.submit().thenAccept(jdaCommand -> {
            commandHandlers.put(jdaCommand.getIdLong(), handlers);
            spicord.debug("Registered discord command /" + jdaCommand.getName());
        });
    }

    /**
     * Register a command for this bot.
     * 
     * @param name    the command name (without prefix)
     * @param command the action to be performed when the command is executed
     * 
     * @throws NullPointerException if one of the arguments is null
     * @throws IllegalArgumentException if the {@code name} is empty or contains spaces
     */
    @Deprecated
    public void onCommand(String name, Consumer<DiscordBotCommand> command) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(command, "command");
        Preconditions.checkArgument(!name.trim().isEmpty(), "The command name cannot be empty.");
        Preconditions.checkArgument(!name.trim().contains(" "), "The command name cannot contain spaces (' ').");

        if (commandSupportEnabled) {
            if (commands.containsKey(name)) {
                logger.warning("The command '" + name + "' is already registered on bot '" + getName() + "'.");
            } else {
                commands.put(name, command);
            }
        } else {
            logger.warning("Cannot register command '" + name + "' on bot '" + getName()
                    + "' because the 'command-support' option is disabled.");
        }
    }

    /**
     * Register a command for this bot.
     * 
     * @param name    the command name (without prefix)
     * @param command the action to be performed when the command is executed
     * 
     * @throws NullPointerException if one of the arguments is null
     * @throws IllegalArgumentException if the {@code name} is empty or contains spaces
     */
    @Deprecated
    public void onCommand(String name, BotCommand command) {
        this.onCommand(name, comm -> command.onCommand(comm, comm.getArguments()));
    }

    /**
     * Register a command to this bot.
     * 
     * @param command the command to be registered
     */
    @Deprecated
    public void registerCommand(final DiscordCommand command) {
        this.onCommand(command.getName(), command);

        for (final String alias : command.getAliases()) {
            this.onCommand(alias, command);
        }
    }

    /**
     * Unregister a single command.
     * 
     * @param name the command name
     */
    @Deprecated
    public void unregisterCommand(String name) {
        commands.remove(name);
    }

    /**
     * Unregister various commands.
     * 
     * @param names the names of the commands
     */
    @Deprecated
    public void unregisterCommands(String... names) {
        for (String name : names) {
            commands.remove(name);
        }
    }

    /**
     * Load the given addon to this bot.
     * 
     * @param addon the addon to be loaded
     */
    public void loadAddon(SimpleAddon addon) {
        if (loadedAddons.add(addon)) {
            addon.onLoad(this);
        }
    }

    /**
     * Unload an addon from this bot.
     * 
     * @param <T> the addon instance type
     * @param addon the addon to unload
     */
    public <T extends SimpleAddon> void unloadAddon(T addon) {
        if (loadedAddons.remove(addon)) {
            unregisterCommands(addon.getCommands());
            addon.onUnload(this);
        }
    }

    /**
     * Check if this bot is enabled.
     * 
     * @see #isDisabled()
     * @return true if the bot is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if this bot is disabled.
     * 
     * @see #isEnabled()
     * @return true if the bot is disabled
     */
    public boolean isDisabled() {
        return !enabled;
    }

    /**
     * Check if this bot instance is connected to the
     * Discord gateway and is ready to perform operations.
     * 
     * @return true if the bot is ready
     */
    public boolean isReady() {
        return status == BotStatus.READY;
    }

    /**
     * Check if this bot instance is connected to the
     * Discord gateway.
     * 
     * @return true if the bot is connected
     */
    public boolean isConnected() {
        return isReady();
    }

    protected void shutdown() {
        status = BotStatus.STOPPING;
        loadedAddons.forEach(addon -> addon.onShutdown(this));

        if (jda != null) {
            for (Object listener : jda.getRegisteredListeners()) {
                jda.removeEventListener(listener);
            }

            ExecutorService pool = jda.getGatewayPool();
            if (pool instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor) pool).setRejectedExecutionHandler((r, executor) -> {
                    // NOP
                });
            }

            jda.shutdownNow();
        }

        jda = null;
        status = BotStatus.OFFLINE;

        commands.clear();
        loadedAddons.clear();
    }

    /**
     * Represents the current status of the bot.
     */
    public enum BotStatus {
        READY("Ready"),
        OFFLINE("Offline"),
        STARTING("Starting"),
        STOPPING("Stopping"),
        UNKNOWN("Unknown");

        private final String value;

        BotStatus(final String value) {
            this.value = value;
        }

        /**
         * Get the friendly name for this status.
         */
        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Get the JDA Status, can be any value from {@link JDA.Status}
     * or "-" if the JDA instance has not been created yet
     * 
     * @return the JDA Status name.
     */
    public String getJdaStatus() {
        if (jda != null) {
            return jda.getStatus().name();
        }
        return "-";
    }

    /**
     * This class provides utility methods to modify the bot presence status.
     */
    public class Presence {

        /**
         * Set the bot "Playing" status.
         * 
         * @param value the value to show next to it
         */
        public void setPlaying(String value) {
            jda.getPresence().setActivity(Activity.of(ActivityType.PLAYING, value));
        }

        /**
         * Set the bot "Listening" status.
         * 
         * @param value the value to show next to it
         */
        public void setListening(String value) {
            jda.getPresence().setActivity(Activity.of(ActivityType.LISTENING, value));
        }

        /**
         * Set the bot "Streaming" status.
         * 
         * @param value the value to show next to it
         */
        public void setStreaming(String value) {
            jda.getPresence().setActivity(Activity.of(ActivityType.STREAMING, value));
        }

        /**
         * Set the bot "Watching" status.
         * 
         * @param value the value to show next to it
         */
        public void setWatching(String value) {
            jda.getPresence().setActivity(Activity.of(ActivityType.WATCHING, value));
        }

        /**
         * Set the bot "Competing" status.
         * 
         * @param value the value to show next to it
         */
        public void setCompeting(String value) {
            jda.getPresence().setActivity(Activity.of(ActivityType.COMPETING, value));
        }

        /**
         * Set the bot custom status.
         * 
         * @param value the value to set
         */
        public void setCustom(String value) {
            jda.getPresence().setActivity(Activity.of(ActivityType.CUSTOM_STATUS, value));
        }

        /**
         * Set the bot online status to DND (red circle)
         */
        public void setDoNotDisturb() {
            jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        }

        /**
         * Set the bot online status to Idle (yellow circle)
         */
        public void setIdle() {
            jda.getPresence().setStatus(OnlineStatus.IDLE);
        }

        /**
         * Set the bot online status to Invisible (gray circle)
         */
        public void setInvisible() {
            jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
        }

        /**
         * Set the bot online status to Online (green circle)
         */
        public void setOnline() {
            jda.getPresence().setStatus(OnlineStatus.ONLINE);
        }
    }

    private class BotStatusListener extends ListenerAdapter {

        private final DiscordBot bot = DiscordBot.this;

        @Override
        public void onStatusChange(StatusChangeEvent event) {
            spicord.debug("Changed JDA Status [%s -> %s]", event.getOldStatus().name(), event.getNewStatus().name());
        }

        @Override
        public void onReady(ReadyEvent event) {
            if (bot.initialCommandCleanup) {

                // Delete all global commands
                jda.updateCommands().queue();

                // Delete all guild commands
                for (Guild guild : jda.getGuilds()) {
                    guild.updateCommands().queue();
                }

                spicord.debug("Cleaning up commands for bot %s", bot.getName());
            }

            bot.status = BotStatus.READY;
            bot.onReady(event);
        }

//        @Override
//        public void onStatusChange(StatusChangeEvent event) {
//            if (event.getNewStatus() == Status.SHUTDOWN) {
//                bot.status = BotStatus.OFFLINE;
//            }
//        }

        @Override
        public void onSessionResume(SessionResumeEvent event) {
            bot.status = BotStatus.READY;
        }

        @Override
        public void onSessionRecreate(SessionRecreateEvent event) {
            bot.status = BotStatus.READY;
        }

        @Override
        public void onSessionDisconnect(SessionDisconnectEvent event) {
            bot.status = BotStatus.OFFLINE;

            if (event.getCloseCode() == CloseCode.DISALLOWED_INTENTS) {
                warnMissingIntents();
            }
        }

        @Override
        public void onShutdown(ShutdownEvent event) {
            if (bot.status != BotStatus.STOPPING) {
                bot.shutdown();
            }
        }
    }

    private class BotCommandListener extends ListenerAdapter {

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            final Long commandId = event.getCommandIdLong();

            if (commandHandlers.containsKey(commandId)) {
                final String id = event.getFullCommandName();
                final Map<String, SlashCommandHandler> handlers = commandHandlers.get(commandId);

                if (handlers.containsKey(id)) {
                    handlers.get(id).execute(event);
                }
            }
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
            final Long commandId = event.getCommandIdLong();

            if (commandHandlers.containsKey(commandId)) {
                final String id = event.getFullCommandName();
                final Map<String, SlashCommandHandler> handlers = commandHandlers.get(commandId);

                if (handlers.containsKey(id)) {
                    handlers.get(id).complete(event);
                }
            }
        }

        private final DiscordBot bot = DiscordBot.this;

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            bot.onMessageReceived(event);

            String commandPrefix = bot.getCommandPrefix();
            String messageContent = event.getMessage().getContentRaw();

            if (messageContent.startsWith(commandPrefix)) {
                messageContent = messageContent.substring(commandPrefix.length());

                if (!messageContent.isEmpty()) {
                    String commandName = messageContent.split(" ")[0];
                    String[] args = messageContent.contains(" ")
                            ? messageContent.substring(commandName.length() + 1).split(" ")
                            : new String[0];

                    // the command instance will only be created if the get() method is called
                    Supplier<DiscordBotCommand> commandSupplier = () -> new DiscordBotCommand(commandName, args, event.getMessage());

                    if (bot.commands.containsKey(commandName)) {
                        bot.commands.get(commandName).accept(commandSupplier.get());
                    } else {
                        for (SimpleAddon addon : bot.loadedAddons) {
                            for (String cmd : addon.getCommands()) {
                                if (cmd.equals(commandName)) {
                                    addon.onCommand(commandSupplier.get(), args);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
