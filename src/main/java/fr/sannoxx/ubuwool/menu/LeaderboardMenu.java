package fr.sannoxx.ubuwool.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

/**
 * LeaderboardMenu — désormais délégué au ProfileMenu unifié (page 2).
 * Cette classe est conservée pour compatibilité avec les références existantes
 * (PlayerListener, UwCommand) mais redirige simplement vers ProfileMenu.
 */
public class LeaderboardMenu implements InventoryHolder {

    // Inventaire vide — jamais réellement ouvert (on ouvre ProfileMenu à la place)
    private final Inventory inventory;

    private LeaderboardMenu() {
        this.inventory = org.bukkit.Bukkit.createInventory(this, 9, "§6§lLeaderboard");
    }

    @Override public @NonNull Inventory getInventory() { return inventory; }

    /** Ouvre le leaderboard via le ProfileMenu unifié (page 2). */
    public static void open(Player viewer) {
        ProfileMenu.openLeaderboardPage(viewer);
    }

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}