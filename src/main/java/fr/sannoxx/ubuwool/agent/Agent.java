package fr.sannoxx.ubuwool.agent;

import org.bukkit.entity.Player;

public abstract class Agent {
    public abstract String getName();
    public abstract String getColor();
    public abstract void applyPassive(Player player);
    public abstract void removePassive(Player player);
    public int getUltimateKillsRequired() { return 5; }
}