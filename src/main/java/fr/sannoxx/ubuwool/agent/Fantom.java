package fr.sannoxx.ubuwool.agent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
public class Fantom extends Agent {
    @Override public String getName() { return "Fantom"; }
    @Override public String getColor() { return "§b"; }
    @Override public int getUltimateKillsRequired() {
        return super.getUltimateKillsRequired();
    }
    @Override public void applyPassive(Player p) {
        p.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
    }
    @Override public void removePassive(Player p) { p.getInventory().setItemInOffHand(null); }
}