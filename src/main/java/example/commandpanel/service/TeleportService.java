package example.commandpanel.service;

import example.commandpanel.CommandPanelPlugin;
import example.commandpanel.util.SafeLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * “传送到玩家”和“回到上次位置”。
 */
public final class TeleportService {

    private static final int MAX_HISTORY = 10;

    private final CommandPanelPlugin plugin;
    private final Map<UUID, Deque<Location>> history = new HashMap<>();

    public TeleportService(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleportToPlayer(Player player, Player target) {
        if (!plugin.settings().teleportEnabled()) {
            plugin.messages().send(player, "teleport-disabled");
            return;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            plugin.messages().send(player, "teleport-self");
            return;
        }
        if (!target.isOnline() || !player.canSee(target)) {
            plugin.messages().send(player, "teleport-target-offline", "target", target.getName());
            return;
        }
        if (!plugin.settings().crossWorld()
                && !player.getWorld().equals(target.getWorld())) {
            plugin.messages().send(player, "teleport-cross-world");
            return;
        }

        long remaining = plugin.cooldowns().remainingTeleportSeconds(player);
        if (remaining > 0) {
            plugin.messages().send(player, "cooldown", "seconds", String.valueOf(remaining));
            return;
        }

        Location destination = target.getLocation().clone();
        Location safe = plugin.settings().safeLanding()
                ? SafeLocation.find(destination, 3, 3)
                : destination;
        if (safe == null || safe.getWorld() == null) {
            plugin.messages().send(player, "teleport-fail");
            return;
        }

        Location origin = player.getLocation().clone();
        String targetName = target.getName();
        player.closeInventory();
        plugin.messages().send(player, "teleport-start", "target", targetName);

        try {
            player.teleportAsync(safe, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .thenAccept(result -> Bukkit.getScheduler().runTask(plugin,
                            () -> finishTeleportToPlayer(player, origin, targetName, result)));
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("传送 " + player.getName() + " 时出错: " + exception.getMessage());
            plugin.messages().send(player, "teleport-fail");
        }
    }

    private void finishTeleportToPlayer(Player player, Location origin,
                                        String targetName, Boolean result) {
        if (!player.isOnline()) {
            return;
        }
        if (Boolean.TRUE.equals(result)) {
            remember(player, origin);
            plugin.cooldowns().markTeleport(player);
            plugin.messages().send(player, "teleport-done", "target", targetName);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
        } else {
            plugin.messages().send(player, "teleport-fail");
        }
    }

    public void back(Player player) {
        Deque<Location> stack = history.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            plugin.messages().send(player, "back-none");
            return;
        }

        Location target = stack.peek();
        Location safe = plugin.settings().safeLanding()
                ? SafeLocation.find(target, 3, 3)
                : target;
        if (safe == null || safe.getWorld() == null) {
            plugin.messages().send(player, "back-fail");
            return;
        }

        stack.pop();
        player.closeInventory();
        try {
            player.teleportAsync(safe, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .thenAccept(result -> Bukkit.getScheduler().runTask(plugin,
                            () -> finishBack(player, target, result)));
        } catch (RuntimeException exception) {
            stack.push(target);
            plugin.getLogger().warning("返回时出错: " + exception.getMessage());
            plugin.messages().send(player, "back-fail");
        }
    }

    private void finishBack(Player player, Location target, Boolean result) {
        if (!player.isOnline()) {
            return;
        }
        if (Boolean.TRUE.equals(result)) {
            plugin.messages().send(player, "back-done");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
        } else {
            Deque<Location> stack = history.get(player.getUniqueId());
            if (stack != null) {
                stack.push(target);
            }
            plugin.messages().send(player, "back-fail");
        }
    }

    public void remember(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Deque<Location> stack = history.computeIfAbsent(
                player.getUniqueId(), ignored -> new ArrayDeque<>());
        stack.push(location.clone());
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    public void forget(UUID playerId) {
        history.remove(playerId);
    }
}
