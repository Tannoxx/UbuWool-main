package fr.sannoxx.ubutag.arena;

import fr.sannoxx.ubutag.Messages;
import fr.sannoxx.ubutag.UbuTag;
import fr.sannoxx.ubutag.stats.PlayerStats;
import fr.sannoxx.ubutag.util.ItemUtil;
import fr.sannoxx.ubutag.util.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Une instance d'arène TNT Tag : elle contient à la fois la configuration
 * (lobby, spawns, durées) et l'état de jeu en cours.
 *
 * Le jeu se déroule en plusieurs phases :
 *  - WAITING  : phase de lobby, en attente de joueurs.
 *  - STARTING : countdown avant le début, peut être annulé si on retombe sous le minimum.
 *  - IN_GAME  : rounds successifs ; à chaque fin de round, les porteurs de TNT explosent.
 *  - ENDING   : courte phase de fin avant le retour au lobby principal.
 */
public class Arena {

    private final UbuTag plugin;

    /* ─── Configuration ─── */
    public final String name;
    private Location lobby;
    private final List<Location> spawns = new ArrayList<>();
    public int minPlayers;
    public int maxPlayers;
    public int lobbyCountdownSeconds;
    public int shortCountdownSeconds;
    public int roundDurationSeconds;
    public int roundsPerGame;
    public double initialTaggedRatio;
    public int speedAmp;
    public int jumpAmp;
    public boolean blockDamage;
    public boolean enabled = true;

    /* ─── Runtime ─── */
    private ArenaState state = ArenaState.WAITING;
    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> tagged = new HashSet<>();
    /** UUID -> uuid du dernier joueur qui lui a passé la TNT (pour attribuer le kill). */
    private final Map<UUID, UUID> lastTagger = new HashMap<>();
    /** UUID -> ms jusqu'auquel ce joueur ne peut pas recevoir la TNT (anti repasse instant). */
    private final Map<UUID, Long> tagCooldown = new HashMap<>();
    /** UUID -> moment auquel ce joueur a rejoint la partie courante (pour le temps de survie). */
    private final Map<UUID, Long> joinedAtMs = new HashMap<>();

    private int countdown;
    private int roundIndex;
    private int roundSecondsLeft;
    private GameTask task;

    /** Sidebar par joueur (créée à la volée par renderSidebars). */
    private final Map<UUID, Sidebar> sidebars = new HashMap<>();
    /** Mémorise le gagnant entre endGame() et resetToLobby() pour le scoreboard. */
    private String lastWinnerName;

    public Arena(UbuTag plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        // valeurs par défaut depuis config.yml
        this.minPlayers = plugin.getConfig().getInt("defaults.min-players", 2);
        this.maxPlayers = plugin.getConfig().getInt("defaults.max-players", 16);
        this.lobbyCountdownSeconds = plugin.getConfig().getInt("defaults.lobby-countdown-seconds", 30);
        this.shortCountdownSeconds = plugin.getConfig().getInt("defaults.short-countdown-seconds", 10);
        this.roundDurationSeconds = plugin.getConfig().getInt("defaults.round-duration-seconds", 30);
        this.roundsPerGame = plugin.getConfig().getInt("defaults.rounds-per-game", 0);
        this.initialTaggedRatio = plugin.getConfig().getDouble("defaults.initial-tagged-ratio", 0.25);
        this.speedAmp = plugin.getConfig().getInt("defaults.speed-effect-amplifier", 1);
        this.jumpAmp = plugin.getConfig().getInt("defaults.jump-effect-amplifier", 1);
        this.blockDamage = plugin.getConfig().getBoolean("defaults.block-damage", false);
    }

    /* ─── Accessors / mutateurs config ─── */
    public Location getLobby()         { return lobby == null ? null : lobby.clone(); }
    public void setLobby(Location l)   { this.lobby = l == null ? null : l.clone(); }
    public List<Location> getSpawns()  { return Collections.unmodifiableList(spawns); }
    public void addSpawn(Location l)   { spawns.add(l.clone()); }
    public void clearSpawns()          { spawns.clear(); }
    public ArenaState getState()       { return state; }
    public int getRound()              { return roundIndex; }
    public int getRoundSecondsLeft()   { return roundSecondsLeft; }
    public int getCountdown()          { return countdown; }

