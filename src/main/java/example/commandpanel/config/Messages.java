package example.commandpanel.config;

import example.commandpanel.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * 读取 messages 段，并在缺少键时回退到插件内置的默认配置。
 */
public final class Messages {

    private final YamlConfiguration config;
    private final YamlConfiguration defaults;

    public Messages(YamlConfiguration config, YamlConfiguration defaults) {
        this.config = config;
        this.defaults = defaults;
    }

    public String prefix() {
        return get("prefix");
    }

    public String get(String key) {
        String path = "messages." + key;
        String value = config.getString(path);
        if (value == null) {
            value = defaults.getString(path, path);
        }
        return value == null ? key : value;
    }

    public String format(String key, String... placeholders) {
        String value = get(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            value = value.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return value;
    }

    public List<String> list(String key, String... placeholders) {
        String path = "messages." + key;
        List<String> values;
        if (config.isList(path)) {
            values = config.getStringList(path);
        } else if (config.isString(path)) {
            values = List.of(config.getString(path, ""));
        } else {
            values = defaults.getStringList(path);
        }

        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            String line = value;
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                line = line.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
            }
            result.add(line);
        }
        return result;
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(Text.parse(prefix() + format(key, placeholders)));
    }

    public void sendRaw(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(Text.parse(format(key, placeholders)));
    }

    public Component component(String key, String... placeholders) {
        return Text.parse(format(key, placeholders));
    }
}
