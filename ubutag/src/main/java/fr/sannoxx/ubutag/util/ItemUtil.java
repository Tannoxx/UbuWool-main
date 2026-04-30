package fr.sannoxx.ubutag.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Helpers de création d'items. */
public final class ItemUtil {

    private ItemUtil() {}

    public static NamespacedKey tagKey(Plugin plugin) {
        return new NamespacedKey(plugin, "ubutag_tnt");
    }

    public static ItemStack makeTnt(Plugin plugin) {
        ItemStack tnt = new ItemStack(Material.TNT, 1);
        ItemMeta meta = tnt.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "TNT");
            meta.getPersistentDataContainer().set(tagKey(plugin), PersistentDataType.BYTE, (byte) 1);
            tnt.setItemMeta(meta);
        }
        return tnt;
    }

    public static boolean isTagItem(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.TNT) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(tagKey(plugin), PersistentDataType.BYTE);
        return b != null && b == 1;
    }
}
