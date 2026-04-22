package fr.sannoxx.ubuwool.agent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
public class Lolita extends Agent {
    @Override public String getName() { return "Lolita"; }
    @Override public String getColor() { return "§0"; }
    @Override public int getUltimateKillsRequired() { return 6; }
    @Override public void applyPassive(Player p) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.HASTE, 200, 1, false, true), true);
        meta.setDisplayName("§d§lPotion");
        potion.setItemMeta(meta);
        p.getInventory().addItem(potion);
    }
    @Override public void removePassive(Player p) {}
}