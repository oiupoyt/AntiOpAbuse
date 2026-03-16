package dev.antiopabuse.antiopabuse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LogHistory {

    public static final int MAX_ENTRIES = 50;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    public record Entry(String timestamp, String line) {}

    private final Deque<Entry> entries = new ArrayDeque<>(MAX_ENTRIES);

    public synchronized void add(String line) {
        if (entries.size() >= MAX_ENTRIES) entries.pollFirst();
        entries.addLast(new Entry(TIME_FMT.format(Instant.now()), line));
    }

    public synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    public synchronized int size() {
        return entries.size();
    }
}
