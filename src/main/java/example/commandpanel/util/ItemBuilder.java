package example.commandpanel.util;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * 简单物品构建器，统一处理名称、Lore、发光和玩家头颅。
 */
public final class ItemBuilder {

    private final ItemStack item;
    private String name;
    private List<String> lore = List.of();
    private boolean glow;
    private Integer customModelData;
    private OfflinePlayer skullOwner;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        this.lore = lore == null ? List.of() : lore;
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public ItemBuilder customModelData(Integer customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    public ItemBuilder skull(OfflinePlayer owner) {
        this.skullOwner = owner;
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (name != null) {
            meta.displayName(Text.parse(name));
        }
        if (!lore.isEmpty()) {
            meta.lore(Text.parse(lore));
        }
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        if (skullOwner != null && meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(skullOwner);
        }
        item.setItemMeta(meta);
        return item;
    }
}
