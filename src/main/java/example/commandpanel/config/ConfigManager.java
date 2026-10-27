package example.commandpanel.config;

import example.commandpanel.CommandPanelPlugin;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 负责加载和校验 config.yml。加载失败时不会破坏当前正在使用的配置。
 */
public final class ConfigManager {

    private final CommandPanelPlugin plugin;
    private ConfigData data;

    public ConfigManager(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigData data() {
        return data;
    }

    public boolean load() {
        try {
            File file = new File(plugin.getDataFolder(), "config.yml");
            if (!file.exists()) {
                plugin.saveDefaultConfig();
            }

            YamlConfiguration defaults = readDefaults();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.addDefaults(defaults);

            ConfigData parsed = parse(config, defaults);
            data = parsed;
            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("加载配置失败: " + exception.getMessage());
            plugin.getLogger().severe("请修正 config.yml 后执行 /cp reload；当前仍使用旧配置（如果有）。");
            return false;
        }
    }

    private YamlConfiguration readDefaults() {
        try (InputStream input = plugin.getResource("config.yml")) {
            if (input == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            plugin.getLogger().warning("读取内置默认配置失败: " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    private ConfigData parse(YamlConfiguration config, YamlConfiguration defaults) {
        Settings settings = parseSettings(config);
        Messages messages = new Messages(config, defaults);

        ConfigurationSection panelsSection = config.getConfigurationSection("panels");
        if (panelsSection == null) {
            throw new ConfigException("缺少 panels 段");
        }

        Map<String, PanelDefinition> panels = new LinkedHashMap<>();
        for (String panelId : panelsSection.getKeys(false)) {
            if (!panelId.matches("[a-zA-Z0-9_-]+")) {
                throw new ConfigException("面板 ID 只能包含字母、数字、下划线和短横线: " + panelId);
            }
            ConfigurationSection panelSection = panelsSection.getConfigurationSection(panelId);
            if (panelSection == null) {
                throw new ConfigException("panels." + panelId + " 必须是一个配置段");
            }
            panels.put(panelId, parsePanel(panelId, panelSection));
        }

        if (panels.isEmpty()) {
            throw new ConfigException("至少需要在 panels 中配置一个面板");
        }
        if (!panels.containsKey(settings.mainPanel())) {
            throw new ConfigException("settings.main-panel 指向了不存在的面板: " + settings.mainPanel());
        }

        for (PanelDefinition panel : panels.values()) {
            for (ButtonDefinition button : panel.buttons().values()) {
                for (ActionDefinition action : button.actions()) {
                    if (action.type() == ActionDefinition.Type.OPEN
                            && !panels.containsKey(action.value())) {
                        throw new ConfigException("按钮 " + panel.id() + "." + button.id()
                                + " 打开了不存在的面板: " + action.value());
                    }
                }
            }
        }

        return new ConfigData(settings, messages, panels);
    }

    private Settings parseSettings(YamlConfiguration config) {
        String mainPanel = config.getString("settings.main-panel", "main");
        if (mainPanel == null || mainPanel.isBlank()) {
            mainPanel = "main";
        } else {
            mainPanel = mainPanel.trim();
        }

        long clickCooldown = clamp(config.getLong("settings.click-cooldown-ms", 350L), 0L, 5000L);
        boolean hideNoPermission = config.getBoolean("settings.hide-no-permission", true);
        boolean joinHint = config.getBoolean("settings.join-hint", true);
        long joinHintDelay = clamp(config.getLong("settings.join-hint-delay-ticks", 20L), 0L, 200L);

        boolean teleportEnabled = config.getBoolean("settings.teleport.enabled", true);
        int teleportCooldown = (int) clamp(
                config.getLong("settings.teleport.cooldown-seconds", 3L), 0L, 300L);
        boolean safeLanding = config.getBoolean("settings.teleport.safe-landing", true);
        boolean crossWorld = config.getBoolean("settings.teleport.cross-world", true);
        boolean safeRespawn = config.getBoolean("settings.spawn.safe-respawn", true);

        return new Settings(mainPanel, clickCooldown, hideNoPermission, joinHint, joinHintDelay,
                teleportEnabled, teleportCooldown, safeLanding, crossWorld, safeRespawn);
    }

    private PanelDefinition parsePanel(String panelId, ConfigurationSection section) {
        String title = section.getString("title", "&8" + panelId);
        int rows = section.getInt("rows", 6);
        if (rows < 1 || rows > 6) {
            throw new ConfigException("panels." + panelId + ".rows 必须在 1 到 6 之间");
        }
        int size = rows * 9;

        PanelDefinition.Fill fill = null;
        ConfigurationSection fillSection = section.getConfigurationSection("fill");
        if (fillSection != null) {
            Material material = parseMaterial(
                    fillSection.getString("material"), "panels." + panelId + ".fill.material");
            String name = fillSection.getString("name", " ");
            fill = new PanelDefinition.Fill(material, name == null ? " " : name);
        }

        Map<Integer, ButtonDefinition> buttons = new LinkedHashMap<>();
        ConfigurationSection buttonsSection = section.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String buttonId : buttonsSection.getKeys(false)) {
                ConfigurationSection buttonSection = buttonsSection.getConfigurationSection(buttonId);
                if (buttonSection == null) {
                    throw new ConfigException("panels." + panelId + ".buttons." + buttonId
                            + " 必须是一个配置段");
                }
                ButtonDefinition button = parseButton(panelId, buttonId, buttonSection, size);
                if (buttons.containsKey(button.slot())) {
                    throw new ConfigException("面板 " + panelId + " 的槽位 " + button.slot()
                            + " 被多个按钮占用");
                }
                buttons.put(button.slot(), button);
            }
        }

        return new PanelDefinition(panelId, title == null ? panelId : title, rows, fill, buttons);
    }

    private ButtonDefinition parseButton(String panelId, String buttonId,
                                         ConfigurationSection section, int size) {
        String path = "panels." + panelId + ".buttons." + buttonId;
        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= size) {
            throw new ConfigException(path + ".slot 必须在 0 到 " + (size - 1) + " 之间");
        }

        Material material = parseMaterial(section.getString("material"), path + ".material");
        Material onMaterial = material;
        Material offMaterial = material;
        if (section.isString("on-material")) {
            onMaterial = parseMaterial(section.getString("on-material"), path + ".on-material");
        }
        if (section.isString("off-material")) {
            offMaterial = parseMaterial(section.getString("off-material"), path + ".off-material");
        }

        String name = section.getString("name", "&f" + buttonId);
        List<String> lore;
        if (section.isList("lore")) {
            lore = new ArrayList<>(section.getStringList("lore"));
        } else if (section.isString("lore")) {
            lore = new ArrayList<>(List.of(section.getString("lore", "")));
        } else {
            lore = new ArrayList<>();
        }

        String permission = section.getString("permission", "");
        if (permission == null) {
            permission = "";
        }
        permission = permission.trim();

        List<ActionDefinition> actions = parseActions(section, path);

        boolean glow = section.getBoolean("glow", false);
        int amount = (int) clamp(section.getLong("amount", 1L), 1L, 64L);
        Integer customModelData = section.isInt("custom-model-data")
                ? section.getInt("custom-model-data") : null;

        return new ButtonDefinition(buttonId, slot, material, onMaterial, offMaterial,
                name == null ? "&f" + buttonId : name, lore, permission, actions,
                glow, amount, customModelData);
    }

    private List<ActionDefinition> parseActions(ConfigurationSection section, String path) {
        List<ActionDefinition> actions = new ArrayList<>();
        boolean hasSingle = section.isConfigurationSection("action");
        boolean hasList = section.isList("actions");

        if (hasSingle && hasList) {
            throw new ConfigException(path + " 不能同时配置 action 和 actions");
        }
        if (hasSingle) {
            actions.add(parseAction(section.getConfigurationSection("action"), path + ".action"));
        } else if (hasList) {
            List<Map<?, ?>> maps = section.getMapList("actions");
            for (int i = 0; i < maps.size(); i++) {
                actions.add(parseActionMap(maps.get(i), path + ".actions[" + i + "]"));
            }
        } else if (section.contains("action") || section.contains("actions")) {
            throw new ConfigException(path + " 的 action/actions 格式错误");
        }

        return actions;
    }

    private ActionDefinition parseActionMap(Map<?, ?> map, String path) {
        YamlConfiguration temporary = new YamlConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            temporary.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return parseAction(temporary, path);
    }

    private ActionDefinition parseAction(ConfigurationSection section, String path) {
        if (section == null) {
            throw new ConfigException(path + " 不能为空");
        }
        String typeName = section.getString("type");
        if (typeName == null || typeName.isBlank()) {
            throw new ConfigException(path + ".type 不能为空");
        }

        ActionDefinition.Type type;
        try {
            type = ActionDefinition.Type.valueOf(
                    typeName.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException(path + ".type 不支持: " + typeName);
        }

        String value = "";
        String executor = "player";
        String scope = "current";

        switch (type) {
            case GAMERULE -> {
                value = section.getString("rule", section.getString("gamerule"));
                if (value == null || value.isBlank()) {
                    throw new ConfigException(path + ".rule 不能为空");
                }
                value = value.trim();
                scope = section.getString("scope", "current").toLowerCase(Locale.ROOT);
                if (!scope.equals("current") && !scope.equals("all")) {
                    throw new ConfigException(path + ".scope 只能是 current 或 all");
                }
                GameRule<?> rule = GameRule.getByName(value);
                if (rule == null) {
                    throw new ConfigException(path + ".rule 不存在: " + value);
                }
                if (rule.getType() != Boolean.class && rule.getType() != Boolean.TYPE) {
                    throw new ConfigException(path + ".rule 不是 true/false 类型: " + value);
                }
                value = rule.getName();
            }
            case COMMAND -> {
                value = section.getString("command");
                if (value == null || value.isBlank()) {
                    throw new ConfigException(path + ".command 不能为空");
                }
                executor = section.getString("as", "player").toLowerCase(Locale.ROOT);
                if (!executor.equals("player") && !executor.equals("console")) {
                    throw new ConfigException(path + ".as 只能是 player 或 console");
                }
            }
            case OPEN -> {
                value = section.getString("panel");
                if (value == null || value.isBlank()) {
                    throw new ConfigException(path + ".panel 不能为空");
                }
                value = value.trim();
            }
            case MESSAGE -> {
                value = section.getString("text");
                if (value == null) {
                    throw new ConfigException(path + ".text 不能为空");
                }
            }
            default -> {
                // 这些动作不需要额外参数
            }
        }

        String permission = section.getString("permission", "");
        if (permission == null) {
            permission = "";
        }

        return new ActionDefinition(type, value, executor, scope, permission.trim());
    }

    private Material parseMaterial(String name, String path) {
        if (name == null || name.isBlank()) {
            throw new ConfigException(path + " 不能为空");
        }
        Material material = Material.matchMaterial(name.trim());
        if (material == null || !material.isItem()) {
            throw new ConfigException(path + " 不是有效的物品: " + name);
        }
        return material;
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
