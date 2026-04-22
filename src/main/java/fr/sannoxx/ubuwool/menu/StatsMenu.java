package fr.sannoxx.ubuwool.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class StatsMenu implements InventoryHolder {

    private final Inventory inventory;

    private StatsMenu() {
        this.inventory = org.bukkit.Bukkit.createInventory(this, 9, "Stats");
    }

    @Override public @NonNull Inventory getInventory() { return inventory; }

    public static void open(Player viewer, Player target) {
        ProfileMenu.openStatsPage(viewer, target);
    }

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!(event.getInventory().getHolder() instanceof StatsMenu)) return;
        if (event.getRawSlot() == 49) LeaderboardMenu.open(viewer);
    }

    static ItemStack item(Material mat, String name, List<String> lore) {
        return ProfileMenu.item(mat, name, lore);
    }
}