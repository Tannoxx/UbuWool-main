package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchmakingQueue {

    public static final int MIN_PLAYERS = 2;

    private static final Queue<UUID> queue = new ConcurrentLinkedQueue<>();

    public static boolean enqueue(Player player) {
        if (queue.contains(player.getUniqueId())) {
            player.sendMessage(Lang.get(player, Lang.Key.QUEUE_ALREADY_IN, getPosition(player)));
            return false;
        }
        if (GameRegistry.isInAnyGame(player)) {
            player.sendMessage(Lang.get(player, Lang.Key.QUEUE_ALREADY_IN_GAME));
            return false;
        }
        queue.add(player.getUniqueId());
        int pos = getPosition(player);
        player.sendMessage(Lang.get(player, Lang.Key.QUEUE_JOINED, pos)
                + " " + Lang.get(player, Lang.Key.QUEUE_ADMIN_HINT));
        broadcastQueueSize();
        return true;
    }

    public static boolean dequeue(Player player) {
        boolean removed = queue.remove(player.getUniqueId());
        if (removed) {
            player.sendMessage(Lang.get(player, Lang.Key.QUEUE_LEFT));
            broadcastQueueSize();
        }
        return removed;
    }

    public static boolean isInQueue(Player player) {
        return queue.contains(player.getUniqueId());
    }

    public static int getPosition(Player player) {
        int pos = 0;
        for (UUID id : queue) {
            pos++;
            if (id.equals(player.getUniqueId())) return pos;
        }
        return -1;
    }

    public static int size() { return queue.size(); }

    public static List<Player> drainOnlinePlayers() {
        queue.removeIf(uuid -> org.bukkit.Bukkit.getPlayer(uuid) == null);
        List<Player> players = new ArrayList<>();
        Iterator<UUID> it = queue.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                players.add(p);
                it.remove();
            }
        }
        return players;
    }

    public static List<UUID> peekAll() {
        return new ArrayList<>(queue);
    }

    public static void startChecker() {}
    public static void stopChecker() {}

    public static void reset() {
        queue.clear();
    }

    private static void broadcastQueueSize() {
        int sz = queue.size();
        if (sz == 0) return;
        for (UUID id : queue) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p != null) {
                p.sendMessage(Lang.get(p, Lang.Key.QUEUE_SIZE_BROADCAST, sz));
            }
        }
    }
}