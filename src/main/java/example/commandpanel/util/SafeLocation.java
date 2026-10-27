package example.commandpanel.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;

/**
 * 在目标位置附近寻找不会卡住玩家的落脚点。
 */
public final class SafeLocation {

    private static final Set<Material> DANGEROUS = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE
    );

    private SafeLocation() {
    }

    /**
     * 从中心开始按由近到远的顺序寻找安全落点。
     *
     * @param center           目标位置
     * @param horizontalRadius 水平搜索半径
     * @param verticalRadius   垂直搜索半径
     * @return 安全位置；找不到时返回 null
     */
    public static Location find(Location center, int horizontalRadius, int verticalRadius) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        World world = center.getWorld();
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();

        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 0; dy <= verticalRadius; dy++) {
                        int[] offsets = dy == 0 ? new int[]{0} : new int[]{dy, -dy};
                        for (int offset : offsets) {
                            int x = baseX + dx;
                            int y = baseY + offset;
                            int z = baseZ + dz;
                            if (isSafe(world, x, y, z)) {
                                return new Location(world, x + 0.5, y, z + 0.5,
                                        center.getYaw(), center.getPitch());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean isSafe(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            return false;
        }
        if (!world.getWorldBorder().isInside(new Location(world, x, y, z))) {
            return false;
        }

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block floor = world.getBlockAt(x, y - 1, z);

        if (!feet.isPassable() || !head.isPassable()) {
            return false;
        }
        if (feet.isLiquid() || head.isLiquid()) {
            return false;
        }
        if (DANGEROUS.contains(feet.getType()) || DANGEROUS.contains(head.getType())) {
            return false;
        }
        if (floor.isPassable() || floor.isLiquid()) {
            return false;
        }
        return !DANGEROUS.contains(floor.getType());
    }

    /**
     * 重生点的安全检查比传送宽松：允许床和重生锚占据脚部方块，
     * 只拒绝窒息、岩浆、火焰等明显危险的位置。
     */
    public static boolean isRespawnSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            return false;
        }
        if (!world.getWorldBorder().isInside(location)) {
            return false;
        }

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block floor = world.getBlockAt(x, y - 1, z);

        if (!head.isPassable() || head.isLiquid()) {
            return false;
        }
        if (DANGEROUS.contains(feet.getType()) || DANGEROUS.contains(head.getType())) {
            return false;
        }
        if (floor.isPassable() || floor.isLiquid() || DANGEROUS.contains(floor.getType())) {
            return false;
        }
        return feet.isPassable()
                || feet.getType() == Material.RED_BED
                || feet.getType() == Material.RESPAWN_ANCHOR;
    }
}
