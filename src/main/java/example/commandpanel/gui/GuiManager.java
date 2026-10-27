package example.commandpanel.gui;

import example.commandpanel.CommandPanelPlugin;
import example.commandpanel.action.ActionExecutor;
import example.commandpanel.config.ActionDefinition;
import example.commandpanel.config.ButtonDefinition;
import example.commandpanel.config.PanelDefinition;
import example.commandpanel.util.ItemBuilder;
import example.commandpanel.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 负责生成面板物品栏、构建按钮和分发点击事件。
 */
public final class GuiManager {

    private static final int PLAYER_PAGE_SIZE = 45;

    private final CommandPanelPlugin plugin;
    private final ActionExecutor actionExecutor;

    public GuiManager(CommandPanelPlugin plugin, ActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
    }

    public void openMain(Player player) {
        openPanel(player, plugin.settings().mainPanel(), null);
    }

    public void openPanel(Player player, String panelId, String returnPanelId) {
        PanelDefinition panel = plugin.config().panels().get(panelId);
        if (panel == null) {
            plugin.messages().send(player, "panel-not-found", "panel", panelId);
            return;
        }

        MenuHolder holder = new MenuHolder(MenuHolder.Kind.PANEL, panelId, returnPanelId,
                0, Map.of(), Map.of());
        Inventory inventory = Bukkit.createInventory(holder, panel.size(), Text.parse(panel.title()));
        holder.setInventory(inventory);

        if (panel.fill() != null) {
            ItemStack fill = new ItemBuilder(panel.fill().material())
                    .name(panel.fill().name())
                    .build();
            for (int slot = 0; slot < panel.size(); slot++) {
                inventory.setItem(slot, fill);
            }
        }

        for (ButtonDefinition button : panel.buttons().values()) {
            boolean allowed = hasPermission(player, button.permission());
            if (!allowed && plugin.settings().hideNoPermission()) {
                continue;
            }
            inventory.setItem(button.slot(), buildButton(player, button, allowed));
        }

        player.openInventory(inventory);
    }

