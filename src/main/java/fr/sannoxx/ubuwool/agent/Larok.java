package fr.sannoxx.ubuwool.agent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
public class Larok extends Agent {
    @Override public String getName() { return "Larok"; }
    @Override public String getColor() { return "§f"; }
    @Override public int getUltimateKillsRequired() { return 5; }
    @Override public void applyPassive(Player p) {
        p.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 7));
    }
    @Override public void removePassive(Player p) {}
}