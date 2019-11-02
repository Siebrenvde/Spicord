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

package eu.mcdb.universal;

import org.bukkit.plugin.java.JavaPlugin;
import eu.mcdb.universal.command.BukkitCommandExecutor;
import eu.mcdb.universal.command.BungeeCommandExecutor;
import eu.mcdb.universal.command.UniversalCommand;
import net.md_5.bungee.api.plugin.Plugin;

public final class MCDB {

    private MCDB() {}

    public static void registerCommand(Plugin plugin, UniversalCommand command) {
        new registerBungeeCommand(plugin, command);
    }

    public static void registerCommand(JavaPlugin plugin, UniversalCommand command) {
        new registerBukkitCommand(plugin, command);
    }

    private static class registerBungeeCommand {
        registerBungeeCommand(Plugin plugin, UniversalCommand command) {
            plugin.getProxy().getPluginManager().registerCommand(plugin, new BungeeCommandExecutor(command));
        }
    }

    private static class registerBukkitCommand {
        registerBukkitCommand(JavaPlugin plugin, UniversalCommand command) {
            plugin.getCommand(command.getName()).setExecutor(new BukkitCommandExecutor(command));
        }
    }
}
