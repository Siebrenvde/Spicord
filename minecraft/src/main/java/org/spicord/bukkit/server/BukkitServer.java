package org.spicord.bukkit.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spicord.util.VanishAPI;

import eu.mcdb.universal.command.UniversalCommand;
import eu.mcdb.universal.player.UniversalPlayer;

public class BukkitServer extends eu.mcdb.universal.Server {

    private final Server server;
    private final Plugin plugin;
    private BukkitServerScheduler scheduler;

    public BukkitServer(Server server, JavaPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
        this.scheduler = new BukkitServerScheduler(plugin);
    }

    @Override
    public int getOnlineCount() {
        return server.getOnlinePlayers().size();
    }

    @Override
    public int getPlayerLimit() {
        return server.getMaxPlayers();
    }

    @Override
    public String[] getOnlinePlayers() {
        final Set<String> result = new HashSet<>();
        final VanishAPI vanish = VanishAPI.get();
        for (Player player : server.getOnlinePlayers()) {
            if (vanish.isVisible(player)) {
                result.add(player.getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public Map<String, List<String>> getServersAndPlayers() {
        final Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("default", Arrays.asList(getOnlinePlayers()));

        return map;
    }

    @Override
    public String getVersion() {
        return server.getVersion();
    }

    @Override
    public String[] getPlugins() {
        return Stream.of(server.getPluginManager().getPlugins())
                .map(Plugin::getName)
                .toArray(String[]::new);
    }

    @Override
    public boolean dispatchCommand(String command) {
        Boolean result = callSyncMethod(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        return result == null ? false : result;
    }

    private <T> T callSyncMethod(Callable<T> task) {
        try {
            if (Bukkit.isPrimaryThread()) {
                return task.call();
            } else {
                return Bukkit.getScheduler().callSyncMethod(plugin, task).get(3, TimeUnit.SECONDS);
            }
        } catch (Throwable e) {
            if (e instanceof ExecutionException) {
                e = e.getCause();
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
        }
        return null;
    }

    @Override
    public Logger getLogger() {
        return server.getLogger();
    }

    @Override
    public UniversalPlayer getPlayer(UUID uuid) {
        final OfflinePlayer player = server.getOfflinePlayer(uuid);

        if (!player.isOnline()) {
            return null;
        }

        return new BukkitPlayer(player.getPlayer());
    }

    @Override
    public void broadcast(String message) {
        server.broadcastMessage(message);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> Runnable registerListener(Class<T> event, Consumer<T> handler) {
        ListenerExecutor<T> listener = (l, e) -> handler.accept((T) e);

        server.getPluginManager().registerEvent(
            event,
            listener,
            EventPriority.NORMAL,
            listener,
            plugin
        );

        return () -> HandlerList.unregisterAll(listener);
    }

    private interface ListenerExecutor<T> extends Listener, EventExecutor {}

    @Override
    public void registerCommand(Object plugin, UniversalCommand command) {
        BukkitCommandExecutor.register((JavaPlugin) plugin, command);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
