package common.models;

import java.util.regex.Matcher;

public interface CLICommands {
    String invalidCommand = "Invalid Command";

    Matcher getMatcher(String input);

    default boolean matches(String input) {
        Matcher matcher = getMatcher(input);
        return matcher.matches();
    }

    default String getGroup(String input, int group) {
        Matcher matcher = getMatcher(input);
        if (matcher.matches()) {
            return matcher.group(group);
        }
        return null;
    }
}
