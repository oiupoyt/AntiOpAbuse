package dev.antiopabuse.antiopabuse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe in-memory ring buffer that stores the last 50
 * command / creative log entries for /abalogs display.
 */
public final class LogHistory {

    public static final int MAX_ENTRIES = 50;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    public record Entry(String timestamp, String line) {}

    private final Deque<Entry> entries = new ArrayDeque<>(MAX_ENTRIES);

    /** Add a line to the history. Drops the oldest if full. */
    public synchronized void add(String line) {
        if (entries.size() >= MAX_ENTRIES) entries.pollFirst();
        entries.addLast(new Entry(TIME_FMT.format(Instant.now()), line));
    }

    /** Returns a snapshot of all entries, oldest first. */
    public synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    public synchronized int size() {
        return entries.size();
    }
}
