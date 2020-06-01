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
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;

class RhinoModuleManager implements ModuleManager {

    private final Map<String, Object> modules;
    private final RhinoScriptEngine engine;

    public RhinoModuleManager(RhinoScriptEngine scriptEngine) {
        this.modules = new HashMap<String, Object>();
        this.engine = scriptEngine;
        this.registerDefaultModules();
    }

    @Override
    public void register(String name, Class<?> clazz) {
        modules.put(name, engine.wrap(clazz));
    }

    @Override
    public void register(String name, Object obj) {
        modules.put(name, engine.javaToJS(obj));
    }

    @Override
    public boolean isRegistered(String name) {
        boolean registered = modules.containsKey(name);

        if (name.startsWith("class:") && !registered) {
            final String className = name.substring("class:".length());
            try {
                final Class<?> clazz = Class.forName(className);
                register(name, clazz);
                return true;
            } catch (Exception e) {}
        }

        return registered;
    }

    @Override
    public Object getModule(String name) {
        return modules.get(name);
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

    @Override
    public void registerNative(Object ins) {
        if (ins instanceof NativeObject) {
            NativeObject obj = (NativeObject) ins;
            NativeObject module = (NativeObject) obj.get("module");

            if (module == null || Undefined.isUndefined(module)) {
                // warn
                System.err.println("module obj = null");
                return;
            }

            String name = (String) module.get("name");
            Object exports = module.get("exports");

            if (name == null || exports == null) {
                // warn
                System.err.println("name or exports = null");
                return;
            }

            modules.put(name, exports);
        }
    }
}
