package fr.sannoxx.ubuwool.agent;
import org.bukkit.entity.Player;
public class Doma extends Agent {
    @Override public String getName() { return "Doma"; }
    @Override public String getColor() { return "§3"; }
    @Override public int getUltimateKillsRequired() {
        return super.getUltimateKillsRequired();
    }
    @Override public void applyPassive(Player p) {}
    @Override public void removePassive(Player p) {}
}