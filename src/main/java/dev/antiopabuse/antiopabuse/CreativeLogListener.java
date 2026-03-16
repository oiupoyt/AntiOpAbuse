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

        // Must be clicking INSIDE the creative menu itself — not enderchest,
        // not player inventory, not any other container
        if (event.getClickedInventory() == null) return;
        if (event.getView().getType() != InventoryType.CREATIVE) return;
        if (event.getClickedInventory().getType() != InventoryType.CREATIVE) return;

        // Only care about:
        //   LEFT/RIGHT click  = taking an item from the menu (PICKUP_ALL / PICKUP_HALF)
        //   MIDDLE click      = clone/duplicate a full stack (CLONE_STACK)
        // Everything else (putting back, shift-click to hotbar, etc.) is ignored
        InventoryAction action = event.getAction();
        ClickType       click  = event.getClick();

        boolean isTake = action == InventoryAction.PICKUP_ALL     // left click take
                      || action == InventoryAction.PICKUP_HALF    // right click take half
                      || action == InventoryAction.PICKUP_ONE;    // right click take one

        boolean isDuplicate = action == InventoryAction.CLONE_STACK  // middle click
                           || click  == ClickType.MIDDLE;

        if (!isTake && !isDuplicate) return;

        // Get the item being taken
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        // Per-player cooldown to avoid spam
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        Long last = cooldowns.get(uid);
        if (last != null && (now - last) < COOLDOWN_MS) return;
        cooldowns.put(uid, now);

        String action_label = isDuplicate ? "duplicated" : "took";
        String itemName     = formatItemName(item);
        int    amount       = isDuplicate ? 64 : item.getAmount(); // middle click always gives 64
        String playerName   = player.getName();

        String line = "[CREATIVE] " + playerName + " " + action_label + " " + amount + "x " + itemName;

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
