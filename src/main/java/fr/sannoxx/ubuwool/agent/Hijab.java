package fr.sannoxx.ubuwool.agent;
import org.bukkit.entity.Player;
public class Hijab extends Agent {
    @Override public String getName() { return "Hijab"; }
    @Override public String getColor() { return "§5"; }
    @Override public int getUltimateKillsRequired() { return 7; }
    @Override public void applyPassive(Player p) {}
    @Override public void removePassive(Player p) {}
}