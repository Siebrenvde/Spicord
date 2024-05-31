package org.spicord.bukkit.server;

import java.util.concurrent.TimeUnit;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.spicord.AbstractServerScheduler;

public class BukkitServerScheduler extends AbstractServerScheduler {

    private final JavaPlugin plugin;
    private final Server server;

    public BukkitServerScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    private long toTicks(TimeUnit unit, long delay) {
        return unit.toMillis(delay) / 50; // Assuming(!) 20 ticks will always be 1 second
    }

    @Override
    protected ServerTask runTaskAsync(Runnable runnable) {
        return new ServerTaskImpl(server.getScheduler().runTaskAsynchronously(plugin, runnable));
    }
    
    @Override
    protected ServerTask runTaskAsyncLater(Runnable runnable, TimeUnit unit, long delay) {
        return new ServerTaskImpl(server.getScheduler().runTaskLaterAsynchronously(plugin, runnable, toTicks(unit, delay)));
    }
    
    @Override
    protected ServerTask runTaskAsyncLaterRepeating(Runnable runnable, TimeUnit unit, long delay, long period) {
        return new ServerTaskImpl(server.getScheduler().runTaskTimerAsynchronously(plugin, runnable, toTicks(unit, delay), toTicks(unit, period)));
    }

    private class ServerTaskImpl implements ServerTask {
        private final BukkitTask task;

        public ServerTaskImpl(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
