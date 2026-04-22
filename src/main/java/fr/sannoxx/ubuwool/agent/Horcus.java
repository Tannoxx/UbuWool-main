package fr.sannoxx.ubuwool.agent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
public class Horcus extends Agent {
    @Override public String getName() { return "Horcus"; }
    @Override public String getColor() { return "§4"; }
    @Override public int getUltimateKillsRequired() { return 4; }
    @Override public void applyPassive(Player p) {
        p.getInventory().addItem(new ItemStack(Material.ARROW, 8));
    }
    @Override public void removePassive(Player p) {}
}