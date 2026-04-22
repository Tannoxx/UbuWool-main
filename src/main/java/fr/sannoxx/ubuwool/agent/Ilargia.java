package fr.sannoxx.ubuwool.agent;
import org.bukkit.entity.Player;
public class Ilargia extends Agent {
    @Override public String getName() { return "Ilargia"; }
    @Override public String getColor() { return "§8"; }
    @Override public int getUltimateKillsRequired() { return 4; }
    @Override public void applyPassive(Player p) {}
    @Override public void removePassive(Player p) {}
}