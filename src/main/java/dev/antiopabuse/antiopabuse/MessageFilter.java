package dev.antiopabuse.antiopabuse;

import java.util.regex.Pattern;

public final class MessageFilter {

    // IPv4
    private static final Pattern IPV4 = Pattern.compile(
        "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}" +
        "(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"
    );

    // IPv6
    private static final Pattern IPV6 = Pattern.compile(
        "(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{0,4}"
    );

    // Private message commands
    private static final Pattern PRIVATE_MSG_CMD = Pattern.compile(
        "(?i)issued server command: /(?:msg|tell|w|whisper)\\b"
    );

    // Auth plugin names
    private static final Pattern AUTH_PLUGIN = Pattern.compile(
        "(?i)\\b(?:authme|nlogin|fastlogin|loginprotection|xauth)\\b"
    );

    // Auth commands — always filtered regardless of commands-only mode
    private static final Pattern AUTH_COMMANDS = Pattern.compile(
        "(?i)issued server command: /(?:register|login|reg|l)\\b"
    );

    // Detects any line where someone issued a command (player or console)
    // e.g. "_Kat issued server command: /gamemode creative"
    //      "Console issued server command: /stop"
    private static final Pattern IS_COMMAND = Pattern.compile(
        "(?i)issued server command:"
    );

    private MessageFilter() {}

    /**
     * @param line         the raw console line
     * @param commandsOnly if true, only command lines are forwarded
     * @return true if the line is safe and should be forwarded
     */
    public static boolean isAllowed(String line, boolean commandsOnly) {
        if (line == null || line.isBlank()) return false;

        // Always block these regardless of mode
        if (IPV4.matcher(line).find())            return false;
        if (IPV6.matcher(line).find())            return false;
        if (PRIVATE_MSG_CMD.matcher(line).find()) return false;
        if (AUTH_PLUGIN.matcher(line).find())     return false;
        if (AUTH_COMMANDS.matcher(line).find())   return false;

        // In commands-only mode, drop anything that isn't a command
        if (commandsOnly && !IS_COMMAND.matcher(line).find()) return false;

        return true;
    }

    /** Backwards-compatible overload — defaults to commandsOnly = false. */
    public static boolean isAllowed(String line) {
        return isAllowed(line, false);
    }
}
