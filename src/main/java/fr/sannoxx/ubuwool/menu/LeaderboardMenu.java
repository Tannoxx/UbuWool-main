package fr.sannoxx.ubuwool.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

public class LeaderboardMenu implements InventoryHolder {

    private final Inventory inventory;

    private LeaderboardMenu() {
        this.inventory = org.bukkit.Bukkit.createInventory(this, 9, "§6§lLeaderboard");
    }

    @Override public @NonNull Inventory getInventory() { return inventory; }

    public static void open(Player viewer) {
        ProfileMenu.openLeaderboardPage(viewer);
    }

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}