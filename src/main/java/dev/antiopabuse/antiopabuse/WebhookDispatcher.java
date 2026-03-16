package dev.antiopabuse.antiopabuse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class WebhookDispatcher {

    private static final int MAX_CONTENT_LENGTH = 1_950;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private volatile String  webhookUrl;
    private volatile boolean codeBlock;
    private final Logger     logger;

    private final ThreadPoolExecutor executor;
    private final AtomicLong rateLimitResetMs = new AtomicLong(0L);

    public WebhookDispatcher(String webhookUrl, boolean codeBlock, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.codeBlock  = codeBlock;
        this.logger     = logger;

        this.executor = new ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(512),
            r -> {
                Thread t = new Thread(r, "AntiOpAbuse-Webhook");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    public void updateSettings(String newUrl, boolean newCodeBlock) {
        this.webhookUrl = newUrl;
        this.codeBlock  = newCodeBlock;
    }

    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isBlank()
            && !webhookUrl.equals("DISCORD_WEBHOOK_HERE");
    }

    public void dispatch(String rawLine) {
        if (!isConfigured()) return;
        executor.submit(() -> sendNow(rawLine, false));
    }

    /** Blocking send for shutdown. */
    public void dispatchNow(String rawLine) {
        if (!isConfigured()) return;
        sendNow(rawLine, true);
    }

    public String testWebhook() {
        if (!isConfigured()) {
            return "§cWebhook URL is not configured! Edit plugins/AntiOpAbuse/config.yml";
        }
        try {
            int status = sendNow("AntiOpAbuse test message — webhook is working!", true);
            if (status == 204 || status == 200) {
                return "§aWebhook OK (HTTP " + status + ") — check your Discord channel!";
            } else if (status == 429) {
                return "§eRate limited by Discord (HTTP 429). Try again in a moment.";
            } else if (status == 401 || status == 403) {
                return "§cInvalid webhook URL — Discord returned HTTP " + status + ". Re-check config.yml.";
            } else if (status == 404) {
                return "§cWebhook not found (HTTP 404) — the webhook may have been deleted.";
            } else {
                return "§cUnexpected response: HTTP " + status;
            }
        } catch (Exception e) {
            return "§cFailed to reach Discord: " + e.getMessage();
        }
    }

    private int sendNow(String rawLine, boolean blocking) {
        long waitMs = rateLimitResetMs.get() - System.currentTimeMillis();
        if (waitMs > 0 && !blocking) {
            try { Thread.sleep(waitMs + 50); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return -1; }
        }

        String content = buildContent(rawLine);
        String payload = buildJsonPayload(content);

        try {
            URL url = URI.create(webhookUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "AntiOpAbuse-MinecraftPlugin/1.1");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(8_000);

            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
                os.flush();
            }

            int status = conn.getResponseCode();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                if (status >= 400 && status != 429) {
                    logger.warning("[AntiOpAbuse] Discord error body: " + sb);
                }
            } catch (Exception ignored) {}

            if (status == 429) {
                String resetAfter = conn.getHeaderField("X-RateLimit-Reset-After");
                double seconds = 2.0;
                if (resetAfter != null) {
                    try { seconds = Double.parseDouble(resetAfter); } catch (NumberFormatException ignored) {}
                }
                rateLimitResetMs.set(System.currentTimeMillis() + (long)(seconds * 1_000));
                if (!blocking) executor.submit(() -> sendNow(rawLine, false));
            } else if (status >= 400) {
                logger.warning("[AntiOpAbuse] Webhook returned HTTP " + status);
            }

            conn.disconnect();
            return status;

        } catch (Exception e) {
            logger.warning("[AntiOpAbuse] Failed to send webhook: "
                + e.getClass().getSimpleName() + ": " + e.getMessage());
            return -1;
        }
    }

    private String buildContent(String rawLine) {
        String timestamp = TIME_FMT.format(Instant.now()) + " UTC";
        String line = truncate(rawLine.trim(), MAX_CONTENT_LENGTH - 50);
        if (codeBlock) {
            return "```\n[" + timestamp + "] " + sanitiseCodeBlock(line) + "\n```";
        } else {
            return "`[" + timestamp + "]` " + line;
        }
    }

    private static String sanitiseCodeBlock(String s) { return s.replace("`", "'"); }
    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
    private static String buildJsonPayload(String content) {
        return "{\"content\":\"" + jsonEscape(content) + "\"}";
    }
    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
