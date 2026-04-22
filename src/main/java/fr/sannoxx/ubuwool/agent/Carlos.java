package fr.sannoxx.ubuwool.agent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
public class Carlos extends Agent {
    @Override public String getName() { return "Carlos"; }
    @Override public String getColor() { return "§e"; }
    @Override public int getUltimateKillsRequired() { return 6; }
    @Override public void applyPassive(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }
    @Override public void removePassive(Player p) { p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE); }
}