package fr.sannoxx.ubuwool.agent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
public class Asky extends Agent {
    @Override public String getName() { return "Asky"; }
    @Override public String getColor() { return "§d"; }
    @Override public int getUltimateKillsRequired() { return 7; }
    @Override public void applyPassive(Player p) {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        meta.setColor(Color.fromRGB(0x555555));
        meta.setDisplayName("§8Balaclava");
        helmet.setItemMeta(meta);
        p.getInventory().setHelmet(helmet);
    }
    @Override public void removePassive(Player p) {}
}