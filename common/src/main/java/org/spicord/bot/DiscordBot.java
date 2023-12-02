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

import com.google.common.base.Preconditions;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.ApplicationTeam;
import net.dv8tion.jda.api.entities.TeamMember;
import net.dv8tion.jda.api.entities.TeamMember.MembershipState;
import net.dv8tion.jda.api.entities.User;
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
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordBot extends SimpleBot {

    private final boolean enabled;

    protected final Collection<SimpleAddon> loadedAddons;
    protected final Map<String, Consumer<DiscordBotCommand>> commands;

    @Getter private final Collection<String> addons;
    @Getter private final boolean commandSupportEnabled;
    @Getter private final String commandPrefix;    

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
     * @param commandSupportEnabled true if this bot should support commands
     * @param prefix                the command prefix for this bot
     * @see {@link DiscordBotLoader#startBot(DiscordBot)}
     */
    public DiscordBot(Spicord spicord, String name, String token, boolean enabled, List<String> addons, boolean commandSupportEnabled, String prefix) {
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
            builder.setRateLimitPool(threadPool, false);

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

    public <T extends SimpleAddon> void unloadAddon(T addonInstance) {
        unregisterCommands(addonInstance.getCommands());
        loadedAddons.remove(addonInstance);
        addonInstance.onDisable();
    }

    private void onReady(ReadyEvent event) {
        this.botId = jda.getSelfUser().getIdLong();
        loadedAddons.forEach(addon -> addon.onReady(this));
    }

    private void onMessageReceived(MessageReceivedEvent event) {
        loadedAddons.forEach(addon -> addon.onMessageReceived(this, event));
    }

    /**
     * Add an event listener to this bot.
     * 
     * @param listener the event listener
     */
    public void addEventListener(ListenerAdapter listener) {
        jda.addEventListener(listener);
    }

    private Map<Long, SlashCommandExecutor> commandExecutors = new HashMap<>();
    private Map<Long, SlashCommandCompleter> commandCompleters = new HashMap<>();

    public SlashCommand commandBuilder(String name, String description) {
        return SlashCommand.builder(jda, name, description);
    }

    public Command registerCommand(SlashCommand commandData) {
        Command command = commandData.getCreateAction().complete();

        if (commandData.getExecutor() != null) {
            commandExecutors.put(command.getIdLong(), commandData.getExecutor());
        }
        if (commandData.getCompleter() != null) {
            commandCompleters.put(command.getIdLong(), commandData.getCompleter());
        }

        return command;
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
    public void onCommand(String name, BotCommand command) {
        this.onCommand(name, comm -> command.onCommand(comm, comm.getArguments()));
    }

    /**
     * Register a command to this bot.
     * 
     * @param command the command to be registered
     */
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
    public void unregisterCommand(String name) {
        commands.remove(name);
    }

    /**
     * Unregister various commands.
     * 
     * @param names the names of the commands
     */
    public void unregisterCommands(String... names) {
        for (String name : names) {
            commands.remove(name);
        }
    }

    /**
     * Load an addon to this bot.
     * 
     * @param addon the addon to be loaded
     */
    public void loadAddon(SimpleAddon addon) {
        loadedAddons.add(addon);
        addon.onLoad(this);
    }

    /**
     * Check if the bot is enabled.
     * 
     * @see #isDisabled()
     * @return true if the bot is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if the bot is disabled.
     * 
     * @see #isEnabled()
     * @return true if the bot is disabled
     */
    public boolean isDisabled() {
        return !enabled;
    }

    /**
     * Check if the bot is running.
     * 
     * @return true if the bot is running
     */
    public boolean isReady() {
        return status == BotStatus.READY;
    }

    /**
     * Check if this bot is connected to the Discord gateway.
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

        @Override
        public String toString() {
            return value;
        }
    }

    public String getJdaStatus() {
        if (jda != null) {
            return jda.getStatus().name();
        }
        return "-";
    }

    public class Presence {
        public void setPlaying(String name) {
            jda.getPresence().setActivity(Activity.of(ActivityType.PLAYING, name));
        }

        public void setListening(String name) {
            jda.getPresence().setActivity(Activity.of(ActivityType.LISTENING, name));
        }

        public void setStreaming(String name) {
            jda.getPresence().setActivity(Activity.of(ActivityType.STREAMING, name));
        }

        public void setWatching(String name) {
            jda.getPresence().setActivity(Activity.of(ActivityType.WATCHING, name));
        }

        public void setCompeting(String name) {
            jda.getPresence().setActivity(Activity.of(ActivityType.COMPETING, name));
        }

        public void setCustom(String name) {
            jda.getPresence().setActivity(Activity.of(ActivityType.CUSTOM_STATUS, name));
        }

        public void setDoNotDisturb() {
            jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        }

        public void setIdle() {
            jda.getPresence().setStatus(OnlineStatus.IDLE);
        }

        public void setInvisible() {
            jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
        }

        public void setOnline() {
            jda.getPresence().setStatus(OnlineStatus.ONLINE);
        }
    }

    private class BotStatusListener extends ListenerAdapter {

        private final DiscordBot bot = DiscordBot.this;

        @Override
        public void onStatusChange(StatusChangeEvent event) {
            if (spicord.getConfig().isDebugEnabled()) {
                spicord.getLogger().info("[DEBUG] Changed JDA Status [" + event.getOldStatus().name() + " -> " + event.getNewStatus().name() + "]");
            }
        }

        @Override
        public void onReady(ReadyEvent event) {
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

            if (commandExecutors.containsKey(commandId)) {
                commandExecutors.get(commandId).handle(event);
            }
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
            final Long commandId = event.getCommandIdLong();

            if (commandCompleters.containsKey(commandId)) {
                commandCompleters.get(commandId).handle(event);
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
