package fr.sannoxx.ubuwool.agent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
public class Gargamel extends Agent {
    @Override public String getName() { return "Gargamel"; }
    @Override public String getColor() { return "§6"; }
    @Override public int getUltimateKillsRequired() {
        return super.getUltimateKillsRequired();
    }
    @Override public void applyPassive(Player p) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0), true);
        potion.setItemMeta(meta);
        p.getInventory().addItem(potion);
    }
    @Override public void removePassive(Player p) {}
}