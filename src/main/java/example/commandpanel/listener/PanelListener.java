package example.commandpanel.listener;

import example.commandpanel.CommandPanelPlugin;
import example.commandpanel.gui.MenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;

/**
 * 面板点击保护、进服提示和重生安全处理。
 */
public final class PanelListener implements Listener {

    private final CommandPanelPlugin plugin;

    public PanelListener(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() != top) {
            return;
        }
        plugin.gui().handleClick(player, holder, event.getRawSlot());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.settings().joinHint()) {
            return;
        }
        Player player = event.getPlayer();
        long delay = plugin.settings().joinHintDelayTicks();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Component hint = plugin.messages().component("join-hint")
                    .clickEvent(ClickEvent.runCommand("/cp"))
                    .hoverEvent(HoverEvent.showText(
                            plugin.messages().component("join-hint-hover")));
            player.sendMessage(hint);
        }, delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.cooldowns().clear(event.getPlayer().getUniqueId());
        plugin.teleports().forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.spawns().handleRespawn(event);
    }
}
