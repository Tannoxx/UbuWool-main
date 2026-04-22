package fr.sannoxx.ubuwool.agent;
import fr.sannoxx.ubuwool.ability.MascordAbilities;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
public class Mascord extends Agent {
    @Override public String getName() { return "Mascord"; }
    @Override public String getColor() { return "§9"; }
    @Override public int getUltimateKillsRequired() { return 6; }
    @Override public void applyPassive(Player p) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName("§9Boussole ?");
        compass.setItemMeta(meta);
        p.getInventory().setItemInOffHand(compass);
        MascordAbilities.startCompassTracker(p);
    }
    @Override public void removePassive(Player p) {
        MascordAbilities.stopCompassTracker(p);
        p.getInventory().setItemInOffHand(null);
    }
}