    public boolean isReady() {
        return enabled && lobby != null && spawns.size() >= 2;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean isInGame() {
        return state == ArenaState.IN_GAME;
    }

    public Set<UUID> getPlayers() { return Collections.unmodifiableSet(new HashSet<>(players)); }
    public Set<UUID> getAlive()   { return Collections.unmodifiableSet(new HashSet<>(alive)); }
    public Set<UUID> getTagged()  { return Collections.unmodifiableSet(new HashSet<>(tagged)); }

    public boolean isTagged(UUID id)  { return tagged.contains(id); }
    public boolean isAlive(UUID id)   { return alive.contains(id); }
    public boolean isInArena(UUID id) { return players.contains(id); }

    /* ─── Cycle de vie ─── */

    /** Démarre l'horloge de jeu (un tick par 1s). Idempotent. */
    public void ensureTask() {
        if (task != null && !task.isCancelled()) return;
        task = new GameTask();
        task.runTaskTimer(plugin, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            try { task.cancel(); } catch (IllegalStateException ignored) {}
        }
        for (UUID id : new ArrayList<>(players)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) handleQuit(p, true);
        }
        destroyAllSidebars();
        state = enabled ? ArenaState.WAITING : ArenaState.DISABLED;
        players.clear();
        alive.clear();
        tagged.clear();
        lastTagger.clear();
        tagCooldown.clear();
        joinedAtMs.clear();
        lastWinnerName = null;
    }

    /* ─── Flux joueur ─── */

    /** Tente d'ajouter un joueur en lobby. Retourne true si ok. */
    public boolean addPlayer(Player p) {
        if (!enabled) return false;
        if (isFull()) return false;
        if (state == ArenaState.IN_GAME || state == ArenaState.ENDING) return false;
        if (!isReady()) return false;
        UUID id = p.getUniqueId();
        if (players.contains(id)) return false;

        players.add(id);
        joinedAtMs.put(id, System.currentTimeMillis());
        prepareForLobby(p);
        if (lobby != null) p.teleport(lobby);

        broadcast(plugin.messages().get("joined",
                "player", p.getName(),
                "arena", name,
                "count", players.size(),
                "max", maxPlayers));

        ensureTask();
        if (state == ArenaState.WAITING && players.size() >= minPlayers) {
            startLobbyCountdown(lobbyCountdownSeconds);
        }
        return true;
    }

    public void removePlayer(Player p) {
        handleQuit(p, false);
    }

    public void handleQuit(Player p, boolean force) {
        UUID id = p.getUniqueId();
        if (!players.contains(id)) return;
        players.remove(id);
        boolean wasAlive = alive.remove(id);
        tagged.remove(id);
        lastTagger.remove(id);
        tagCooldown.remove(id);
        joinedAtMs.remove(id);
        destroySidebar(id);

        broadcast(plugin.messages().get("left", "player", p.getName()));

        // Restauration cosmétique
        cleanPlayer(p);
        Location backLobby = plugin.arenas().getMainLobby();
        if (backLobby != null) p.teleport(backLobby);
        else if (lobby != null) p.teleport(lobby);

        if (state == ArenaState.STARTING && players.size() < minPlayers) {
            broadcast(plugin.messages().get("countdown-cancelled"));
            state = ArenaState.WAITING;
            countdown = 0;
        }

        if (state == ArenaState.IN_GAME && wasAlive) {
            // partie en cours, vérifier la condition de victoire
            checkWinCondition();
        }
    }

    /* ─── Logique TNT Tag ─── */

    /**
     * Traite la tentative de passation de la TNT entre un attaquant et une cible.
     * Retourne true si le tag a été passé.
     */
    public boolean tryPassTag(Player tagger, Player target) {
        if (state != ArenaState.IN_GAME) return false;
        UUID a = tagger.getUniqueId();
        UUID b = target.getUniqueId();
        if (!alive.contains(a) || !alive.contains(b)) return false;
        if (!tagged.contains(a)) return false;
        if (tagged.contains(b)) return false;
        long now = System.currentTimeMillis();
        Long cd = tagCooldown.get(b);
        if (cd != null && cd > now) return false;

        tagged.remove(a);
        tagged.add(b);
        lastTagger.put(b, a);
        tagCooldown.put(b, now + Math.max(0, plugin.getConfig().getInt("tag-cooldown-seconds", 1)) * 1000L);

        // Stats
        PlayerStats sa = plugin.stats().getOrCreate(a, tagger.getName());
        sa.tagsPassed++;

        // Cosmétique
        clearTagItems(tagger);
        applyTagItems(target);

        broadcast(plugin.messages().get("tag-passed",
                "tagger", tagger.getName(),
                "target", target.getName()));
        plugin.messages().send(tagger, "you-tagged-target", "target", target.getName());

        target.sendTitle(plugin.messages().get("you-are-tagged-title"),
                plugin.messages().get("you-are-tagged-subtitle"), 5, 30, 10);
        plugin.messages().send(target, "you-got-tagged");
        playSoundAll(Sound.ENTITY_TNT_PRIMED, 1f, 1.5f);
        return true;
    }

