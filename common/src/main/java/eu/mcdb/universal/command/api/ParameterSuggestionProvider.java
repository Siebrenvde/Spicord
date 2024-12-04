package eu.mcdb.universal.command.api;

import java.util.List;

/**
 * Provides suggestions for a {@link CommandParameter}
 */
@FunctionalInterface
public interface ParameterSuggestionProvider {

    List<String> suggest(String[] args);

}
