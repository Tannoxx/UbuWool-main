package fr.sannoxx.ubuwool.ability;

import org.bukkit.entity.Player;

/**
 * Contrat commun pour toutes les classes d'abilities d'agent.
 *
 * Chaque agent implémente cette interface pour :
 * - Garantir que resetRound() est présent (impossible à oublier)
 * - Permettre un dispatch polymorphique dans AbilityDispatcher (élimine le switch géant)
 * - Faciliter l'ajout de nouveaux agents sans modifier PlayerListener
 */
public interface AgentAbility {

    /**
     * Utilise la capacité C1 du joueur.
     * @param player le joueur qui utilise la capacité
     * @return true si la capacité a été utilisée (le cooldown doit être appliqué),
     *         false si la capacité a échoué (pas de cooldown)
     */
    boolean useC1(Player player);

    /**
     * Utilise la capacité C2 du joueur.
     * Pour Lolita C2 (morsure), target est non-null.
     * @param player  le joueur qui utilise la capacité
     * @param target  la cible (peut être null si C2 ne nécessite pas de cible directe)
     * @return true si la capacité a été utilisée (cooldown à appliquer)
     */
    boolean useC2(Player player, Player target);

    /**
     * Utilise l'ultimate du joueur.
     * @param player le joueur qui utilise l'ultimate
     * @return true si l'ultimate a été utilisé
     */
    boolean useUltimate(Player player);

    /**
     * Durée du cooldown C1 en millisecondes.
     */
    long cooldownC1Ms();

    /**
     * Durée du cooldown C2 en millisecondes.
     */
    long cooldownC2Ms();

    /**
     * Nettoyage des états persistants de cette ability pour ce joueur au début/fin de round.
     * Appelé systématiquement dans GameManager.cleanupRoundBlocks() et reset().
     */
    void resetRound();

    /**
     * Nettoyage immédiat pour un joueur spécifique (mort, déconnexion).
     * Implémentation par défaut : ne fait rien (override si nécessaire).
     */
    default void cleanupForPlayer(Player player) {}
}