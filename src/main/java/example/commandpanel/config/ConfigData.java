package example.commandpanel.config;

import java.util.Map;

public record ConfigData(
        Settings settings,
        Messages messages,
        Map<String, PanelDefinition> panels
) {

    public ConfigData {
        panels = Map.copyOf(panels);
    }
}
