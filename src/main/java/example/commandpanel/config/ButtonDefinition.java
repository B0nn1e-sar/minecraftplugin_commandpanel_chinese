package example.commandpanel.config;

import org.bukkit.Material;

import java.util.List;

/**
 * 面板里的一个按钮。
 */
public record ButtonDefinition(
        String id,
        int slot,
        Material material,
        Material onMaterial,
        Material offMaterial,
        String name,
        List<String> lore,
        String permission,
        List<ActionDefinition> actions,
        boolean glow,
        int amount,
        Integer customModelData
) {

    public ButtonDefinition {
        lore = List.copyOf(lore);
        actions = List.copyOf(actions);
        permission = permission == null ? "" : permission;
    }

    /**
     * @return 第一个 gamerule 动作；不是开关按钮时返回 null
     */
    public ActionDefinition gameruleAction() {
        for (ActionDefinition action : actions) {
            if (action.type() == ActionDefinition.Type.GAMERULE) {
                return action;
            }
        }
        return null;
    }

    public boolean hasGameruleAction() {
        return gameruleAction() != null;
    }
}
