package fr.sannoxx.ubuwool.agent;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Gekko extends Agent {
    @Override public String getName() { return "Gekko"; }
    @Override public String getColor() { return "§a"; }
    @Override public int getUltimateKillsRequired() { return 6; }

    @Override
    public void applyPassive(Player p) {
        ItemStack potion = new ItemStack(Material.LINGERING_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 200, 1, false, true), true);
        meta.setDisplayName("§a§lPogo");
        potion.setItemMeta(meta);
        p.getInventory().addItem(potion);
    }

    @Override public void removePassive(Player p) {}
}
