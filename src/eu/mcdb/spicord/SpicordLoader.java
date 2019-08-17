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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import eu.mcdb.spicord.config.SpicordConfiguration;
import eu.mcdb.spicord.util.SpicordClassLoader;
import eu.mcdb.util.ServerType;
import lombok.Getter;

public class SpicordLoader {

    /**
     * The server type.
     */
    @Getter
    private final ServerType serverType;

    /**
     * The {@link Spicord} instance.
     */
    private Spicord spicord;

    /**
     * The lib folder inside the plugin data folder.
     */
    private File libFolder;

    /**
     * The {@link SpicordClassLoader} instance.
     */
    @Getter
    private SpicordClassLoader classLoader;

    private Libraries libraries;

    /**
     * The {@link SpicordLoader} constructor.
     * 
     * @param logger      the {@link Spicord} instance
     * @param classLoader the plugin class loader
     * @param serverType  the server type
     */
    public SpicordLoader(Logger logger, ClassLoader classLoader, ServerType serverType) {
        Preconditions.checkNotNull(logger);
        Preconditions.checkNotNull(classLoader);

        this.classLoader = new SpicordClassLoader((URLClassLoader) classLoader);
        this.spicord = new Spicord(logger);
        this.serverType = serverType;
    }

    /**
     * Loads Spicord
     */
    public void load() {
        try {
            SpicordConfiguration config = new SpicordConfiguration(serverType);
            this.extractLibraries(config);
            this.loadLibraries();

            spicord.onLoad(config);
        } catch (IOException e) {
            spicord.getLogger().severe(
                    "Spicord could not be loaded, please report this error in \n\t -> https://github.com/OopsieWoopsie/Spicord/issues");
            disable();
            e.printStackTrace();
        }
    }

    /**
     * Turns off Spicord.
     */
    public void disable() {
        this.spicord.onDisable();
        this.spicord = null;
        this.libFolder = null;
    }

    /**
     * Extract the internal libraries to the plugin data folder.
     * 
     * @param config the {@link SpicordConfiguration} instance.
     */
    public void extractLibraries(SpicordConfiguration config) throws IOException {
        Preconditions.checkNotNull(config);

        // TODO: Move this
        InputStream in = getClass().getResourceAsStream("/lib/libraries.json");
        String json = new String(ByteStreams.toByteArray(in), Charset.defaultCharset());
        this.libraries = new Gson().fromJson(json, Libraries.class);

        JarFile jarFile = new JarFile(config.getFile());
        Enumeration<JarEntry> entries = jarFile.entries();

        this.libFolder = new File(config.getDataFolder(), "lib");

        if (!libFolder.exists())
            libFolder.mkdir();

        Preconditions.checkArgument(libFolder.isDirectory(), "File 'lib' must be a directory.");

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.getName().startsWith("lib/") && entry.getName().endsWith(".jar")) {
                String jarName = entry.getName();
                jarName = jarName.substring(jarName.lastIndexOf("/") + 1);

                File file = new File(libFolder, jarName);

                if (!file.exists()) {
                    Files.copy(jarFile.getInputStream(entry), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    spicord.getLogger().info("[Loader] Extracted library '" + file.getName() + "'.");
                }
            }
        }

        jarFile.close();
    }

    protected static boolean hasJDA;

    /**
     * Loads the libraries inside the plugin data folder.
     */
    public void loadLibraries() {
        Preconditions.checkNotNull(this.libFolder, "libFolder");
        Preconditions.checkArgument(this.libFolder.isDirectory(), "libFolder not directory");

        hasJDA = false;
        try {
            Class.forName("net.dv8tion.jda.core.JDA");
            hasJDA = true;
            spicord.getLogger().warning("Detected another JDA instance, some options will not work.");
        } catch (Exception e) {
        }

        for (String libName : libraries.getLibraries()) {
            if (hasJDA) {
                break;
            }
            File file = new File(libFolder, libName);
            if (file.isFile() && file.getName().endsWith(".jar")) {

                if (file.exists()) {
                    try {
                        getClassLoader().loadJar(file.toPath());
                        spicord.getLogger().info("[Loader] Loaded library '" + file.getName() + "'.");
                    } catch (Exception e) {
                        spicord.getLogger().severe("[Loader] Cannot load library '" + file.getName() + "'. " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    spicord.getLogger().severe("[Loader] Library '" + file.getName() + "' was not found on the library path.");
                }
            }
        }
        try {
            Class.forName("net.dv8tion.jda.core.JDA");
        } catch (ClassNotFoundException e) {
            spicord.getLogger().severe("[Loader] JDA library is not loaded, this plugin will not work.");
            this.disable();
        }
    }

    private class Libraries {

        @Getter
        private String[] libraries;
    }
}
