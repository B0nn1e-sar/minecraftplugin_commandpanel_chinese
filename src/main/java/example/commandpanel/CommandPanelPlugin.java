package example.commandpanel;

import example.commandpanel.action.ActionExecutor;
import example.commandpanel.command.PanelCommand;
import example.commandpanel.config.ConfigData;
import example.commandpanel.config.ConfigManager;
import example.commandpanel.config.Messages;
import example.commandpanel.config.Settings;
import example.commandpanel.gui.GuiManager;
import example.commandpanel.listener.PanelListener;
import example.commandpanel.service.CooldownService;
import example.commandpanel.service.SpawnService;
import example.commandpanel.service.TeleportService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandPanelPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private GuiManager guiManager;
    private ActionExecutor actionExecutor;
    private SpawnService spawnService;
    private TeleportService teleportService;
    private CooldownService cooldownService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        if (!configManager.load()) {
            getLogger().severe("CommandPanel 配置加载失败，插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        cooldownService = new CooldownService(this);
        spawnService = new SpawnService(this);
        teleportService = new TeleportService(this);
        actionExecutor = new ActionExecutor(this);
        guiManager = new GuiManager(this, actionExecutor);

        PluginCommand command = getCommand("cp");
        if (command == null) {
            getLogger().severe("无法注册 /cp 指令，请检查 plugin.yml。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        PanelCommand handler = new PanelCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getServer().getPluginManager().registerEvents(new PanelListener(this), this);
        getLogger().info("CommandPanel 已启用，主面板: "
                + config().settings().mainPanel());
    }

    @Override
    public void onDisable() {
        if (guiManager != null) {
            try {
                guiManager.closeOpenMenus();
            } catch (RuntimeException exception) {
                getLogger().warning("关闭面板时出错: " + exception.getMessage());
            }
        }
    }

    public boolean reloadPluginConfig() {
        return configManager.load();
    }

    public ConfigData config() {
        return configManager.data();
    }

    public Settings settings() {
        return config().settings();
    }

    public Messages messages() {
        return config().messages();
    }

    public GuiManager gui() {
        return guiManager;
    }

    public SpawnService spawns() {
        return spawnService;
    }

    public TeleportService teleports() {
        return teleportService;
    }

    public CooldownService cooldowns() {
        return cooldownService;
    }
}
