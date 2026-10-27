package example.commandpanel.config;

/**
 * 插件的全局设置。
 */
public record Settings(
        String mainPanel,
        long clickCooldownMs,
        boolean hideNoPermission,
        boolean joinHint,
        long joinHintDelayTicks,
        boolean teleportEnabled,
        int teleportCooldownSeconds,
        boolean safeLanding,
        boolean crossWorld,
        boolean safeRespawn
) {
}
