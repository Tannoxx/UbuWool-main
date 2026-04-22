package fr.sannoxx.ubuwool.ability;

import org.bukkit.entity.Player;

public interface AgentAbility {

    boolean useC1(Player player);
    boolean useC2(Player player, Player target);
    boolean useUltimate(Player player);
    long cooldownC1Ms();
    long cooldownC2Ms();
    void resetRound();
    default void cleanupForPlayer(Player player) {}
}