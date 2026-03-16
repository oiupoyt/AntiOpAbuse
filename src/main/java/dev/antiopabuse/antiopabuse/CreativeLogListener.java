package dev.antiopabuse.antiopabuse;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class CreativeLogListener implements Listener {

    private final WebhookDispatcher dispatcher;
    private final LogHistory        history;
    private final Logger            logger;
    private final Map<UUID, Long>   cooldowns = new HashMap<>();
    private static final long       COOLDOWN_MS = 1_000;

    public CreativeLogListener(WebhookDispatcher dispatcher, LogHistory history, Logger logger) {
        this.dispatcher = dispatcher;
        this.history    = history;
        this.logger     = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;

        // The open GUI must be the creative menu — this blocks enderchests,
        // chests, and any other container a creative player might have open
        if (event.getView().getType() != InventoryType.CREATIVE) return;

        // Only log actual takes and middle-click duplication.
        // Ignore putting items back, dragging, shift-clicking to hotbar, etc.
        InventoryAction action = event.getAction();
        ClickType       click  = event.getClick();

        boolean isTake = action == InventoryAction.PICKUP_ALL
                      || action == InventoryAction.PICKUP_HALF
                      || action == InventoryAction.PICKUP_ONE
                      || action == InventoryAction.PICKUP_SOME;

        boolean isDuplicate = action == InventoryAction.CLONE_STACK
                           || click  == ClickType.MIDDLE;

        if (!isTake && !isDuplicate) return;

        // Get the item — check current slot and cursor
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) item = event.getCursor();
        if (item == null || item.getType().isAir()) return;

        // Per-player cooldown
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        Long last = cooldowns.get(uid);
        if (last != null && (now - last) < COOLDOWN_MS) return;
        cooldowns.put(uid, now);

        String actionLabel = isDuplicate ? "duplicated" : "took";
        String itemName    = formatItemName(item);
        int    amount      = isDuplicate ? 64 : item.getAmount();
        String playerName  = player.getName();

        String line = "[CREATIVE] " + playerName + " " + actionLabel + " " + amount + "x " + itemName;

        history.add(line);
        logger.info(line);
        dispatcher.dispatch(line);
    }

    private static String formatItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta() != null
                && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName()
                       .replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        }
        String raw = item.getType().name();
        String[] words = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0)))
              .append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
