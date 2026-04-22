package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameRegistry — gestionnaire central de toutes les instances de jeu.
 *
 * Remplace le singleton GameManager.get().
 * Chaque instance est identifiée par un entier (1, 2, 3…).
 *
 * Usage :
 *   GameRegistry.createInstance()       → crée une nouvelle instance (max 3)
 *   GameRegistry.getInstance(id)        → récupère une instance par ID
 *   GameRegistry.getInstanceOf(player)  → instance du joueur, ou null
 *   GameRegistry.removeInstance(id)     → supprime une instance terminée
 */
public class GameRegistry {

    public static final int MAX_INSTANCES = 3;

    /** id → instance */
    private static final Map<Integer, GameManager> instances = new LinkedHashMap<>();
    private static int nextId = 1;

    // =========================================================
    // Création / Suppression
    // =========================================================

    /**
     * Crée une nouvelle instance si le maximum n'est pas atteint.
     * @return l'instance créée, ou null si max atteint
     */
    public static GameManager createInstance() {
        if (instances.size() >= MAX_INSTANCES) return null;
        int id = nextId++;
        GameManager gm = new GameManager(id);
        instances.put(id, gm);
        UbuWool.getInstance().getLogger()
                .info("[GameRegistry] Instance #" + id + " créée (" + instances.size() + "/" + MAX_INSTANCES + ")");
        return gm;
    }

    /** Supprime une instance (appelé en fin de partie ou /uw stop). */
    public static void removeInstance(int id) {
        GameManager gm = instances.remove(id);
        if (gm != null) {
            UbuWool.getInstance().getLogger()
                    .info("[GameRegistry] Instance #" + id + " supprimée.");
        }
    }

    // =========================================================
    // Accès
    // =========================================================

    /** @return l'instance avec cet id, ou null */
    public static GameManager getInstance(int id) {
        return instances.get(id);
    }

    /** @return toutes les instances actives */
    public static Collection<GameManager> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    /** @return l'instance à laquelle appartient le joueur, ou null */
    public static GameManager getInstanceOf(Player player) {
        for (GameManager gm : instances.values()) {
            if (gm.isPlayerInGame(player)) return gm;
        }
        return null;
    }

    /** @return true si le joueur est dans une instance quelconque */
    public static boolean isInAnyGame(Player player) {
        return getInstanceOf(player) != null;
    }

    /** @return true si une instance est en attente de joueurs (WAITING) */
    public static boolean hasWaitingInstance() {
        for (GameManager gm : instances.values()) {
            if (gm.state == GameManager.GameState.WAITING) return true;
        }
        return false;
    }

    /**
     * Retourne une instance en état WAITING, ou en crée une nouvelle si possible.
     * Utilisé par la file d'attente de matchmaking.
     */
    public static GameManager getOrCreateWaitingInstance() {
        // Chercher une instance existante en WAITING
        for (GameManager gm : instances.values()) {
            if (gm.state == GameManager.GameState.WAITING) return gm;
        }
        // Sinon en créer une nouvelle
        return createInstance();
    }

    /** Nombre d'instances actives */
    public static int count() {
        return instances.size();
    }

    /** Réinitialise tout (utilisé au disable du plugin) */
    public static void resetAll() {
        for (GameManager gm : new ArrayList<>(instances.values())) {
            try { gm.reset(); } catch (Exception ignored) {}
        }
        instances.clear();
        nextId = 1;
    }

    // =========================================================
    // Affichage
    // =========================================================

    /**
     * Retourne un résumé de l'état de toutes les instances.
     * Utilisé par /uw admin instances.
     */
    public static List<String> getStatusLines() {
        List<String> lines = new ArrayList<>();
        if (instances.isEmpty()) {
            lines.add("§7Aucune instance active.");
            return lines;
        }
        for (Map.Entry<Integer, GameManager> entry : instances.entrySet()) {
            GameManager gm = entry.getValue();
            int players = gm.getAllPlayers().size();
            String map = gm.getActiveMapName() != null ? gm.getActiveMapName() : "§7aucune";
            lines.add(String.format("§6Instance #%d §7| Phase: §f%s §7| Joueurs: §e%d §7| Map: §f%s",
                    entry.getKey(), gm.state.name(), players, map));
        }
        return lines;
    }
}