package example.commandpanel.service;

import example.commandpanel.CommandPanelPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 点击冷却与传送冷却。
 */
public final class CooldownService {

    private final CommandPanelPlugin plugin;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleport = new ConcurrentHashMap<>();

    public CooldownService(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean allowClick(Player player) {
        if (player.hasPermission("commandpanel.bypass.cooldown")) {
            return true;
        }
        long cooldown = plugin.settings().clickCooldownMs();
        if (cooldown <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = lastClick.get(player.getUniqueId());
        if (previous != null && now - previous < cooldown) {
            return false;
        }
        lastClick.put(player.getUniqueId(), now);
        return true;
    }

    public long remainingTeleportSeconds(Player player) {
        if (player.hasPermission("commandpanel.bypass.cooldown")) {
            return 0;
        }
        int cooldown = plugin.settings().teleportCooldownSeconds();
        if (cooldown <= 0) {
            return 0;
        }
        Long previous = lastTeleport.get(player.getUniqueId());
        if (previous == null) {
            return 0;
        }
        long remainingMs = cooldown * 1000L - (System.currentTimeMillis() - previous);
        return remainingMs <= 0 ? 0 : (remainingMs + 999L) / 1000L;
    }

    public void markTeleport(Player player) {
        lastTeleport.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clear(UUID playerId) {
        lastClick.remove(playerId);
        lastTeleport.remove(playerId);
    }
}
