package fr.sannoxx.ubuwool.agent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
public class Sembol extends Agent {
    @Override public String getName() { return "Sembol"; }
    @Override public String getColor() { return "§c"; }
    @Override public int getUltimateKillsRequired() {
        return super.getUltimateKillsRequired();
    }
    @Override public void applyPassive(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
    }
    @Override public void removePassive(Player p) { p.removePotionEffect(PotionEffectType.SPEED); }
}