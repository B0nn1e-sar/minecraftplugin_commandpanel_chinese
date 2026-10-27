package example.commandpanel.config;

/**
 * 按钮点击后执行的一个动作。
 */
public record ActionDefinition(
        Type type,
        String value,
        String executor,
        String scope,
        String permission
) {

    public enum Type {
        GAMERULE,
        COMMAND,
        OPEN,
        TELEPORT_PLAYER,
        SET_SPAWN,
        CLEAR_SPAWN,
        SPAWN,
        WORLD_SPAWN,
        BACK,
        CLOSE,
        MESSAGE
    }

    public ActionDefinition {
        value = value == null ? "" : value;
        executor = executor == null ? "player" : executor;
        scope = scope == null ? "current" : scope;
        permission = permission == null ? "" : permission;
    }

    public boolean opensGui() {
        return type == Type.OPEN || type == Type.TELEPORT_PLAYER || type == Type.CLOSE;
    }
}
