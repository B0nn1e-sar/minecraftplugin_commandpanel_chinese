package example.commandpanel.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;

/**
 * 标记一个物品栏属于 CommandPanel，并保存该界面的上下文。
 */
public final class MenuHolder implements InventoryHolder {

    public enum Kind {
        PANEL,
        PLAYER_LIST
    }

    private final Kind kind;
    private final String panelId;
    private final String returnPanelId;
    private final int page;
    private final Map<Integer, UUID> targets;
    private final Map<Integer, String> targetNames;
    private Inventory inventory;

    public MenuHolder(Kind kind, String panelId, String returnPanelId,
                      int page, Map<Integer, UUID> targets, Map<Integer, String> targetNames) {
        this.kind = kind;
        this.panelId = panelId;
        this.returnPanelId = returnPanelId;
        this.page = page;
        this.targets = targets;
        this.targetNames = targetNames;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Kind kind() {
        return kind;
    }

    public String panelId() {
        return panelId;
    }

    public String returnPanelId() {
        return returnPanelId;
    }

    public int page() {
        return page;
    }

    public Map<Integer, UUID> targets() {
        return targets;
    }

    public Map<Integer, String> targetNames() {
        return targetNames;
    }
}
