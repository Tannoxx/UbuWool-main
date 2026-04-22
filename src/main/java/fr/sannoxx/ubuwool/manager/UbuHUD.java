package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UbuHUD — refactorisé pour le multi-instance.
 *
 * Chaque instance a sa propre BossBar Map.
 * Les joueurs voient uniquement la bossbar de leur instance.
 *
 * CHANGEMENTS :
 * - init(gm), update(gm, ...), reset(gm) prennent l'instance en paramètre
 * - addLatePlayer(player) cherche l'instance du joueur via GameRegistry
 */
public class UbuHUD {

    /** instanceId → (playerUUID → BossBar) */
    private static final Map<Integer, Map<UUID, BossBar>> instanceBars = new ConcurrentHashMap<>();

    /** Derniers états par instance pour les late-joiners */
    private static final Map<Integer, HudState> lastStates = new ConcurrentHashMap<>();

    private record HudState(int red, int blue, int seconds, Lang.Key key, Object[] args) {}

    private static Component text(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    public static void init(GameManager gm) {
        int id = gm.getInstanceId();
        instanceBars.computeIfAbsent(id, k -> new HashMap<>());
        for (Player p : gm.getAllPlayers()) {
            addPlayer(gm, p);
        }
    }

    public static void addPlayer(GameManager gm, Player p) {
        int id = gm.getInstanceId();
        Map<UUID, BossBar> bars = instanceBars.computeIfAbsent(id, k -> new HashMap<>());
        if (bars.containsKey(p.getUniqueId())) return;
        BossBar bar = BossBar.bossBar(text("§6UBUWOOL #" + id), 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        p.showBossBar(bar);
        bars.put(p.getUniqueId(), bar);
    }

    public static void update(GameManager gm, int scoreRed, int scoreBlue,
                              int seconds, Lang.Key phaseKey, Object... args) {
        int id = gm.getInstanceId();
        lastStates.put(id, new HudState(scoreRed, scoreBlue, seconds, phaseKey, args));

        String timer = String.format("%02d:%02d", seconds / 60, seconds % 60);
        Map<UUID, BossBar> bars = instanceBars.computeIfAbsent(id, k -> new HashMap<>());

        for (Player p : gm.getAllPlayers()) {
            addPlayer(gm, p);
            BossBar bar = bars.get(p.getUniqueId());
            if (bar == null) continue;
            String phase = Lang.get(p, phaseKey, args);
            bar.name(text("§6#" + id + " §c✪ " + scoreRed + " §7| " + phase
                    + " §7| §9" + scoreBlue + " ✪  §7| ⏱ " + timer));
        }
    }

    /**
     * Appelé quand un joueur rejoint une instance en cours (late join).
     * Affiche la bossbar avec le dernier état connu de son instance.
     */
    public static void addLatePlayer(Player p) {
        GameManager gm = GameRegistry.getInstanceOf(p);
        if (gm == null) return;
        int id = gm.getInstanceId();
        HudState state = lastStates.get(id);
        if (state == null) return;

        addPlayer(gm, p);
        Map<UUID, BossBar> bars = instanceBars.get(id);
        if (bars == null) return;
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) return;

        String timer = String.format("%02d:%02d", state.seconds() / 60, state.seconds() % 60);
        String phase = Lang.get(p, state.key(), state.args());
        bar.name(text("§6#" + id + " §c✪ " + state.red() + " §7| " + phase
                + " §7| §9" + state.blue() + " ✪  §7| ⏱ " + timer));
    }

    public static void reset(GameManager gm) {
        int id = gm.getInstanceId();
        Map<UUID, BossBar> bars = instanceBars.remove(id);
        if (bars != null) {
            for (Map.Entry<UUID, BossBar> entry : bars.entrySet()) {
                Player p = UbuWool.getInstance().getServer().getPlayer(entry.getKey());
                if (p != null) p.hideBossBar(entry.getValue());
            }
        }
        lastStates.remove(id);
    }

    /** Reset global (disable du plugin). */
    public static void resetAll() {
        for (int id : instanceBars.keySet()) {
            Map<UUID, BossBar> bars = instanceBars.get(id);
            if (bars == null) continue;
            for (Map.Entry<UUID, BossBar> entry : bars.entrySet()) {
                Player p = UbuWool.getInstance().getServer().getPlayer(entry.getKey());
                if (p != null) p.hideBossBar(entry.getValue());
            }
        }
        instanceBars.clear();
        lastStates.clear();
    }
}