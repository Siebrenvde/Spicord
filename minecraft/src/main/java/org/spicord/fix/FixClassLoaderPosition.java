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

package org.spicord.fix;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.spicord.reflect.ReflectErrorRule;
import org.spicord.reflect.ReflectedField;
import org.spicord.reflect.ReflectedObject;

@SuppressWarnings("all")
public class FixClassLoaderPosition {

    public static void bukkit() {
        ClassLoader loader = FixClassLoaderPosition.class.getClassLoader();

        ReflectedField loadersField = new ReflectedObject(loader)
                .getField("loader").setAccessible()
                .getReflectValue()
                .getField("loaders").setAccessible().setModifiable();

        Map<String, Object> loaders = loadersField.getValue();
        Map<String, Object> newMap = new LinkedHashMap<String, Object>(loaders.size()+1);
        newMap.put("Spicord", loader);
        newMap.putAll(loaders);
        loadersField.setValue(newMap);
    }

    public static void bungee() {
        ClassLoader loader = FixClassLoaderPosition.class.getClassLoader();

        ReflectedField allLoadersField = new ReflectedObject(loader)
                .getField("allLoaders").setAccessible().setModifiable();

        Set<Object> allLoaders = allLoadersField.getValue();
        Set<Object> newSet = new LinkedHashSet<Object>();
        newSet.add(loader);
        newSet.addAll(allLoaders);
        allLoadersField.setValue(newSet);
    }
}
