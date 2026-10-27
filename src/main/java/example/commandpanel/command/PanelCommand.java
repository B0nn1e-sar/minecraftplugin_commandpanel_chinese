package example.commandpanel.command;

import example.commandpanel.CommandPanelPlugin;
import example.commandpanel.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /cp、/panel、/caidan、/mianban 的入口。
 */
public final class PanelCommand implements CommandExecutor, TabCompleter {

    private final CommandPanelPlugin plugin;

    public PanelCommand(CommandPanelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            openMain(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "open" -> {
                if (args.length >= 2) {
                    openPanel(sender, args[1]);
                } else {
                    openMain(sender);
                }
            }
            case "reload" -> reload(sender);
            case "help" -> help(sender);
            default -> {
                // 允许 /cp main 这种简写
                if (args.length == 1) {
                    openPanel(sender, args[0]);
                } else {
                    plugin.messages().send(sender, "unknown-subcommand");
                }
            }
        }
        return true;
    }

    private void openMain(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("commandpanel.use")) {
            plugin.messages().send(player, "no-permission", "permission", "commandpanel.use");
            return;
        }
        plugin.gui().openMain(player);
    }

    private void openPanel(CommandSender sender, String panelId) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("commandpanel.use")) {
            plugin.messages().send(player, "no-permission", "permission", "commandpanel.use");
            return;
        }
        plugin.gui().openPanel(player, panelId, null);
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("commandpanel.reload")) {
            plugin.messages().send(sender, "no-permission", "permission", "commandpanel.reload");
            return;
        }
        if (plugin.reloadPluginConfig()) {
            plugin.gui().closeOpenMenus();
            plugin.messages().send(sender, "reloaded",
                    "panels", String.valueOf(plugin.config().panels().size()));
            plugin.getLogger().info(sender.getName() + " 重载了 CommandPanel 配置");
        } else {
            plugin.messages().send(sender, "reload-failed");
        }
    }

    private void help(CommandSender sender) {
        if (!sender.hasPermission("commandpanel.use")) {
            plugin.messages().send(sender, "no-permission", "permission", "commandpanel.use");
            return;
        }
        for (String line : plugin.messages().list("help")) {
            sender.sendMessage(Text.parse(line));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("open", "reload", "help", "main"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return filter(plugin.config().panels().keySet(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(Collection<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
