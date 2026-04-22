package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire d'état centralisé pour toutes les abilities.
 *
 * Remplace les dizaines de Map<String,...> / Map<UUID,...> statiques
 * dispersées dans chaque *Abilities.java.
 *
 * Avantages :
 * - Un seul point de nettoyage (resetAll / resetPlayer)
 * - Aucune fuite d'entités ou de schedulers oubliés
 * - Les noms de joueurs → UUID partout (stables même en cas de changement de pseudo)
 */
public class AbilityStateManager {

    // ---- Entités actives à nettoyer en fin de round ----
    /** Entités spawned par des abilities (fox, zombie, clone...) → owner UUID */
    private static final Map<UUID, UUID> trackedEntities = new ConcurrentHashMap<>();

    // ---- BukkitTasks à annuler en fin de round ----
    private static final Map<UUID, List<Integer>> playerTasks = new ConcurrentHashMap<>();

    // ---- Données génériques UUID → Object (pour les abilities custom) ----
    private static final Map<String, Map<UUID, Object>> namedStates = new ConcurrentHashMap<>();

    // =========================================================
    // Entités
    // =========================================================

    /** Enregistre une entité pour nettoyage automatique. */
    public static void trackEntity(UUID entityUUID, UUID ownerUUID) {
        trackedEntities.put(entityUUID, ownerUUID);
    }

    /** Supprime le suivi d'une entité (elle a été retirée proprement). */
    public static void untrackEntity(UUID entityUUID) {
        trackedEntities.remove(entityUUID);
    }

    /** @return true si cette entité est trackée (appartient à une ability) */
    public static boolean isTracked(UUID entityUUID) {
        return trackedEntities.containsKey(entityUUID);
    }

    /** @return UUID du propriétaire de l'entité, ou null */
    public static UUID getEntityOwner(UUID entityUUID) {
        return trackedEntities.get(entityUUID);
    }

    // =========================================================
    // BukkitTasks
    // =========================================================

    /** Enregistre un task pour un joueur (annulation automatique en fin de round). */
    public static void trackTask(UUID playerUUID, BukkitTask task) {
        playerTasks.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(task.getTaskId());
    }

    /** Annule tous les tasks d'un joueur. */
    public static void cancelPlayerTasks(UUID playerUUID) {
        List<Integer> tasks = playerTasks.remove(playerUUID);
        if (tasks == null) return;
        for (int taskId : tasks) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    // =========================================================
    // États nommés génériques (UUID → valeur)
    // =========================================================

    /**
     * Stocke un état nommé pour un joueur.
     * Ex: AbilityStateManager.setState("horcus.sponge", playerUUID, location)
     */
    public static void setState(String key, UUID playerUUID, Object value) {
        namedStates.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(playerUUID, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getState(String key, UUID playerUUID) {
        Map<UUID, Object> map = namedStates.get(key);
        if (map == null) return null;
        return (T) map.get(playerUUID);
    }

    public static boolean hasState(String key, UUID playerUUID) {
        Map<UUID, Object> map = namedStates.get(key);
        return map != null && map.containsKey(playerUUID);
    }

    public static void removeState(String key, UUID playerUUID) {
        Map<UUID, Object> map = namedStates.get(key);
        if (map != null) map.remove(playerUUID);
    }

    /** @return tous les UUIDs ayant un état pour cette clé */
    public static Set<UUID> getStateOwners(String key) {
        Map<UUID, Object> map = namedStates.get(key);
        return map != null ? map.keySet() : Collections.emptySet();
    }

    // =========================================================
    // Nettoyage global
    // =========================================================

    /** Appelé en fin de round ou reset complet. */
    public static void resetAll() {
        // Supprimer toutes les entités trackées
        for (UUID entityUUID : new ArrayList<>(trackedEntities.keySet())) {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                Entity e = w.getEntity(entityUUID);
                if (e != null && e.isValid()) { e.remove(); break; }
            }
        }
        trackedEntities.clear();

        // Annuler tous les tasks
        for (UUID uuid : new ArrayList<>(playerTasks.keySet())) {
            cancelPlayerTasks(uuid);
        }
        playerTasks.clear();

        // Vider les états
        namedStates.clear();
    }

    /** Nettoyage pour un joueur spécifique (mort, déconnexion). */
    public static void resetPlayer(UUID playerUUID) {
        cancelPlayerTasks(playerUUID);
        // Retirer les entités owned par ce joueur
        trackedEntities.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(playerUUID)) {
                for (org.bukkit.World w : Bukkit.getWorlds()) {
                    Entity e = w.getEntity(entry.getKey());
                    if (e != null && e.isValid()) { e.remove(); break; }
                }
                return true;
            }
            return false;
        });
        // Retirer ses états nommés
        for (Map<UUID, Object> map : namedStates.values()) {
            map.remove(playerUUID);
        }
    }
}