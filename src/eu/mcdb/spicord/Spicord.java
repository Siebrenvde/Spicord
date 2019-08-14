/*
 * Copyright (C) 2019  OopsieWoopsie
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

package eu.mcdb.spicord;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import eu.mcdb.spicord.addon.AddonManager;
import eu.mcdb.spicord.addon.InfoAddon;
import eu.mcdb.spicord.addon.PlayersAddon;
import eu.mcdb.spicord.addon.PluginsAddon;
import eu.mcdb.spicord.api.ISpicord;
import eu.mcdb.spicord.bot.DiscordBot;
import eu.mcdb.spicord.bot.DiscordBotLoader;
import eu.mcdb.spicord.config.SpicordConfiguration;
import eu.mcdb.util.ServerType;
import lombok.Getter;
import net.dv8tion.jda.core.utils.JDALogger;

public class Spicord implements ISpicord {

    /**
     * The {@link Spicord} instance.
     */
    private static Spicord instance;

    /**
     * The {@link Spicord} version
     */
    @Getter
    private static final String version = "2.1.0-SNAPSHOT";

    /**
     * The {@link Logger} instance.
     */
    @Getter
    private Logger logger;

    /**
     * The server type.
     */
    @Getter
    private ServerType serverType;

    /**
     * The Spicord configuration.
     */
    @Getter
    private SpicordConfiguration config;

    /**
     * The addon manager.
     */
    @Getter
    private AddonManager addonManager;

    /**
     * The Spicord constructor.
     * 
     * @param logger the logger instance
     */
    public Spicord(Logger logger) {
        instance = this;
        this.logger = logger;
        this.addonManager = new AddonManager(this);
    }

    protected void onLoad(SpicordConfiguration config) throws IOException {
        if (!isLoaded())
            return;

        this.config = config;
        this.serverType = config.getServerType();

        this.registerIntegratedAddons();

        try {
            // TODO: Don't use reflection for this
            Class<?> loggerClass = Class.forName("eu.mcdb.spicord.logger.JDALogger");
            Constructor<?> constructor = loggerClass.getConstructor(boolean.class, boolean.class);
            Object loggerInstance = constructor.newInstance(config.isDebugEnabled(), config.isJdaMessagesEnabled());
            Method method = JDALogger.class.getMethod("setLog", loggerClass.getInterfaces()[0]);
            method.invoke(null, loggerInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (config.isJdaMessagesEnabled()) {
            debug("Successfully enabled JDA messages.");
        } else {
            debug("Successfully disabled JDA messages.");
        }

        getLogger().info("Starting the bots...");
        config.getBots().forEach(DiscordBotLoader::startBot);
    }

    private void registerIntegratedAddons() {
        this.getAddonManager().registerAddon(new InfoAddon());
        this.getAddonManager().registerAddon(new PluginsAddon());
        this.getAddonManager().registerAddon(new PlayersAddon());
    }

    protected void onDisable() {
        getLogger().info("Disabling Spicord...");
        config.getBots().forEach(DiscordBotLoader::shutdownBot);
        config.getBots().clear();
        addonManager.getAddons().clear();
        this.addonManager = null;
        this.serverType = null;
        this.logger = null;
        this.config = null;
        instance = null;
    }

    /**
     * @deprecated As of snapshot 2.0.0, use
     *             {@link DiscordBotLoader#startBot(DiscordBot)} instead.
     */
    @Deprecated
    public boolean startBot(DiscordBot bot) {
        return DiscordBotLoader.startBot(bot);
    }

    /**
     * Shutdown a bot if it is enabled.
     * 
     * @param bot the bot object.
     * @deprecated As of snapshot 2.0.0, use
     *             {@link DiscordBotLoader#shutdownBot(DiscordBot)} instead.
     */
    @Deprecated
    public void shutdownBot(DiscordBot bot) {
        DiscordBotLoader.shutdownBot(bot);
    }

    /**
     * Get a bot by its name.
     * 
     * @param name the bot name.
     * @return the {@link DiscordBot} object if the bot exists, or null if not.
     */
    public DiscordBot getBotByName(String name) {
        for (DiscordBot bot : config.getBots())
            if (bot.getName().equals(name))
                return bot;

        return null;
    }

    /**
     * Gets the Spicord instance.
     * 
     * @throws IllegalStateException if Spicord has not loaded.
     * @return the Spicord instance.
     * @see {@link #isLoaded()}
     */
    public static Spicord getInstance() {
        if (!isLoaded())
            throw new IllegalStateException("Spicord has not loaded yet.");

        return instance;
    }

    /**
     * Check if Spicord was loaded.
     * 
     * @return true if Spicord is loaded, or false if not.
     */
    public static boolean isLoaded() {
        return instance != null;
    }

    /**
     * Displays a message if the debug mode if enabled.
     * 
     * @param msg the message to be displayed.
     */
    public void debug(String msg) {
        if (config.isDebugEnabled())
            getLogger().info("[DEBUG] " + msg);
    }
}
