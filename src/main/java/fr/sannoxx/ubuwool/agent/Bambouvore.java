package fr.sannoxx.ubuwool.agent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
public class Bambouvore extends Agent {
    @Override public String getName() { return "Bambouvore"; }
    @Override public String getColor() { return "§2"; }
    @Override public int getUltimateKillsRequired() { return 6; }
    @Override public void applyPassive(Player p) {
        p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
    }
    @Override public void removePassive(Player p) {}
}