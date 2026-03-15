package dev.antiopabuse.antiopabuse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.logging.Logger;

public final class RelayAppender extends AbstractAppender {

    private static final String APPENDER_NAME = "AntiOpAbuseDiscordAppender";

    private final WebhookDispatcher dispatcher;
    private final Logger            javaLogger;
    private volatile boolean        commandsOnly;

    public RelayAppender(WebhookDispatcher dispatcher, Logger javaLogger, boolean commandsOnly) {
        super(APPENDER_NAME, null,
              PatternLayout.newBuilder().withPattern("%msg%n").build(),
              true, Property.EMPTY_ARRAY);
        this.dispatcher   = dispatcher;
        this.javaLogger   = javaLogger;
        this.commandsOnly = commandsOnly;
    }

    public void setCommandsOnly(boolean commandsOnly) {
        this.commandsOnly = commandsOnly;
    }

    @Override
    public void append(LogEvent event) {
        try {
            String level   = event.getLevel().name();
            String message = event.getMessage().getFormattedMessage();

            if (message == null || message.isBlank()) return;

            // Strip Minecraft colour codes
            message = message.replaceAll("§[0-9a-fk-orA-FK-OR]", "");

            String line = "[" + level + "]: " + message;

            if (MessageFilter.isAllowed(line, commandsOnly)) {
                dispatcher.dispatch(line);
            }
        } catch (Exception e) {
            javaLogger.fine("[AntiOpAbuse] Appender error: " + e.getMessage());
        }
    }

    public void install() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            start();
            config.addAppender(this);
            config.getRootLogger().addAppender(this, null, null);
            ctx.updateLoggers();
            javaLogger.info("[AntiOpAbuse] Log appender installed.");
        } catch (Exception e) {
            javaLogger.severe("[AntiOpAbuse] Failed to install log appender: " + e.getMessage());
        }
    }

    public void uninstall() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            config.getRootLogger().removeAppender(APPENDER_NAME);
            ctx.updateLoggers();
            stop();
            javaLogger.info("[AntiOpAbuse] Log appender removed.");
        } catch (Exception e) {
            javaLogger.warning("[AntiOpAbuse] Error removing log appender: " + e.getMessage());
        }
    }

    public static void removeExisting() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            Appender existing = config.getAppenders().get(APPENDER_NAME);
            if (existing != null) {
                config.getRootLogger().removeAppender(APPENDER_NAME);
                ctx.updateLoggers();
                existing.stop();
            }
        } catch (Exception ignored) {}
    }
}
