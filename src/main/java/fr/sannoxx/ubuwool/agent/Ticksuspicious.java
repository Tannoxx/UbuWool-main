package fr.sannoxx.ubuwool.agent;
import fr.sannoxx.ubuwool.ability.TicksuspiciousAbilities;
import org.bukkit.entity.Player;
public class Ticksuspicious extends Agent {
    @Override public String getName() { return "Ticksuspicious"; }
    @Override public String getColor() { return "§7"; }
    @Override public int getUltimateKillsRequired() {
        return super.getUltimateKillsRequired();
    }
    @Override public void applyPassive(Player p) { TicksuspiciousAbilities.initHitCounter(p); }
    @Override public void removePassive(Player p) { TicksuspiciousAbilities.clearHitCounter(p); }
}