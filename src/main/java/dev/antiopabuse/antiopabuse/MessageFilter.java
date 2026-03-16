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

    // Private message commands (player or console)
    private static final Pattern PRIVATE_MSG_CMD = Pattern.compile(
        "(?i)(?:issued server command:|ran command:)\\s*/(?:msg|tell|w|whisper)\\b"
    );

    // Team chat command — filter this out
    private static final Pattern TEAM_CHAT = Pattern.compile(
        "(?i)(?:issued server command:|ran command:)\\s*/(?:team\\s+chat|tc)\\b"
    );

    // Auth plugin names
    private static final Pattern AUTH_PLUGIN = Pattern.compile(
        "(?i)\\b(?:authme|nlogin|fastlogin|loginprotection|xauth)\\b"
    );

    // Auth commands
    private static final Pattern AUTH_COMMANDS = Pattern.compile(
        "(?i)(?:issued server command:|ran command:)\\s*/(?:register|login|reg|l)\\b"
    );

    // Detects a player-issued command
    // Paper 1.21+: "_Kat issued server command: /gamemode creative"
    private static final Pattern PLAYER_COMMAND = Pattern.compile(
        "(?i)issued server command:"
    );

    // Detects a console-run command
    // Paper 1.21+: "Ran command: /say hello" or "[Rcon] Ran command /stop"
    private static final Pattern CONSOLE_COMMAND = Pattern.compile(
        "(?i)(?:ran command:|\\[rcon\\])"
    );

    // Detects either type of command for commands-only mode
    private static final Pattern IS_ANY_COMMAND = Pattern.compile(
        "(?i)(?:issued server command:|ran command:|\\[rcon\\])"
    );

    private MessageFilter() {}

    public static boolean isAllowed(String line, boolean commandsOnly) {
        if (line == null || line.isBlank()) return false;

        // Always block these
        if (IPV4.matcher(line).find())            return false;
        if (IPV6.matcher(line).find())            return false;
        if (PRIVATE_MSG_CMD.matcher(line).find()) return false;
        if (TEAM_CHAT.matcher(line).find())       return false;
        if (AUTH_PLUGIN.matcher(line).find())     return false;
        if (AUTH_COMMANDS.matcher(line).find())   return false;

        // In commands-only mode, drop anything that isn't a command
        if (commandsOnly && !IS_ANY_COMMAND.matcher(line).find()) return false;

        return true;
    }

    public static boolean isAllowed(String line) {
        return isAllowed(line, false);
    }
}
