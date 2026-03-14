package dev.antiopabuse.antiopabuse;

import java.util.regex.Pattern;

public final class MessageFilter {

    // IPv4: four octets separated by dots
    private static final Pattern IPV4 = Pattern.compile(
        "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}" +
        "(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"
    );

    // IPv6: at least two groups of hex digits separated by colons
    private static final Pattern IPV6 = Pattern.compile(
        "(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{0,4}"
    );

    // Private message commands
    private static final Pattern PRIVATE_MSG_CMD = Pattern.compile(
        "(?i)issued server command: /(?:msg|tell|w|whisper)\\b"
    );

    // Auth plugin names in log output
    private static final Pattern AUTH_PLUGIN = Pattern.compile(
        "(?i)\\b(?:authme|nlogin|fastlogin|loginprotection|xauth)\\b"
    );

    // Lines containing raw password attempts
    private static final Pattern PASSWORD_LINE = Pattern.compile(
        "(?i)issued server command: /(?:register|login|reg|l)\\b"
    );

    private MessageFilter() {}

    /**
     * Returns true if the line is safe to forward to Discord.
     */
    public static boolean isAllowed(String line) {
        if (line == null || line.isBlank()) return false;
        if (IPV4.matcher(line).find())              return false;
        if (IPV6.matcher(line).find())              return false;
        if (PRIVATE_MSG_CMD.matcher(line).find())   return false;
        if (AUTH_PLUGIN.matcher(line).find())       return false;
        if (PASSWORD_LINE.matcher(line).find())     return false;
        return true;
    }
}
