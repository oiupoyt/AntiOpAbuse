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

        // From the debug output we know exactly what creative menu clicks look like:
        // view=CREATIVE, clicked=PLAYER, action=PLACE_ALL, click=CREATIVE
        // This combination is ONLY possible when taking from the creative item list.
        if (event.getView().getType() != InventoryType.CREATIVE) return;
        if (event.getClick() != ClickType.CREATIVE) return;
        if (event.getAction() != InventoryAction.PLACE_ALL) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        // per-player cooldown
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        Long last = cooldowns.get(uid);
        if (last != null && (now - last) < COOLDOWN_MS) return;
        cooldowns.put(uid, now);

        String itemName = formatItemName(item);
        int    amount   = item.getAmount();
        String line     = "[CREATIVE] " + player.getName() + " took " + amount + "x " + itemName;

        history.add(line);
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
