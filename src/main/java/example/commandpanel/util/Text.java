package example.commandpanel.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * 将配置文件里的 &amp; 颜色代码转换为 Adventure 组件。
 */
public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component parse(String value) {
        return value == null || value.isEmpty() ? Component.empty() : LEGACY.deserialize(value);
    }

    public static List<Component> parse(List<String> values) {
        List<Component> result = new ArrayList<>(values.size());
        for (String value : values) {
            result.add(parse(value));
        }
        return result;
    }
}
