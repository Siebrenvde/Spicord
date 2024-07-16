package org.spicord.bungee.server;

import java.util.concurrent.TimeUnit;

import org.spicord.AbstractServerScheduler;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class BungeeServerScheduler extends AbstractServerScheduler {

    private final Plugin plugin;
    private final ProxyServer server;

    public BungeeServerScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getProxy();
    }

    @Override
    protected boolean isPluginEnabled() {
        return true;
    }

    @Override
    protected ServerTask runTaskAsync(Runnable runnable) {
        return new ServerTaskImpl(server.getScheduler().runAsync(plugin, runnable));
    }

    @Override
    protected ServerTask runTaskAsyncLater(Runnable runnable, TimeUnit unit, long delay) {
        return new ServerTaskImpl(server.getScheduler().schedule(plugin, runnable, delay, unit));
    }

    @Override
    protected ServerTask runTaskAsyncLaterRepeating(Runnable runnable, TimeUnit unit, long delay, long period) {
        return new ServerTaskImpl(server.getScheduler().schedule(plugin, runnable, delay, period, unit));
    }

    private class ServerTaskImpl implements ServerTask {
        private final ScheduledTask task;

        private boolean cancelled = false;

        public ServerTaskImpl(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
            this.cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