    /** À la fin de chaque round, les porteurs explosent et sont éliminés. */
    private void detonateAndEliminate() {
        broadcast(plugin.messages().get("explosion"));
        List<UUID> toKill = new ArrayList<>(tagged);
        for (UUID id : toKill) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                eliminateInternal(id, lastTagger.get(id), null);
                continue;
            }
            Location loc = p.getLocation();
            // Effet visuel/sonore d'explosion (sans dégâts aux blocs si désactivé)
            p.getWorld().createExplosion(loc, 0f, false, blockDamage);
            p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            eliminateInternal(id, lastTagger.get(id), p);
        }
        tagged.clear();
    }

    private void eliminateInternal(UUID id, UUID killerId, Player p) {
        if (!alive.remove(id)) return;
        tagged.remove(id);

        // Stats victime (temps de survie)
        Long joined = joinedAtMs.remove(id);
        if (joined != null) {
            PlayerStats sv = plugin.stats().getOrCreate(id, p == null ? "" : p.getName());
            sv.survivalSeconds += Math.max(0, (System.currentTimeMillis() - joined) / 1000L);
        }
        // Reset streak en cours côté victime
        PlayerStats victim = plugin.stats().get(id);
        if (victim != null) {
            victim.currentStreak = 0;
        }

        // Stats killer (le dernier qui lui a passé la TNT)
        if (killerId != null && !killerId.equals(id) && players.contains(killerId)) {
            Player killer = Bukkit.getPlayer(killerId);
            PlayerStats sk = plugin.stats().getOrCreate(killerId, killer == null ? "" : killer.getName());
            sk.kills++;
        }

        if (p != null) {
            cleanPlayer(p);
            plugin.messages().send(p, "eliminated");
            if (plugin.getConfig().getBoolean("allow-spectate", true)) {
                p.setGameMode(GameMode.SPECTATOR);
                plugin.messages().send(p, "spectator-mode");
            } else {
                Location back = plugin.arenas().getMainLobby();
                if (back == null) back = lobby;
                if (back != null) p.teleport(back);
            }
        }
    }

    /** Lance la phase de countdown du lobby. */
    public void startLobbyCountdown(int seconds) {
        state = ArenaState.STARTING;
        countdown = Math.max(1, seconds);
        broadcast(plugin.messages().get("countdown-start", "seconds", countdown));
    }

    /** Démarre la partie. Appelé par le tick à la fin du countdown. */
    private void startGame() {
        // Filtrer les joueurs encore connectés
        List<UUID> active = new ArrayList<>();
        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) active.add(id);
        }
        if (active.size() < minPlayers) {
            broadcast(plugin.messages().get("countdown-cancelled"));
            state = ArenaState.WAITING;
            return;
        }

        state = ArenaState.IN_GAME;
        roundIndex = 0;
        alive.clear();
        alive.addAll(active);

        // TP joueurs vers les spawns (round-robin)
        for (int i = 0; i < active.size(); i++) {
            UUID id = active.get(i);
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Location s = spawns.get(i % spawns.size());
            p.teleport(s);
            preparePlayerInGame(p);
            joinedAtMs.put(id, System.currentTimeMillis());
            // Stats : +1 partie
            PlayerStats ps = plugin.stats().getOrCreate(id, p.getName());
            ps.gamesPlayed++;
            p.sendTitle(plugin.messages().get("game-start-title"),
                    plugin.messages().get("game-start-subtitle"), 10, 40, 10);
        }
        playSoundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
        nextRound();
    }

    /** Prépare le round suivant. */
    private void nextRound() {
        // Conditions d'arrêt
        if (alive.size() <= 1 || (roundsPerGame > 0 && roundIndex >= roundsPerGame)) {
            endGame();
            return;
        }
        roundIndex++;
        roundSecondsLeft = Math.max(5, roundDurationSeconds);
        tagged.clear();
        lastTagger.clear();
        tagCooldown.clear();

        int targetCount = Math.max(1, (int) Math.round(alive.size() * Math.max(0.05, initialTaggedRatio)));
        if (targetCount >= alive.size()) targetCount = Math.max(1, alive.size() - 1);

        List<UUID> shuffled = new ArrayList<>(alive);
        Collections.shuffle(shuffled);
        for (int i = 0; i < targetCount; i++) {
            UUID id = shuffled.get(i);
            tagged.add(id);
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                applyTagItems(p);
                p.sendTitle(plugin.messages().get("you-are-tagged-title"),
                        plugin.messages().get("you-are-tagged-subtitle"), 5, 40, 10);
                plugin.messages().send(p, "you-got-tagged");
            }
        }
        broadcast(plugin.messages().get("round-start",
                "round", roundIndex, "seconds", roundSecondsLeft));
        playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
    }

    /** Vérifie si la partie doit se terminer (>=1 joueur restant). */
    private void checkWinCondition() {
        if (state != ArenaState.IN_GAME) return;
        if (alive.size() <= 1) endGame();
    }

    /** Termine la partie (annonce le gagnant, met à jour les stats). */
    private void endGame() {
        state = ArenaState.ENDING;
        UUID winnerId = alive.size() == 1 ? alive.iterator().next() : null;
        String winnerName = "—";
        if (winnerId != null) {
            Player wp = Bukkit.getPlayer(winnerId);
            winnerName = wp == null ? "?" : wp.getName();
            lastWinnerName = winnerName;
            PlayerStats ws = plugin.stats().getOrCreate(winnerId, winnerName);
            ws.wins++;
            ws.currentStreak++;
            if (ws.currentStreak > ws.bestStreak) ws.bestStreak = ws.currentStreak;
            // Le gagnant a aussi survécu jusqu'au bout : crédit du temps de survie
            Long joined = joinedAtMs.remove(winnerId);
            if (joined != null) ws.survivalSeconds += Math.max(0, (System.currentTimeMillis() - joined) / 1000L);
            broadcast(plugin.messages().get("winner", "winner", winnerName));
            if (wp != null) {
                wp.sendTitle("§6§lVICTORY", "§7" + winnerName, 10, 60, 20);
                playSoundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        } else {
            lastWinnerName = null;
            broadcast(plugin.messages().get("no-winner"));
        }
        // Replanifier le retour au lobby
        countdown = 5;
    }

    /** Renvoie tous les joueurs au lobby principal et reset l'état. */
    private void resetToLobby() {
        broadcast(plugin.messages().get("game-end"));
        Location back = plugin.arenas().getMainLobby();
        for (UUID id : new ArrayList<>(players)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            cleanPlayer(p);
            destroySidebar(id);
            if (back != null) p.teleport(back);
            else if (lobby != null) p.teleport(lobby);
        }
        destroyAllSidebars();
        players.clear();
        alive.clear();
        tagged.clear();
        lastTagger.clear();
        tagCooldown.clear();
        joinedAtMs.clear();
        lastWinnerName = null;
        state = enabled ? ArenaState.WAITING : ArenaState.DISABLED;
        plugin.stats().saveAll();
    }

    /* ─── Scoreboard sidebar (style Hypixel) ─── */

    private void renderSidebars() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        Messages M = plugin.messages();
        String title = M.get("scoreboard.title");
        String footer = M.get("scoreboard.footer");

        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Sidebar sb = sidebars.get(id);
            if (sb == null) {
                sb = new Sidebar(p, title);
                sidebars.put(id, sb);
            } else {
                sb.setTitle(title);
            }

            List<String> rawLines;
            Object[] vars;
            switch (state) {
                case WAITING:
                case STARTING: {
                    String status;
                    if (state == ArenaState.STARTING) {
                        status = M.get("scoreboard.lobby-status-starting", "seconds", countdown);
                    } else if (players.size() < minPlayers) {
                        status = M.get("scoreboard.lobby-status-need-more");
                    } else {
                        status = M.get("scoreboard.lobby-status-waiting");
                    }
                    rawLines = M.rawList("scoreboard.lobby");
                    vars = new Object[]{
                            "arena", name,
                            "count", players.size(),
                            "max", maxPlayers,
                            "status", status,
                            "footer", footer
                    };
                    break;
                }
                case IN_GAME: {
                    String status;
                    if (!alive.contains(id)) {
                        status = M.get("scoreboard.game-status-spectator");
                    } else if (tagged.contains(id)) {
                        status = M.get("scoreboard.game-status-tagged");
                    } else {
                        status = M.get("scoreboard.game-status-safe");
                    }
                    rawLines = M.rawList("scoreboard.game");
                    vars = new Object[]{
                            "round", roundIndex,
                            "time", roundSecondsLeft,
                            "alive", alive.size(),
                            "tagged", tagged.size(),
                            "status", status,
                            "footer", footer
                    };
                    break;
                }
                case ENDING: {
                    String result = lastWinnerName != null
                            ? M.get("scoreboard.ending-result-winner", "winner", lastWinnerName)
                            : M.get("scoreboard.ending-result-no-winner");
                    rawLines = M.rawList("scoreboard.ending");
                    vars = new Object[]{
                            "result", result,
                            "footer", footer
                    };
                    break;
                }
                default:
                    rawLines = java.util.Collections.emptyList();
                    vars = new Object[0];
                    break;
            }

            List<String> lines = new ArrayList<>(rawLines.size());
            for (String raw : rawLines) lines.add(M.applyPlaceholders(raw, vars));
            sb.setLines(lines);
        }

        // Nettoie les sidebars orphelines (joueur déconnecté ou parti)
        for (UUID id : new ArrayList<>(sidebars.keySet())) {
            if (!players.contains(id)) destroySidebar(id);
        }
    }

    private void destroySidebar(UUID id) {
        Sidebar sb = sidebars.remove(id);
        if (sb != null) sb.destroy();
    }

    private void destroyAllSidebars() {
        for (UUID id : new ArrayList<>(sidebars.keySet())) destroySidebar(id);
    }

    /* ─── Cosmétique ─── */

    private void prepareForLobby(Player p) {
        cleanPlayer(p);
        p.setGameMode(GameMode.ADVENTURE);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
    }

    private void preparePlayerInGame(Player p) {
        cleanPlayer(p);
        p.setGameMode(GameMode.ADVENTURE);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE,
                Math.max(0, speedAmp - 1), false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE,
                Math.max(0, jumpAmp - 1), false, false));
    }

    public void applyTagItems(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TNT));
        inv.setItemInMainHand(ItemUtil.makeTnt(plugin));
        p.setGlowing(true);
    }

    public void clearTagItems(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(null);
        // Nettoyer toutes les TNT taggées
        for (int i = 0; i < inv.getSize(); i++) {
            if (ItemUtil.isTagItem(plugin, inv.getItem(i))) inv.setItem(i, null);
        }
        p.setGlowing(false);
    }

    public void cleanPlayer(Player p) {
        clearTagItems(p);
        p.removePotionEffect(PotionEffectType.SPEED);
        p.removePotionEffect(PotionEffectType.JUMP_BOOST);
        p.setGameMode(GameMode.ADVENTURE);
    }

    /* ─── Helpers ─── */

    public void broadcast(String msg) {
        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(msg);
        }
    }

    public void playSoundAll(Sound s, float vol, float pitch) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;
        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.playSound(p.getLocation(), s, vol, pitch);
        }
    }

    /* ─── BukkitRunnable interne (1 tick / 1 seconde) ─── */

    private final class GameTask extends BukkitRunnable {
        @Override public void run() {
            if (!enabled) return;
            switch (state) {
                case WAITING:
                    // rien — on attend des joueurs
                    break;
                case STARTING:
                    if (players.size() < minPlayers) {
                        broadcast(plugin.messages().get("countdown-cancelled"));
                        state = ArenaState.WAITING;
                        countdown = 0;
                        break;
                    }
                    if (countdown <= 0) {
                        startGame();
                        break;
                    }
                    if (countdown <= 5 || countdown == 10 || countdown == 15 || countdown == 30) {
                        broadcast(plugin.messages().get("countdown-tick", "seconds", countdown));
                        playSoundAll(Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                    countdown--;
                    break;
                case IN_GAME:
                    // Tick action bar / titre
                    sendActionBar();
                    if (roundSecondsLeft <= 0) {
                        detonateAndEliminate();
                        if (alive.size() <= 1) {
                            endGame();
                        } else {
                            nextRound();
                        }
                        break;
                    }
                    if (roundSecondsLeft <= 5) {
                        playSoundAll(Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f + roundSecondsLeft * 0.1f);
                    }
                    roundSecondsLeft--;
                    break;
                case ENDING:
                    if (countdown <= 0) {
                        resetToLobby();
                    } else {
                        countdown--;
                    }
                    break;
                case DISABLED:
                    break;
            }
            renderSidebars();
        }

        private void sendActionBar() {
            String bar = "§6Round " + roundIndex + " §8| §c" + roundSecondsLeft + "s §8| §a"
                    + alive.size() + " §7vivants §8| §c" + tagged.size() + " §7TNT";
            for (UUID id : players) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.sendActionBar(net.kyori.adventure.text.Component.text(bar));
            }
        }
    }

    /* Suppression de l'avertissement "static" sur Messages.color (utilisé dans
     * les méthodes ci-dessus indirectement via Messages#get). */
    @SuppressWarnings("unused")
    private static final Class<?> MSG_REF = Messages.class;
}
