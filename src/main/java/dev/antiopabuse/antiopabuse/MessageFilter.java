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

    // Team chat
    private static final Pattern TEAM_CHAT = Pattern.compile(
        "(?i)issued server command: /(?:teammsg|tm|team\\s+chat|team\\s+msg)\\b"
    );

    // Auth plugin names
    private static final Pattern AUTH_PLUGIN = Pattern.compile(
        "(?i)\\b(?:authme|nlogin|fastlogin|loginprotection|xauth)\\b"
    );

    // Auth commands
    private static final Pattern AUTH_COMMANDS = Pattern.compile(
        "(?i)issued server command: /(?:register|login|reg|l)\\b"
    );

    // ── Noise filter patterns (used when commands-only is true) ──────────

    // Player chat — <username> always appears in chat lines regardless of
    // secure/not-secure mode, server software, or chat plugins
    private static final Pattern PLAYER_CHAT = Pattern.compile(
        "<[^>]+>"
    );

    // Join/leave/death messages
    private static final Pattern JOIN_LEAVE = Pattern.compile(
        "(?i)(?:joined the game|left the game|lost connection:|has just earned|made the advancement|reached the goal|Challenge complete)"
    );

    // Generic plugin info/debug spam (lines starting with plugin brackets that aren't CREATIVE)
    // e.g. [SomePlugin] doing internal stuff
    // We allow [AntiOpAbuse] and [CREATIVE] through
    private static final Pattern PLUGIN_SPAM = Pattern.compile(
        "^\\[\\w+\\]: \\[(?!AntiOpAbuse|CREATIVE)[A-Za-z0-9_\\-]+\\] (?!.*issued server command)"
    );

    // "Player moved too quickly", "Can't keep up", "Reached end of stream" etc
    private static final Pattern SERVER_NOISE = Pattern.compile(
        "(?i)(?:moved too quickly|moved wrongly|invalid move|reach distance|keep up|end of stream|read timed out|connection reset|com\\.mojang\\.authlib)"
    );

    public static final Pattern IS_LOGGABLE = Pattern.compile(
        "(?i)(?:issued server command:|\\[CREATIVE\\])"
    );

    private MessageFilter() {}

    public static boolean isAllowed(String line, boolean commandsOnly) {
        if (line == null || line.isBlank()) return false;

        // Always block these regardless of mode
        if (IPV4.matcher(line).find())            return false;
        if (IPV6.matcher(line).find())            return false;
        if (PRIVATE_MSG_CMD.matcher(line).find()) return false;
        if (TEAM_CHAT.matcher(line).find())       return false;
        if (AUTH_PLUGIN.matcher(line).find())     return false;
        if (AUTH_COMMANDS.matcher(line).find())   return false;

        if (commandsOnly) {
            // [CREATIVE] always passes
            if (line.contains("[CREATIVE]"))      return true;

            // Block chat, joins/leaves, server noise
            if (PLAYER_CHAT.matcher(line).find())   return false;
            if (JOIN_LEAVE.matcher(line).find())     return false;
            if (SERVER_NOISE.matcher(line).find())   return false;
            if (PLUGIN_SPAM.matcher(line).find())    return false;

            // everything else passes — commands, console output, warnings, etc.
        }

        return true;
    }

    public static boolean isAllowed(String line) {
        return isAllowed(line, false);
    }
}