    public void openPlayerList(Player player, String returnPanelId, int page) {
        if (!plugin.settings().teleportEnabled()) {
            plugin.messages().send(player, "teleport-disabled");
            return;
        }
        if (!player.hasPermission("commandpanel.teleport")) {
            plugin.messages().send(player, "no-permission",
                    "permission", "commandpanel.teleport");
            return;
        }

        List<Player> targets = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (!player.canSee(online)) {
                continue;
            }
            targets.add(online);
        }
        targets.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int pages = Math.max(1, (targets.size() + PLAYER_PAGE_SIZE - 1) / PLAYER_PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, pages - 1));

        MenuHolder holder = new MenuHolder(MenuHolder.Kind.PLAYER_LIST, "",
                returnPanelId, safePage, new HashMap<>(), new HashMap<>());
        String title = plugin.messages().format("player-list-title",
                "page", String.valueOf(safePage + 1),
                "pages", String.valueOf(pages));
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.parse(title));
        holder.setInventory(inventory);

        ItemStack fill = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, fill);
        }

        if (targets.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.PAPER)
                    .name(plugin.messages().get("player-list-empty"))
                    .build());
        } else {
            int start = safePage * PLAYER_PAGE_SIZE;
            int end = Math.min(start + PLAYER_PAGE_SIZE, targets.size());
            for (int index = start; index < end; index++) {
                Player target = targets.get(index);
                int slot = index - start;
                inventory.setItem(slot, buildPlayerHead(player, target));
                holder.targets().put(slot, target.getUniqueId());
                holder.targetNames().put(slot, target.getName());
            }
        }

        if (safePage > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                    .name(plugin.messages().get("player-list-prev"))
                    .build());
        }
        if (safePage < pages - 1) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW)
                    .name(plugin.messages().get("player-list-next"))
                    .build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name(plugin.messages().get("player-list-close"))
                .build());

        player.openInventory(inventory);
    }

    private ItemStack buildPlayerHead(Player viewer, Player target) {
        String distance;
        if (viewer.getWorld().equals(target.getWorld())) {
            distance = Math.round(viewer.getLocation().distance(target.getLocation())) + " 格";
        } else {
            distance = plugin.messages().get("different-world");
        }

        List<String> lore = plugin.messages().list("player-list-item-lore",
                "player", target.getName(),
                "world", target.getWorld().getName(),
                "distance", distance);
        String name = plugin.messages().format("player-list-item-name", "player", target.getName());

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skull(target)
                .name(name)
                .lore(lore)
                .build();
    }

    private ItemStack buildButton(Player player, ButtonDefinition button, boolean allowed) {
        ActionDefinition ruleAction = button.gameruleAction();
        RuleState state = ruleAction == null ? null : ruleState(player, ruleAction);

        Material material = button.material();
        if (state == RuleState.ON) {
            material = button.onMaterial();
        } else if (state == RuleState.OFF) {
            material = button.offMaterial();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholders.put("teleport-cooldown",
                String.valueOf(plugin.settings().teleportCooldownSeconds()));
        placeholders.put("rule", ruleAction == null ? "" : ruleAction.value());
        placeholders.put("scope", ruleAction == null ? "" : plugin.messages().get(
                ruleAction.scope().equals("all") ? "scope-all" : "scope-current"));
        placeholders.put("state", stateText(state));

        String name = apply(button.name(), placeholders);
        List<String> lore = new ArrayList<>();
        for (String line : button.lore()) {
            lore.add(apply(line, placeholders));
        }
        if (!allowed) {
            lore.add(plugin.messages().get("no-permission-lore"));
        }

        return new ItemBuilder(material)
                .amount(button.amount())
                .name(name)
                .lore(lore)
                .glow(button.glow())
                .customModelData(button.customModelData())
                .build();
    }

    private String stateText(RuleState state) {
        if (state == null) {
            return "";
        }
        return switch (state) {
            case ON -> plugin.messages().get("state-on");
            case OFF -> plugin.messages().get("state-off");
            case MIXED -> plugin.messages().get("state-mixed");
        };
    }

    @SuppressWarnings("unchecked")
    private RuleState ruleState(Player player, ActionDefinition action) {
        GameRule<?> raw = GameRule.getByName(action.value());
        if (raw == null || (raw.getType() != Boolean.class && raw.getType() != Boolean.TYPE)) {
            return null;
        }
        GameRule<Boolean> rule = (GameRule<Boolean>) raw;

        if (action.scope().equals("all")) {
            boolean anyOn = false;
            boolean anyOff = false;
            for (World world : Bukkit.getWorlds()) {
                if (Boolean.TRUE.equals(world.getGameRuleValue(rule))) {
                    anyOn = true;
                } else {
                    anyOff = true;
                }
            }
            if (anyOn && anyOff) {
                return RuleState.MIXED;
            }
            return anyOn ? RuleState.ON : RuleState.OFF;
        }

        return Boolean.TRUE.equals(player.getWorld().getGameRuleValue(rule))
                ? RuleState.ON : RuleState.OFF;
    }

    private String apply(String input, Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    public void handleClick(Player player, MenuHolder holder, int slot) {
        if (holder.kind() == MenuHolder.Kind.PLAYER_LIST) {
            handlePlayerListClick(player, holder, slot);
            return;
        }

        PanelDefinition panel = plugin.config().panels().get(holder.panelId());
        if (panel == null) {
            player.closeInventory();
            plugin.messages().send(player, "panel-not-found", "panel", holder.panelId());
            return;
        }

        ButtonDefinition button = panel.buttons().get(slot);
        if (button == null) {
            return;
        }
        if (!hasPermission(player, button.permission())) {
            plugin.messages().send(player, "no-permission",
                    "permission", button.permission());
            return;
        }
        if (!plugin.cooldowns().allowClick(player)) {
            return;
        }

        boolean openedGui = actionExecutor.execute(player, button, holder.panelId());
        if (!openedGui && button.hasGameruleAction()) {
            refreshLater(player, holder);
        }
    }

    private void handlePlayerListClick(Player player, MenuHolder holder, int slot) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            if (holder.page() > 0) {
                openPlayerList(player, holder.returnPanelId(), holder.page() - 1);
            }
            return;
        }
        if (slot == 53) {
            openPlayerList(player, holder.returnPanelId(), holder.page() + 1);
            return;
        }

        UUID targetId = holder.targets().get(slot);
        if (targetId == null) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            plugin.messages().send(player, "teleport-target-offline",
                    "target", holder.targetNames().getOrDefault(slot, "?"));
            openPlayerList(player, holder.returnPanelId(), holder.page());
            return;
        }
        if (!player.hasPermission("commandpanel.teleport")) {
            plugin.messages().send(player, "no-permission",
                    "permission", "commandpanel.teleport");
            return;
        }
        plugin.teleports().teleportToPlayer(player, target);
    }

    private void refreshLater(Player player, MenuHolder holder) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder current)) {
                return;
            }
            if (current.kind() != MenuHolder.Kind.PANEL
                    || !current.panelId().equals(holder.panelId())) {
                return;
            }
            openPanel(player, holder.panelId(), holder.returnPanelId());
        });
    }

    public void closeOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                player.closeInventory();
            }
        }
    }

    private enum RuleState {
        ON,
        OFF,
        MIXED
    }
}
