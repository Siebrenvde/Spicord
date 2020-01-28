package org.spicord.script;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

public interface IScriptEngine {

    /**
     * Evaluate a JavaScript source script.
     * 
     * @param script the script source
     * @return the execution result
     * @throws ScriptException
     */
    <T> T eval(String script) throws ScriptException;

    /**
     * Call a JavaScript function available on the global scope.
     * 
     * @param name the function name
     * @param args the function arguments (optional)
     * @return the execution result
     * @throws ScriptException
     */
    <T> T callFunction(String name, Object... args) throws ScriptException;

    /**
     * Call a JavaScript function.
     * 
     * @param function the function instance
     * @param args     the function arguments (optional)
     * @return the execution result
     * @throws ScriptException
     */
    <T> T callFunction(Object function, Object... args) throws ScriptException;

    /**
     * Load a JavaScript script in a vanilla JavaScript environment, functions like {@code require()} and {@code console.log()} are not available there.
     * 
     * @see {@link #require(File)}
     * @param file the script file
     * @return the execution result
     * @throws IOException     if an I/O error occurrs
     * @throws ScriptException
     */
    <T> T loadScript(File file) throws IOException, ScriptException;

    /**
     * Load a JavaScript script in a vanilla JavaScript environment, functions like {@code require()} and {@code console.log()} are not available there.
     * 
     * @see {@link #require(File)}
     * @param reader the script reader
     * @return the execution result
     * @throws IOException     if an I/O error occurrs
     * @throws ScriptException
     */
    <T> T loadScript(Reader reader) throws IOException, ScriptException;

    /**
     * Load a JavaScript script in a NodeJS-like environment.
     * 
     * @param dir the directory where the script is located
     * @param name the script name
     * @return the {@code module.exports} value
     * @throws Exception
     */
    <T> T require(String dir, String name) throws IOException;

    /**
     * Load a JavaScript script in a NodeJS-like environment.
     * 
     * @param file the script file
     * @return the {@code module.exports} value
     * @throws Exception
     */
    <T> T require(File file) throws IOException;

    /**
     * Unwrap a JavaScript object.
     * <br>
     * An engine may wrap a Java class or instance to be accessible through a
     * JavaScript environment, this method reverts that effect.
     * 
     * @param object the JavaScript object
     * @return the unwraped object
     */
    <T> T java(Object object);

    /**
     * Get the module manager.
     * 
     * @return the module manager
     */
    ModuleManager getModuleManager();

}
