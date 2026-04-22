package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityStateManager {

    private static final Map<UUID, UUID> trackedEntities = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Integer>> playerTasks = new ConcurrentHashMap<>();
    private static final Map<String, Map<UUID, Object>> namedStates = new ConcurrentHashMap<>();
    public static void trackEntity(UUID entityUUID, UUID ownerUUID) {
        trackedEntities.put(entityUUID, ownerUUID);
    }

    public static void untrackEntity(UUID entityUUID) {
        trackedEntities.remove(entityUUID);
    }

    public static boolean isTracked(UUID entityUUID) {
        return trackedEntities.containsKey(entityUUID);
    }

    public static UUID getEntityOwner(UUID entityUUID) {
        return trackedEntities.get(entityUUID);
    }

    public static void trackTask(UUID playerUUID, BukkitTask task) {
        playerTasks.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(task.getTaskId());
    }

    public static void cancelPlayerTasks(UUID playerUUID) {
        List<Integer> tasks = playerTasks.remove(playerUUID);
        if (tasks == null) return;
        for (int taskId : tasks) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

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

    public static Set<UUID> getStateOwners(String key) {
        Map<UUID, Object> map = namedStates.get(key);
        return map != null ? map.keySet() : Collections.emptySet();
    }

    public static void resetAll() {
        for (UUID entityUUID : new ArrayList<>(trackedEntities.keySet())) {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                Entity e = w.getEntity(entityUUID);
                if (e != null && e.isValid()) { e.remove(); break; }
            }
        }
        trackedEntities.clear();

        for (UUID uuid : new ArrayList<>(playerTasks.keySet())) {
            cancelPlayerTasks(uuid);
        }
        playerTasks.clear();

        namedStates.clear();
    }

    public static void resetPlayer(UUID playerUUID) {
        cancelPlayerTasks(playerUUID);
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
        for (Map<UUID, Object> map : namedStates.values()) {
            map.remove(playerUUID);
        }
    }
}