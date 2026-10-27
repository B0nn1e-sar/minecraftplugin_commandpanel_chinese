package example.commandpanel.service;

import example.commandpanel.CommandPanelPlugin;
import example.commandpanel.util.SafeLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 个人重生点的设置、清除、传送与重生安全落点。
 */
public final class SpawnService {

    private final CommandPanelPlugin plugin;

    public SpawnService(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    public void setSpawn(Player player) {
        Location location = player.getLocation().clone();
        player.setRespawnLocation(location, true);
        plugin.messages().send(player, "spawn-set",
                "world", location.getWorld() == null ? "?" : location.getWorld().getName(),
                "x", String.valueOf(location.getBlockX()),
                "y", String.valueOf(location.getBlockY()),
                "z", String.valueOf(location.getBlockZ()));
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.7f, 1.0f);
    }

    public void clearSpawn(Player player) {
        player.setRespawnLocation(null, true);
        plugin.messages().send(player, "spawn-cleared");
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 1.0f);
    }

    public void teleportToSpawn(Player player) {
        Location respawn = player.getRespawnLocation();
        boolean personal = respawn != null && respawn.getWorld() != null;
        if (!personal) {
            plugin.messages().send(player, "spawn-none");
        }
        Location destination = personal ? respawn : player.getWorld().getSpawnLocation();
        teleport(player, destination, personal ? "spawn-teleported" : "spawn-world");
    }

    public void teleportToWorldSpawn(Player player) {
        teleport(player, player.getWorld().getSpawnLocation(), "spawn-world");
    }

    private void teleport(Player player, Location destination, String successMessage) {
        Location safe = destination;
        if (plugin.settings().safeLanding() && !SafeLocation.isRespawnSafe(destination)) {
            safe = SafeLocation.find(destination, 3, 3);
        }
        if (safe == null || safe.getWorld() == null) {
            plugin.messages().send(player, "spawn-fail");
            return;
        }

        Location origin = player.getLocation().clone();
        player.closeInventory();
        try {
            player.teleportAsync(safe, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .thenAccept(result -> Bukkit.getScheduler().runTask(plugin,
                            () -> finishTeleport(player, origin, successMessage, result)));
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("传送 " + player.getName() + " 时出错: " + exception.getMessage());
            plugin.messages().send(player, "spawn-fail");
        }
    }

    private void finishTeleport(Player player, Location origin, String successMessage, Boolean result) {
        if (!player.isOnline()) {
            return;
        }
        if (Boolean.TRUE.equals(result)) {
            plugin.teleports().remember(player, origin);
            plugin.messages().send(player, successMessage);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
        } else {
            plugin.messages().send(player, "spawn-fail");
        }
    }

    /**
     * 重生时如果落脚点被堵住，自动在附近找一个安全位置。
     * 床和重生锚交给原版处理，避免把玩家挪下床。
     */
    public void handleRespawn(PlayerRespawnEvent event) {
        if (!plugin.settings().safeRespawn()) {
            return;
        }
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        Location location = event.getRespawnLocation();
        if (SafeLocation.isRespawnSafe(location)) {
            return;
        }
        Location safe = SafeLocation.find(location, 3, 3);
        if (safe != null) {
            event.setRespawnLocation(safe);
        }
    }
}
