package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameRegistry {

    public static final int MAX_INSTANCES = 3;

    private static final Map<Integer, GameManager> instances = new LinkedHashMap<>();
    private static int nextId = 1;

    public static GameManager createInstance() {
        if (instances.size() >= MAX_INSTANCES) return null;
        int id = nextId++;
        GameManager gm = new GameManager(id);
        instances.put(id, gm);
        UbuWool.getInstance().getLogger()
                .info("[GameRegistry] Instance #" + id + " créée (" + instances.size() + "/" + MAX_INSTANCES + ")");
        return gm;
    }

    public static void removeInstance(int id) {
        GameManager gm = instances.remove(id);
        if (gm != null) {
            UbuWool.getInstance().getLogger()
                    .info("[GameRegistry] Instance #" + id + " supprimée.");
        }
    }

    public static GameManager getInstance(int id) {
        return instances.get(id);
    }

    public static Collection<GameManager> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public static GameManager getInstanceOf(Player player) {
        for (GameManager gm : instances.values()) {
            if (gm.isPlayerInGame(player)) return gm;
        }
        return null;
    }

    public static boolean isInAnyGame(Player player) {
        return getInstanceOf(player) != null;
    }

    public static boolean hasWaitingInstance() {
        for (GameManager gm : instances.values()) {
            if (gm.state == GameManager.GameState.WAITING) return true;
        }
        return false;
    }

    public static GameManager getOrCreateWaitingInstance() {
        for (GameManager gm : instances.values()) {
            if (gm.state == GameManager.GameState.WAITING) return gm;
        }
        return createInstance();
    }

    public static int count() {
        return instances.size();
    }

    public static void resetAll() {
        for (GameManager gm : new ArrayList<>(instances.values())) {
            try { gm.reset(); } catch (Exception ignored) {}
        }
        instances.clear();
        nextId = 1;
    }

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