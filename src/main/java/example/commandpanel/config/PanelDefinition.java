package example.commandpanel.config;

import org.bukkit.Material;

import java.util.Map;

/**
 * 一个可打开的面板。
 */
public record PanelDefinition(
        String id,
        String title,
        int rows,
        Fill fill,
        Map<Integer, ButtonDefinition> buttons
) {

    public PanelDefinition {
        buttons = Map.copyOf(buttons);
    }

    public int size() {
        return rows * 9;
    }

    public record Fill(Material material, String name) {
    }
}
