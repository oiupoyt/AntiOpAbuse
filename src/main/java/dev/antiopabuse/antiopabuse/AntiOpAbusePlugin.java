package dev.antiopabuse.antiopabuse;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class AntiOpAbusePlugin extends JavaPlugin implements CommandExecutor {

    private WebhookDispatcher dispatcher;
    private RelayAppender     appender;

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

        dispatcher = new WebhookDispatcher(webhookUrl, codeBlock, getLogger());

        RelayAppender.removeExisting();
        appender = new RelayAppender(dispatcher, getLogger(), commandsOnly);
        appender.install();

        getServer().getPluginManager().registerEvents(
            new CreativeLogListener(dispatcher, getLogger()), this
        );

        getCommand("antiopabuse").setExecutor(this);

        getLogger().info("AntiOpAbuse v" + getDescription().getVersion()
            + " enabled — forwarding console to Discord."
            + (commandsOnly ? " [commands-only mode]" : ""));

        if (dispatcher.isConfigured()) {
            new BukkitRunnable() {
                @Override public void run() {
                    getLogger().info("[AntiOpAbuse] Console relay is active and forwarding to Discord.");
                }
            }.runTaskLater(this, 60L);
        }
    }

    @Override
    public void onDisable() {
        if (appender != null)   appender.uninstall();
        if (dispatcher != null) dispatcher.shutdown();
        getLogger().info("AntiOpAbuse disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be an operator to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6AntiOpAbuse §7v" + getDescription().getVersion());
            sender.sendMessage("§7Usage: §f/antiopabuse webhook §7- test the Discord webhook");
            sender.sendMessage("§7Usage: §f/antiopabuse reload  §7- reload config");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "webhook" -> {
                sender.sendMessage("§7[AntiOpAbuse] Testing webhook, please wait...");
                new BukkitRunnable() {
                    @Override public void run() {
                        final String result = dispatcher.testWebhook();
                        new BukkitRunnable() {
                            @Override public void run() {
                                sender.sendMessage("§7[AntiOpAbuse] " + result);
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
                sender.sendMessage("§7[AntiOpAbuse] §aConfig reloaded."
                    + (newCommandsOnly ? " §7(commands-only mode ON)" : ""));
            }

            default -> sender.sendMessage("§cUnknown sub-command. Use: webhook | reload");
        }

        return true;
    }
}
