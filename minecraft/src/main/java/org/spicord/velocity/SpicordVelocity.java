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

package org.spicord.velocity;

import java.io.File;
import org.spicord.SpicordCommand;
import org.spicord.SpicordLoader;
import org.spicord.SpicordPlugin;
import org.spicord.Version;
import org.spicord.plugin.VelocityPlugin;
import org.spicord.reflect.ReflectUtils;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

@Plugin(id = "spicord", name = "Spicord", version = Version.VERSION, authors = { "Sheidy" })
public class SpicordVelocity extends VelocityPlugin implements SpicordPlugin {

    private SpicordLoader loader;

    @Inject
    public SpicordVelocity(ProxyServer proxyServer) {
        super(proxyServer);
        this.loader = new SpicordLoader(getLogger(), getDataFolder());
        this.loader.load();
        new SpicordCommand(() -> {}).register(this);
    }

    @Override
    public File getFile() {
        return ReflectUtils.getJarFile(SpicordVelocity.class);
    }
}
