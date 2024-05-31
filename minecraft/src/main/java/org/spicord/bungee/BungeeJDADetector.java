package org.spicord.bungee;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeJDADetector {

    public static Set<ClassLoader> checkOtherJDA(SpicordBungee spicordPlugin) {
        Set<ClassLoader> conflicting = new HashSet<>();

        for (Plugin plugin : spicordPlugin.getProxy().getPluginManager().getPlugins()) {
            if (plugin == spicordPlugin) {
                continue;
            }

            ClassLoader classLoader = plugin.getClass().getClassLoader();

            URL cls = classLoader.getResource("net/dv8tion/jda/api/JDA.class");

            if (cls != null) {
                spicordPlugin.getLogger().warning("Found potential incompatibility problem with plugin: " + plugin.getDescription().getName());

                conflicting.add(classLoader);
            }
        }

        return conflicting;
    }
}
