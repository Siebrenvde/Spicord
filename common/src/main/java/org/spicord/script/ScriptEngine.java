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

package org.spicord.script;

import java.util.HashMap;
import java.util.Map;

public abstract class ScriptEngine implements IScriptEngine {

    private final static Map<String, Class<? extends ScriptEngine>> engines;

    static {
        engines = new HashMap<>();
        registerEngine("rhino", RhinoScriptEngine.class);
    }

    /**
     * Get a new ScriptEngine instance for the requested engine.
     * 
     * @param name the engine name
     * @return the new ScriptEngine instance
     * @throws IllegalArgumentException if the engine was not found
     * @throws RuntimeException if the engine throws an error while creating a new instance
     */
    public static ScriptEngine getEngine(String name) {
        if (engines.containsKey(name)) {
            Class<? extends ScriptEngine> c = engines.get(name);
            try {
                return c.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("engine '" + name + "' not found");
        }
    }

    public static void registerEngine(String name, Class<? extends ScriptEngine> engineClass) {
        engines.put(name, engineClass);
    }

    public static ScriptEngine getDefaultEngine() {
        return getEngine("rhino");
    }
}
