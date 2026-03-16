package dev.antiopabuse.antiopabuse;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class AntiOpAbusePlugin extends JavaPlugin implements CommandExecutor {

    private WebhookDispatcher dispatcher;
    private RelayAppender     appender;
    private LogHistory        history;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        reloadConfig();

        String  webhookUrl   = getConfig().getString("webhook-url", "");
        boolean codeBlock    = getConfig().getBoolean("send-as-codeblock", true);
        boolean commandsOnly = getConfig().getBoolean("commands-only", false);

        if (webhookUrl.isBlank() || webhookUrl.equals("DISCORD_WEBHOOK_HERE")) {
            getLogger().severe("╔══════════════════════════════════════════════════╗");
            getLogger().severe("║  AntiOpAbuse: webhook-url is not configured!     ║");
            getLogger().severe("║  Edit plugins/AntiOpAbuse/config.yml             ║");
            getLogger().severe("╚══════════════════════════════════════════════════╝");
        }

        history    = new LogHistory();
        dispatcher = new WebhookDispatcher(webhookUrl, codeBlock, getLogger());

        RelayAppender.removeExisting();
        appender = new RelayAppender(dispatcher, history, getLogger(), commandsOnly);
        appender.install();

        getServer().getPluginManager().registerEvents(
            new CreativeLogListener(dispatcher, history, getLogger()), this
        );

        getCommand("antiopabuse").setExecutor(this);
        getCommand("abalogs").setExecutor(this);

        getLogger().info("AntiOpAbuse v" + getDescription().getVersion()
            + " enabled — forwarding console to Discord."
            + (commandsOnly ? " [commands-only mode]" : ""));

        if (dispatcher.isConfigured()) {
            new BukkitRunnable() {
                @Override public void run() {
                    String version = getServer().getVersion();
                    dispatcher.dispatch("🟢 Server started — " + version);
                }
            }.runTaskLater(this, 60L);
        }
    }

    @Override
    public void onDisable() {
        if (dispatcher != null && dispatcher.isConfigured()) {
            dispatcher.dispatchNow("🔴 Server stopped.");
        }
        if (appender != null)   appender.uninstall();
        if (dispatcher != null) dispatcher.shutdown();
        getLogger().info("AntiOpAbuse disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        // ── /abalogs — available to all players ───────────────────────────
        if (command.getName().equalsIgnoreCase("abalogs")) {
            List<LogHistory.Entry> entries = history.snapshot();

            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━ " +
                ChatColor.YELLOW + "AntiOpAbuse Logs" +
                ChatColor.GOLD + " ━━━━━━━━━━");

            if (entries.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No logs yet — nothing suspicious has happened.");
                sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return true;
            }

            for (LogHistory.Entry entry : entries) {
                // Colour-code by type
                String line = entry.line();
                ChatColor color;
                if (line.contains("[CREATIVE]")) {
                    color = ChatColor.AQUA;        // creative grabs = aqua
                } else if (line.toLowerCase().contains("/op")
                        || line.toLowerCase().contains("/deop")) {
                    color = ChatColor.RED;          // op/deop = red, high priority
                } else {
                    color = ChatColor.WHITE;        // everything else = white
                }

                sender.sendMessage(ChatColor.DARK_GRAY + "[" + entry.timestamp() + "] "
                    + color + line);
            }

            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage(ChatColor.GRAY + "Showing " + entries.size()
                + "/" + LogHistory.MAX_ENTRIES + " most recent entries.");
            return true;
        }

        // ── /antiopabuse — OP only ─────────────────────────────────────────
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "AntiOpAbuse " + ChatColor.GRAY
                + "v" + getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Usage: " + ChatColor.WHITE
                + "/antiopabuse webhook " + ChatColor.GRAY + "- test the Discord webhook");
            sender.sendMessage(ChatColor.GRAY + "Usage: " + ChatColor.WHITE
                + "/antiopabuse reload  " + ChatColor.GRAY + "- reload config");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "webhook" -> {
                sender.sendMessage(ChatColor.GRAY + "[AntiOpAbuse] Testing webhook, please wait...");
                new BukkitRunnable() {
                    @Override public void run() {
                        final String result = dispatcher.testWebhook();
                        new BukkitRunnable() {
                            @Override public void run() {
                                sender.sendMessage(ChatColor.GRAY + "[AntiOpAbuse] " + result);
                            }
                        }.runTask(AntiOpAbusePlugin.this);
                    }
                }.runTaskAsynchronously(this);
            }

            case "reload" -> {
                reloadConfig();
                String  newUrl          = getConfig().getString("webhook-url", "");
                boolean newCb           = getConfig().getBoolean("send-as-codeblock", true);
                boolean newCommandsOnly = getConfig().getBoolean("commands-only", false);
                dispatcher.updateSettings(newUrl, newCb);
                appender.setCommandsOnly(newCommandsOnly);
                sender.sendMessage(ChatColor.GRAY + "[AntiOpAbuse] " + ChatColor.GREEN
                    + "Config reloaded."
                    + (newCommandsOnly ? ChatColor.GRAY + " (commands-only mode ON)" : ""));
            }

            default -> sender.sendMessage(ChatColor.RED
                + "Unknown sub-command. Use: webhook | reload");
        }

        return true;
    }
}
