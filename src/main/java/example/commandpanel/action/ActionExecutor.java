package example.commandpanel.action;

import example.commandpanel.CommandPanelPlugin;
import example.commandpanel.config.ActionDefinition;
import example.commandpanel.config.ButtonDefinition;
import example.commandpanel.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 执行按钮上的动作。返回 true 表示已经打开/关闭了界面，调用方不需要刷新面板。
 */
public final class ActionExecutor {

    private final CommandPanelPlugin plugin;

    public ActionExecutor(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(Player player, ButtonDefinition button, String currentPanelId) {
        for (ActionDefinition action : button.actions()) {
            if (!action.permission().isBlank()
                    && !player.hasPermission(action.permission())) {
                plugin.messages().send(player, "no-permission",
                        "permission", action.permission());
                return true;
            }

            switch (action.type()) {
                case GAMERULE -> toggleGameRule(player, action);
                case COMMAND -> runCommand(player, action);
                case OPEN -> {
                    plugin.gui().openPanel(player, action.value(), currentPanelId);
                    return true;
                }
                case TELEPORT_PLAYER -> {
                    plugin.gui().openPlayerList(player, currentPanelId, 0);
                    return true;
                }
                case SET_SPAWN -> plugin.spawns().setSpawn(player);
                case CLEAR_SPAWN -> plugin.spawns().clearSpawn(player);
                case SPAWN -> plugin.spawns().teleportToSpawn(player);
                case WORLD_SPAWN -> plugin.spawns().teleportToWorldSpawn(player);
                case BACK -> plugin.teleports().back(player);
                case CLOSE -> {
                    player.closeInventory();
                    return true;
                }
                case MESSAGE -> player.sendMessage(Text.parse(apply(action.value(), player)));
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void toggleGameRule(Player player, ActionDefinition action) {
        GameRule<?> raw = GameRule.getByName(action.value());
        if (raw == null) {
            plugin.messages().send(player, "gamerule-unknown", "rule", action.value());
            return;
        }
        if (raw.getType() != Boolean.class && raw.getType() != Boolean.TYPE) {
            plugin.messages().send(player, "gamerule-not-boolean", "rule", action.value());
            return;
        }
        GameRule<Boolean> rule = (GameRule<Boolean>) raw;

        List<World> worlds = action.scope().equals("all")
                ? Bukkit.getWorlds()
                : List.of(player.getWorld());

        boolean allOn = true;
        for (World world : worlds) {
            if (!Boolean.TRUE.equals(world.getGameRuleValue(rule))) {
                allOn = false;
                break;
            }
        }
        boolean newValue = !allOn;

        for (World world : worlds) {
            world.setGameRule(rule, newValue);
        }

        String scope = plugin.messages().get(
                action.scope().equals("all") ? "scope-all" : "scope-current");
        plugin.messages().send(player, newValue ? "gamerule-on" : "gamerule-off",
                "rule", rule.getName(), "scope", scope);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        plugin.getLogger().info("玩家 " + player.getName() + " 将规则 "
                + rule.getName() + " 设置为 " + newValue + "（" + scope + "）");
    }

    private void runCommand(Player player, ActionDefinition action) {
        String command = apply(action.value(), player).trim();
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) {
            plugin.messages().send(player, "action-error", "error", "指令为空");
            return;
        }

        boolean success;
        if (action.executor().equals("console")) {
            plugin.getLogger().info("玩家 " + player.getName()
                    + " 通过面板执行控制台指令: /" + command);
            success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            success = player.performCommand(command);
        }

        if (!success) {
            plugin.messages().send(player, "command-failed", "command", command);
        }
    }

    private String apply(String input, Player player) {
        Location location = player.getLocation();
        return input
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName())
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()));
    }
}
