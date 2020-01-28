package org.spicord.script;

/**
 * The ModuleManager utility provides methods for module registrations,
 * the modules can be accessed from a JavaScript environment using the
 * {@code require('module name')} function.
 */
public interface ModuleManager {

    /**
     * Register a new module class. <br>
     * A new class instance can be created from a JavaScript environment using the
     * '{@code new}' keyword. <br>
     * For example: <br>
     * {@code register("java-string", String.class);} <br>
     * can be accessed from JavaScript by using: <br>
     * <br>
     * <code>
     * const JavaString = require("java-string"); <br><br>
     * // in this example the static method 'format' can <br>
     * // be accessed and a new String instance is created <br>
     * var world = new JavaString("World"); <br>
     * console.log(JavaString.format("%s %s", "Hello", world)); <br><br>
     * // output: Hello World
     * </code>
     * 
     * @param name  the module name
     * @param clazz the module class
     */
    void register(String name, Class<?> clazz);

    /**
     * Register a new module instance. <br>
     * The module instance can be accessed from a JavaScript environment using the
     * {@code require('module name')} function. <br>
     * 
     * @param name     the module name
     * @param instance the module instance
     */
    void register(String name, Object instance);

    /**
     * Check if a module is registered.
     * 
     * @param name the module name
     * @return true if the module is registered
     */
    boolean isRegistered(String name);

    /**
     * Get a module instance or class.
     * 
     * @param name the module name
     * @return the module instance
     */
    Object getModule(String name);

}
