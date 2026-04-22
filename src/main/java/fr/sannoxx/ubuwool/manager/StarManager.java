package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.PlayerData;
import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StarManager {

    private static final Map<Integer, InstanceStarState> states = new ConcurrentHashMap<>();

    private static class InstanceStarState {
        final Map<UUID, Integer> standingTicks = new HashMap<>();
        final Map<UUID, Double>  lastHealth    = new HashMap<>();
        final List<Location>     activeStars   = new ArrayList<>();
        int checkerTask = -1;
    }

    private static InstanceStarState getOrCreate(int instanceId) {
        return states.computeIfAbsent(instanceId, k -> new InstanceStarState());
    }

    public static void startRound(GameManager gm) {
        int id = gm.getInstanceId();
        InstanceStarState state = new InstanceStarState(); // reset complet
        states.put(id, state);

        MapConfig.UbuMap map = MapConfig.getSelectedMap();
        if (map != null && map.starPositions != null) {
            org.bukkit.World world = Bukkit.getWorlds().getFirst();
            for (int[] pos : map.starPositions)
                state.activeStars.add(new Location(world, pos[0], pos[1], pos[2]));
        }
        startChecker(gm, state);
    }

    public static void stopRound(GameManager gm) {
        int id = gm.getInstanceId();
        InstanceStarState state = states.get(id);
        if (state == null) return;
        if (state.checkerTask != -1) {
            Bukkit.getScheduler().cancelTask(state.checkerTask);
            state.checkerTask = -1;
        }
        state.standingTicks.clear();
        state.lastHealth.clear();
    }

    public static void reset(GameManager gm) {
        int id = gm.getInstanceId();
        InstanceStarState state = states.remove(id);
        if (state == null) return;
        if (state.checkerTask != -1) {
            Bukkit.getScheduler().cancelTask(state.checkerTask);
        }
    }

    public static void resetAll() {
        for (InstanceStarState state : states.values()) {
            if (state.checkerTask != -1) Bukkit.getScheduler().cancelTask(state.checkerTask);
        }
        states.clear();
    }

    private static void startChecker(GameManager gm, InstanceStarState state) {
        if (state.checkerTask != -1) Bukkit.getScheduler().cancelTask(state.checkerTask);

        state.checkerTask = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), () -> {
                    if (gm.state != GameManager.GameState.ROUND_ACTIVE) return;
                    org.bukkit.World world = Bukkit.getWorlds().getFirst();

                    for (Location star : new ArrayList<>(state.activeStars)) {
                        world.spawnParticle(Particle.END_ROD,    star.clone().add(0.5, 0.5, 0.5), 4, 0.4, 0.4, 0.4, 0.03);
                        world.spawnParticle(Particle.FIREWORK, star.clone().add(0.5, 0.5, 0.5), 2, 0.3, 0.3, 0.3, 0.01);
                    }

                    for (Player player : gm.getAllPlayers()) {
                        if (gm.deadPlayers.contains(player.getUniqueId())) {
                            state.standingTicks.remove(player.getUniqueId());
                            state.lastHealth.remove(player.getUniqueId());
                            continue;
                        }
                        double currentHealth = player.getHealth();
                        double prevHealth = state.lastHealth.getOrDefault(player.getUniqueId(), currentHealth);
                        boolean tookDamage = currentHealth < prevHealth;
                        state.lastHealth.put(player.getUniqueId(), currentHealth);

                        Location nearStar = getNearStar(player, state);
                        if (nearStar != null && !tookDamage) {
                            int ticks = state.standingTicks.getOrDefault(player.getUniqueId(), 0) + 1;
                            state.standingTicks.put(player.getUniqueId(), ticks);
                            if (ticks % 5 == 0) {
                                int secondsLeft = 5 - (ticks / 5);
                                if (secondsLeft > 0)
                                    player.sendMessage(Lang.get(player, Lang.Key.STAR_CAPTURE_PROGRESS, secondsLeft));
                                world.spawnParticle(Particle.HAPPY_VILLAGER,
                                        player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.1);
                            }
                            if (ticks >= 25) {
                                state.standingTicks.remove(player.getUniqueId());
                                captureStar(player, gm, nearStar, state);
                            }
                        } else if (nearStar != null) {
                            if (state.standingTicks.containsKey(player.getUniqueId())) {
                                state.standingTicks.remove(player.getUniqueId());
                                player.sendMessage(Lang.get(player, Lang.Key.STAR_CAPTURE_INTERRUPTED));
                            }
                        } else {
                            state.standingTicks.remove(player.getUniqueId());
                        }
                    }
                }, 4L, 4L).getTaskId();
    }

    private static Location getNearStar(Player player, InstanceStarState state) {
        Location pos = player.getLocation();
        for (Location star : state.activeStars) {
            Location starCenter = star.clone().add(0.5, 0.5, 0.5);
            if (Math.abs(pos.getX() - starCenter.getX()) < 1.5
                    && Math.abs(pos.getY() - starCenter.getY()) < 2.0
                    && Math.abs(pos.getZ() - starCenter.getZ()) < 1.5)
                return star;
        }
        return null;
    }

    private static void captureStar(Player player, GameManager gm,
                                    Location starPos, InstanceStarState state) {
        state.activeStars.remove(starPos);

        PlayerData data = gm.playerDataMap.get(player.getUniqueId());
        if (data != null && !data.isUltimateReady()) {
            data.ultimateKills++;
            gm.updateUltimateItem(player, data);
            TabManager.updateTab(gm);
        }

        Location center = starPos.clone().add(0.5, 1, 0.5);
        starPos.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 30, 0.5, 0.5, 0.5, 0.3);
        String teamColor = gm.isRedTeam(player) ? "§c" : "§9";
        String colored = teamColor + player.getName();
        for (Player p : gm.getAllPlayers())
            p.sendMessage(Lang.get(p, Lang.Key.STAR_CAPTURED, colored));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
        gm.getAllPlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f));
    }
}