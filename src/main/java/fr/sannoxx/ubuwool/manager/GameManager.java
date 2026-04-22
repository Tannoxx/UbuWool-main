package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.*;
import fr.sannoxx.ubuwool.ability.*;
import fr.sannoxx.ubuwool.menu.AgentMenu;
import fr.sannoxx.ubuwool.menu.BuyMenu;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final int instanceId;

    public int getInstanceId() { return instanceId; }

    public GameState state = GameState.WAITING;
    public Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    public final Set<UUID> confirmedTeam = new HashSet<>();

    public Set<UUID> teamRed  = new LinkedHashSet<>();
    public Set<UUID> teamBlue = new LinkedHashSet<>();

    public Set<UUID> deadPlayers = new HashSet<>();
    public int scoreRed = 0, scoreBlue = 0, currentRound = 0;

    public List<Location>         fantomAnvils        = new ArrayList<>();
    public Set<Integer>           fantomFallingBlocks = new HashSet<>();
    public List<Location>         horcusSponges       = new ArrayList<>();
    public List<Location>         bambouvoreWalls     = new ArrayList<>();
    public List<Location>         askyJukeboxes       = new ArrayList<>();
    public Map<String, Location>  banosPlayers        = new HashMap<>();
    public Map<UUID, UUID>        lastDamager         = new HashMap<>();
    public List<String>           roundHistory        = new ArrayList<>();

    public Map<UUID, UUID>                activeRockets = new HashMap<>();
    public Map<UUID, org.bukkit.Location> rocketLastPos = new HashMap<>();

    private int roundSeconds = 0;
    private int timerTask = -1, captureTask = -1, countingTask = -1;
    private List<Location> activeCenterBlocks = null;
    private int activeCleanCX, activeCleanCY, activeCleanCZ, activeCleanR;

    private String activeMapName = null;

    public String getActiveMapName() { return activeMapName; }

    public enum GameState { WAITING, AGENT_SELECT, BUY_PHASE, ROUND_ACTIVE, ROUND_END, GAME_OVER }

    public GameManager(int instanceId) {
        this.instanceId = instanceId;
    }

    public boolean isPlayerInGame(Player player) {
        return playerDataMap.containsKey(player.getUniqueId());
    }

    public static int getRoundsToWin()               { return UbuWool.getInstance().getConfig().getInt("game.rounds-to-win", 5); }
    public static int getBuyPhaseSeconds()           { return UbuWool.getInstance().getConfig().getInt("game.buy-phase-seconds", 15); }
    public static int getAgentSelectSeconds()        { return UbuWool.getInstance().getConfig().getInt("game.agent-select-seconds", 60); }
    public static int getPauseBetweenRoundsSeconds() { return UbuWool.getInstance().getConfig().getInt("game.pause-between-rounds-seconds", 5); }
    public static int getStartUbus()                 { return UbuWool.getInstance().getConfig().getInt("economy.start-ubus", 500); }
    public static int getKillUbus()                  { return UbuWool.getInstance().getConfig().getInt("economy.kill-ubus", 250); }
    public static int getRoundWinUbus()              { return UbuWool.getInstance().getConfig().getInt("economy.round-win-ubus", 1800); }
    public static int getRoundLossUbus()             { return UbuWool.getInstance().getConfig().getInt("economy.round-loss-ubus", 1400); }
    public static int getCenterCaptureUbus()         { return UbuWool.getInstance().getConfig().getInt("economy.center-capture-ubus", 500); }
    public static int getStartingWool()              { return UbuWool.getInstance().getConfig().getInt("kit.starting-wool", 16); }
    public static int getShopPrice(String key, int defaultValue) {
        return UbuWool.getInstance().getConfig().getInt("shop." + key, defaultValue);
    }

    public boolean isRedTeam(Player player) { return teamRed.contains(player.getUniqueId()); }

    public List<Player> getAllPlayers() {
        List<Player> all = new ArrayList<>();
        for (UUID id : teamRed)  { Player p = Bukkit.getPlayer(id); if (p != null) all.add(p); }
        for (UUID id : teamBlue) { Player p = Bukkit.getPlayer(id); if (p != null) all.add(p); }
        return all;
    }

    public void tryStartGame() {
        if (state != GameState.WAITING) return;

        int total = teamRed.size() + teamBlue.size();
        if (total < 2) return;
        if (teamRed.isEmpty() || teamBlue.isEmpty()) return;

        for (UUID uuid : teamRed) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (!fr.sannoxx.ubuwool.menu.MapVoteMenu.votes.containsKey(p.getName())) return;
        }
        for (UUID uuid : teamBlue) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (!fr.sannoxx.ubuwool.menu.MapVoteMenu.votes.containsKey(p.getName())) return;
        }

        state = GameState.AGENT_SELECT;
        confirmedTeam.clear();

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), this::startGame, 40L);
    }

    public List<Player> getTeamRedPlayers() {
        return teamRed.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
    }
    public List<Player> getTeamBluePlayers() {
        return teamBlue.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Location getSpawnRed() {
        MapConfig.UbuMap map = MapConfig.getSelectedMap();
        return map != null
                ? new Location(Bukkit.getWorlds().getFirst(), map.spawnRedX, map.spawnRedY, map.spawnRedZ)
                : new Location(Bukkit.getWorlds().getFirst(), 30, 65, 30);
    }
    private Location getSpawnBlue() {
        MapConfig.UbuMap map = MapConfig.getSelectedMap();
        return map != null
                ? new Location(Bukkit.getWorlds().getFirst(), map.spawnBlueX, map.spawnBlueY, map.spawnBlueZ)
                : new Location(Bukkit.getWorlds().getFirst(), -30, 65, -30);
    }
    public void tpToSpawnPublic(Player p) { tpToSpawn(p); }
    private void tpToSpawn(Player p) { p.teleport(isRedTeam(p) ? getSpawnRed() : getSpawnBlue()); }

    public List<Location> getActiveCenterBlocks() {
        if (activeCenterBlocks != null) return activeCenterBlocks;
        MapConfig.UbuMap map = MapConfig.getSelectedMap();
        if (map != null && map.centerBlocks != null) {
            World world = Bukkit.getWorlds().getFirst();
            activeCenterBlocks = map.centerBlocks.stream()
                    .map(arr -> new Location(world, arr[0], arr[1], arr[2]))
                    .collect(Collectors.toList());
            return activeCenterBlocks;
        }
        return new ArrayList<>();
    }

    public void joinTeam(Player player) { fr.sannoxx.ubuwool.menu.TeamMenu.open(player, this); }

    public void leaveTeam(Player player) {
        teamRed.remove(player.getUniqueId());
        teamBlue.remove(player.getUniqueId());
        playerDataMap.remove(player.getUniqueId());
        confirmedTeam.remove(player.getUniqueId());
        player.sendMessage(Lang.get(player, Lang.Key.LEFT_TEAM));
    }

    public void startGame() {
        if (state != GameState.WAITING && state != GameState.AGENT_SELECT) return;
        if (teamRed.isEmpty() || teamBlue.isEmpty()) {
            broadcast(Lang.Key.NEED_PLAYERS); return;
        }
        String winningMap = fr.sannoxx.ubuwool.menu.MapVoteMenu.getWinningMap();
        if (winningMap != null) {
            MapConfig.setSelectedMap(winningMap);
            activeMapName = winningMap;
            MapConfig.UbuMap wm = MapConfig.getMap(winningMap);
            String display = (wm != null && wm.displayName != null) ? wm.displayName : winningMap;
            broadcastFormatted(Lang.Key.MAP_SELECTED, display);
        } else {
            broadcast(Lang.Key.NO_VALID_MAP); return;
        }
        fr.sannoxx.ubuwool.menu.MapVoteMenu.resetVotes();
        scoreRed = 0; scoreBlue = 0; currentRound = 0;
        state = GameState.AGENT_SELECT;
        UbuHUD.init(this);
        UbuHUD.update(this, 0, 0, getAgentSelectSeconds(), Lang.Key.HUD_AGENT_SELECT);

        for (Player p : getAllPlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
            p.getInventory().clear();
            AbilityManager.clearCooldowns(p.getUniqueId());
            tpToSpawn(p);
        }
        broadcast(Lang.Key.CHOOSE_AGENT);
        for (Player p : getAllPlayers()) AgentMenu.open(p);
        startAgentPhase();
    }

    private void startAgentPhase() {
        cancelTimer();
        timerTask = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), new Runnable() {
                    int sec = getAgentSelectSeconds();
                    public void run() {
                        if (sec <= 0) {
                            boolean missing = getAllPlayers().stream().anyMatch(p -> {
                                PlayerData d = playerDataMap.get(p.getUniqueId());
                                return d == null || d.agent == null;
                            });
                            if (missing) stopGame();
                            else startBuyPhase();
                            cancelTimer(); return;
                        }
                        UbuHUD.update(GameManager.this, 0, 0, sec--, Lang.Key.HUD_AGENT_SELECT);
                        boolean allChosen = getAllPlayers().stream().allMatch(p -> {
                            PlayerData d = playerDataMap.get(p.getUniqueId());
                            return d != null && d.agent != null;
                        });
                        if (allChosen && !getAllPlayers().isEmpty()) { cancelTimer(); startBuyPhase(); }
                    }
                }, 20L, 20L).getTaskId();
    }

    public void checkTeamEliminationPublic() { checkTeamElimination(); }

    public void setAgent(Player player, String agentName) {
        PlayerData data = playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
        if (!teamRed.contains(player.getUniqueId()) && !teamBlue.contains(player.getUniqueId())) {
            player.sendMessage(Lang.get(player, Lang.Key.NOT_IN_GAME)); return;
        }
        List<Player> myTeam = isRedTeam(player) ? getTeamRedPlayers() : getTeamBluePlayers();
        for (Player teammate : myTeam) {
            if (teammate == player) continue;
            PlayerData td = playerDataMap.get(teammate.getUniqueId());
            if (td != null && td.agent != null && td.agent.getName().equalsIgnoreCase(agentName)) {
                player.sendMessage(Lang.get(player, Lang.Key.AGENT_TAKEN));
                UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(), () -> {
                    if (state == GameState.AGENT_SELECT) AgentMenu.open(player);
                }, 10L);
                return;
            }
        }
        data.agent = switch (agentName.toLowerCase()) {
            case "sembol"         -> new fr.sannoxx.ubuwool.agent.Sembol();
            case "fantom"         -> new fr.sannoxx.ubuwool.agent.Fantom();
            case "gargamel"       -> new fr.sannoxx.ubuwool.agent.Gargamel();
            case "horcus"         -> new fr.sannoxx.ubuwool.agent.Horcus();
            case "bambouvore"     -> new fr.sannoxx.ubuwool.agent.Bambouvore();
            case "lolita"         -> new fr.sannoxx.ubuwool.agent.Lolita();
            case "asky"           -> new fr.sannoxx.ubuwool.agent.Asky();
            case "carlos"         -> new fr.sannoxx.ubuwool.agent.Carlos();
            case "larok"          -> new fr.sannoxx.ubuwool.agent.Larok();
            case "ticksuspicious" -> new fr.sannoxx.ubuwool.agent.Ticksuspicious();
            case "mascord"        -> new fr.sannoxx.ubuwool.agent.Mascord();
            case "hijab"          -> new fr.sannoxx.ubuwool.agent.Hijab();
            case "ilargia"        -> new fr.sannoxx.ubuwool.agent.Ilargia();
            case "gekko"          -> new fr.sannoxx.ubuwool.agent.Gekko();
            case "doma"          -> new fr.sannoxx.ubuwool.agent.Doma();
            default -> {
                player.sendMessage(Lang.get(player, Lang.Key.AGENT_UNKNOWN));
                yield null;
            }
        };
        if (data.agent == null) return;
        for (Player teammate : myTeam) {
            if (teammate == player) continue;
            teammate.sendMessage("§7" + player.getName() + " §7→ "
                    + data.agent.getColor() + "§l" + data.agent.getName());
        }
        player.sendMessage(Lang.get(player, Lang.Key.AGENT_CHOSEN,
                data.agent.getColor() + "§l" + data.agent.getName()));
        TabManager.updatePlayerTab(player);
    }

    public void startBuyPhase() {
        state = GameState.BUY_PHASE;
        for (Player p : getAllPlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20); p.setSaturation(0);
            tpToSpawn(p);
        }
        deadPlayers.clear();
        broadcast(Lang.Key.BUY_PHASE_START);
        for (Player p : getAllPlayers()) BuyMenu.open(p, this);
        startTimer(getBuyPhaseSeconds(), Lang.Key.HUD_BUY_PHASE, this::startRound);
    }

    public void startRound() {
        currentRound++;
        state = GameState.ROUND_ACTIVE;
        deadPlayers.clear();
        roundSeconds = 0;
        DeathRecap.clearAll();

        for (Player p : getAllPlayers()) p.closeInventory();

        MapConfig.UbuMap activeMap = MapConfig.getSelectedMap();
        if (activeMap != null) {
            activeCleanCX = activeMap.woolCleanCenterX;
            activeCleanCY = activeMap.woolCleanCenterY;
            activeCleanCZ = activeMap.woolCleanCenterZ;
            activeCleanR  = activeMap.woolCleanRadius;
            activeMapName = activeMap.name;
        }

        Bukkit.getWorlds().getFirst().getEntitiesByClass(org.bukkit.entity.Arrow.class)
                .forEach(org.bukkit.entity.Entity::remove);
        Bukkit.getWorlds().getFirst().getEntitiesByClass(org.bukkit.entity.Item.class)
                .forEach(org.bukkit.entity.Entity::remove);

        cleanWool();

        if (activeMap != null && activeMap.centerBlocks != null) {
            World world = Bukkit.getWorlds().getFirst();
            activeCenterBlocks = activeMap.centerBlocks.stream()
                    .map(arr -> new Location(world, arr[0], arr[1], arr[2]))
                    .collect(Collectors.toList());
        }
        resetCenterBlocks();

        broadcastFormatted(Lang.Key.ROUND_START, currentRound);

        for (Player p : getTeamRedPlayers())  setupPlayerForRound(p, true);
        for (Player p : getTeamBluePlayers()) setupPlayerForRound(p, false);

        BuyMenu.resetPurchases(this);
        TabManager.updateTab(this);
        startCaptureChecker();
        startCountingTimer();
        StarManager.startRound(this);
    }

    private void setupPlayerForRound(Player p, boolean isRed) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        giveKit(p, isRed);
        applyShopPurchases(p);
        tpToSpawn(p);
        PlayerData d = playerDataMap.get(p.getUniqueId());
        if (d != null && d.agent != null) d.agent.applyPassive(p);
    }

    public void endRound(boolean redWins) {
        if (state == GameState.ROUND_END || state == GameState.GAME_OVER) return;
        state = GameState.ROUND_END;
        cancelTimer();

        for (UUID uuid : teamRed)  PlayerStats.recordRoundEnd(uuid, redWins);
        for (UUID uuid : teamBlue) PlayerStats.recordRoundEnd(uuid, !redWins);
        PlayerStats.saveAll();

        if (redWins) {
            scoreRed++;
            broadcastFormatted(Lang.Key.ROUND_WIN_RED, scoreRed, scoreBlue);
            getTeamRedPlayers().forEach(p -> addUbus(p, getRoundWinUbus()));
            getTeamBluePlayers().forEach(p -> addUbus(p, getRoundLossUbus()));
            roundHistory.add("§c■ ");
        } else {
            scoreBlue++;
            broadcastFormatted(Lang.Key.ROUND_WIN_BLUE, scoreRed, scoreBlue);
            getTeamBluePlayers().forEach(p -> addUbus(p, getRoundWinUbus()));
            getTeamRedPlayers().forEach(p -> addUbus(p, getRoundLossUbus()));
            roundHistory.add("§9■ ");
        }

        UbuHUD.update(this, scoreRed, scoreBlue, 0, Lang.Key.HUD_ROUND_END);

        if (scoreRed  >= getRoundsToWin()) { endGame(true);  return; }
        if (scoreBlue >= getRoundsToWin()) { endGame(false); return; }

        for (Player p : getAllPlayers()) {
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        }

        cancelCapture();
        cancelCountingTimer();
        cleanupRoundBlocks();

        StarManager.stopRound(this);
        startTimer(getPauseBetweenRoundsSeconds(), Lang.Key.HUD_PAUSE, this::startBuyPhase);
    }

    public void forceEndRound(boolean redWins) {
        if (state != GameState.ROUND_ACTIVE) return;
        broadcast(Lang.Key.ROUND_FORCE_END);
        endRound(redWins);
    }

    private void cleanupRoundBlocks() {
        World world = Bukkit.getWorlds().getFirst();

        for (Location loc : new ArrayList<>(fantomAnvils)) {
            Material m = loc.getBlock().getType();
            if (m == Material.ANVIL || m == Material.CHIPPED_ANVIL || m == Material.DAMAGED_ANVIL)
                loc.getBlock().setType(Material.AIR);
        }
        fantomAnvils.clear();
        fantomFallingBlocks.clear();

        int r = activeCleanR, cx = activeCleanCX, cy = activeCleanCY, cz = activeCleanCZ;
        for (int x = cx-r; x <= cx+r; x++) {
            for (int y = cy-40; y <= cy+20; y++) {
                for (int z = cz-r; z <= cz+r; z++) {
                    Material m = world.getBlockAt(x, y, z).getType();
                    if (m == Material.ANVIL || m == Material.CHIPPED_ANVIL || m == Material.DAMAGED_ANVIL)
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }

        for (Location loc : new ArrayList<>(horcusSponges)) {
            if (loc.getBlock().getType() == Material.SPONGE) loc.getBlock().setType(Material.AIR);
        }
        horcusSponges.clear();
        HorcusAbilities.spongePositions.clear();
        HorcusAbilities.resetRound();

        for (Location loc : new ArrayList<>(bambouvoreWalls)) {
            if (loc.getBlock().getType() == Material.BAMBOO_PLANKS) loc.getBlock().setType(Material.AIR);
        }
        bambouvoreWalls.clear();

        for (Location loc : new ArrayList<>(askyJukeboxes)) {
            if (loc.getBlock().getType() == Material.JUKEBOX) loc.getBlock().setType(Material.AIR);
        }
        askyJukeboxes.clear();

        for (Map.Entry<String, Location> entry : new HashMap<>(banosPlayers).entrySet()) {
            Player p = Bukkit.getPlayerExact(entry.getKey());
            if (p != null) {
                p.teleport(entry.getValue());
                p.removePotionEffect(PotionEffectType.STRENGTH);
                p.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            }
        }
        banosPlayers.clear();

        AbilityDispatcher.resetAllRounds();
        AbilityStateManager.resetAll();

        LolitaAbilities.resetRound();
        TicksuspiciousAbilities.resetRound();
        MascordAbilities.resetRound();
        HijabAbilities.resetRound();
        IlargiaAbilities.resetRound();
        GekkoAbilities.resetRound();
        CarlosAbilities.resetRound();

        DeathRecap.clearAll();
    }

    public void endGame(boolean redWins) {
        state = GameState.GAME_OVER;
        cancelTimer(); cancelCapture();
        UbuHUD.reset(this); TabManager.resetTab(this);
        List<Player> allPlayers = getAllPlayers();

        for (Player p : allPlayers) {
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
        }
        broadcast(redWins ? Lang.Key.GAME_WIN_RED : Lang.Key.GAME_WIN_BLUE);
        printGameSummary(redWins);

        for (UUID uuid : teamRed) {
            PlayerData d = playerDataMap.get(uuid);
            String agentName = (d != null && d.agent != null) ? d.agent.getName() : "?";
            PlayerStats.recordGameEnd(uuid, agentName, redWins);
        }
        for (UUID uuid : teamBlue) {
            PlayerData d = playerDataMap.get(uuid);
            String agentName = (d != null && d.agent != null) ? d.agent.getName() : "?";
            PlayerStats.recordGameEnd(uuid, agentName, !redWins);
        }
        PlayerStats.saveAll();

        for (Player p : allPlayers)
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        StarManager.stopRound(this);
        cleanupRoundBlocks();
        cleanWool();
        reset();

        org.bukkit.World mainWorld = Bukkit.getWorlds().getFirst();
        Location worldSpawn = mainWorld.getSpawnLocation();
        for (Player p : allPlayers) {
            if (p.isOnline()) {
                p.setGameMode(GameMode.ADVENTURE);
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.getInventory().clear();
                p.teleport(worldSpawn);
            }
        }

        GameRegistry.removeInstance(instanceId);
    }

    private void printGameSummary(boolean redWins) {
        for (Player p : getAllPlayers()) {
            p.sendMessage("");
            p.sendMessage(Lang.get(p, Lang.Key.SUMMARY_TITLE));
            StringBuilder rl = new StringBuilder(Lang.get(p, Lang.Key.SUMMARY_ROUNDS));
            for (String r : roundHistory) rl.append(r);
            p.sendMessage(rl.toString());
            p.sendMessage("");
            p.sendMessage(Lang.get(p, Lang.Key.SUMMARY_TEAM_RED));
            for (Player tr : getTeamRedPlayers()) {
                PlayerData d = playerDataMap.get(tr.getUniqueId());
                int k = d != null ? d.kills : 0;
                String ag = (d != null && d.agent != null)
                        ? d.agent.getColor() + d.agent.getName()
                        : Lang.get(p, Lang.Key.SUMMARY_NO_AGENT);
                String killWord = k == 1
                        ? Lang.get(p, Lang.Key.SUMMARY_KILL_SINGULAR)
                        : Lang.get(p, Lang.Key.SUMMARY_KILL_PLURAL);
                p.sendMessage("  §f" + tr.getName() + " §7(" + ag + "§7) — §e" + k + " " + killWord);
            }
            p.sendMessage("");
            p.sendMessage(Lang.get(p, Lang.Key.SUMMARY_TEAM_BLUE));
            for (Player tb : getTeamBluePlayers()) {
                PlayerData d = playerDataMap.get(tb.getUniqueId());
                int k = d != null ? d.kills : 0;
                String ag = (d != null && d.agent != null)
                        ? d.agent.getColor() + d.agent.getName()
                        : Lang.get(p, Lang.Key.SUMMARY_NO_AGENT);
                String killWord = k == 1
                        ? Lang.get(p, Lang.Key.SUMMARY_KILL_SINGULAR)
                        : Lang.get(p, Lang.Key.SUMMARY_KILL_PLURAL);
                p.sendMessage("  §f" + tb.getName() + " §7(" + ag + "§7) — §e" + k + " " + killWord);
            }
            p.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }
    }

    public void stopGame() {
        cancelTimer(); cancelCapture(); cancelCountingTimer();
        state = GameState.WAITING;
        UbuHUD.reset(this); TabManager.resetTab(this);
        for (Player p : getAllPlayers()) {
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            AbilityManager.clearCooldowns(p.getUniqueId());
        }
        cleanupRoundBlocks();
        reset();
        broadcast(Lang.Key.GAME_STOPPED);

        GameRegistry.removeInstance(instanceId);
    }

    public void onPlayerDeath(Player player) {
        if (state != GameState.ROUND_ACTIVE) return;
        if (!playerDataMap.containsKey(player.getUniqueId())) return;
        if (deadPlayers.contains(player.getUniqueId())) return;

        if (banosPlayers.containsKey(player.getName())) {
            banosPlayers.remove(player.getName());
            fr.sannoxx.ubuwool.ability.LolitaAbilities.playersInBanos.remove(player.getName());
            for (String otherName : new java.util.ArrayList<>(banosPlayers.keySet())) {
                Location otherOld = banosPlayers.get(otherName);
                banosPlayers.remove(otherName);
                fr.sannoxx.ubuwool.ability.LolitaAbilities.playersInBanos.remove(otherName);
                Player other = Bukkit.getPlayerExact(otherName);
                if (other != null) {
                    other.teleport(otherOld);
                    other.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
                    other.removePotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE);
                    other.sendMessage(Lang.get(other, Lang.Key.BANOS_RETURN_END));
                }
            }
        }

        String pName = player.getName();
        if (fr.sannoxx.ubuwool.ability.CarlosAbilities.pendingRevive.containsKey(pName)) {
            ItemStack[] savedInv   = fr.sannoxx.ubuwool.ability.CarlosAbilities.pendingReviveInventory.get(pName);
            ItemStack[] savedArmor = fr.sannoxx.ubuwool.ability.CarlosAbilities.pendingReviveArmor.get(pName);
            fr.sannoxx.ubuwool.ability.CarlosAbilities.clearRevive(pName);
            player.setHealth(player.getMaxHealth());

            final ItemStack[] finalInv   = savedInv;
            final ItemStack[] finalArmor = savedArmor;
            UbuWool.getInstance().getServer().getScheduler().runTask(UbuWool.getInstance(), () -> {
                player.getInventory().clear();
                if (finalInv != null) {
                    for (int i = 0; i < finalInv.length && i < player.getInventory().getSize(); i++)
                        player.getInventory().setItem(i, finalInv[i]);
                }
                if (finalArmor != null) player.getInventory().setArmorContents(finalArmor);
            });

            broadcastFormatted(Lang.Key.CARLOS_REVIVE, player.getName());
            fr.sannoxx.ubuwool.ability.CarlosAbilities.startReviveTimer(player);
            getAllPlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f));
            return;
        }

        CarlosAbilities.cancelReviveTimer(player.getUniqueId());
        deadPlayers.add(player.getUniqueId());

        PlayerData victimData = playerDataMap.get(player.getUniqueId());
        if (victimData != null) victimData.addDeath();

        Player killer = null;
        UUID killerUuid = lastDamager.get(player.getUniqueId());
        if (killerUuid != null) killer = Bukkit.getPlayer(killerUuid);

        if (killer != null) {
            PlayerData killerData = playerDataMap.get(killer.getUniqueId());
            String agentName  = killerData != null && killerData.agent != null ? killerData.agent.getName() : null;
            String agentColor = killerData != null && killerData.agent != null ? killerData.agent.getColor() : "§7";
            DeathRecap.record(player, killer, agentName, agentColor, isRedTeam(killer));
        } else {
            DeathRecap.record(player, null, null, null, false);
        }

        final DeathRecap.RecapData recapSnapshot = DeathRecap.getSnapshot(player.getUniqueId());

        UbuWool.getInstance().getServer().getScheduler().runTask(UbuWool.getInstance(), () -> {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.setHealth(player.getMaxHealth());
            DeathRecap.sendFromSnapshot(player, recapSnapshot);
        });
        player.sendMessage(Lang.get(player, Lang.Key.ELIMINATED));

        lastDamager.remove(player.getUniqueId());
        PlayerStats.recordDeath(player.getUniqueId());

        if (killer != null) {
            PlayerData killerData = playerDataMap.get(killer.getUniqueId());
            if (killerData != null) {
                killerData.addKill(getKillUbus());
                updateUltimateItem(killer, killerData);

                String killerAgent = killerData.agent != null ? killerData.agent.getName() : "?";
                PlayerStats.recordKill(killer.getUniqueId(), killerAgent, activeMapName);

                if (victimData != null && !victimData.isUltimateReady()) {
                    victimData.ultimateKills++;
                    updateUltimateItem(player, victimData);
                    player.sendMessage(Lang.get(player, Lang.Key.STAR_EARNED_DEATH));
                }
                boolean killerRed = isRedTeam(killer), victimRed = isRedTeam(player);
                String kc = (killerRed ? "§c" : "§9") + killer.getName();
                String vc = (victimRed ? "§c" : "§9") + player.getName();
                for (Player p : getAllPlayers())
                    p.sendMessage(Lang.get(p, Lang.Key.KILL_MESSAGE, kc, vc, getKillUbus()));
                getAllPlayers().forEach(p ->
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f));
            }
        }

        TabManager.updateTab(this);

        checkTeamElimination();
    }

    private void checkTeamElimination() {
        long redDead  = teamRed.stream().filter(deadPlayers::contains).count();
        long blueDead = teamBlue.stream().filter(deadPlayers::contains).count();
        if (redDead  >= teamRed.size())  endRound(false);
        else if (blueDead >= teamBlue.size()) endRound(true);
    }

    private static ItemStack makeUnbreakable(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return stack;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stack.setItemMeta(meta);
        return stack;
    }

    public void giveKit(Player player, boolean isRed) {
        AbilityManager.clearAllCooldowns(player);
        player.getInventory().clear();

        int rgb = isRed ? 0xAA0000 : 0x0000AA;
        ItemStack boots      = new ItemStack(Material.LEATHER_BOOTS);
        ItemStack leggings   = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        for (ItemStack armor : new ItemStack[]{boots, leggings, chestplate}) {
            LeatherArmorMeta m = (LeatherArmorMeta) armor.getItemMeta();
            m.setColor(Color.fromRGB(rgb));
            m.setUnbreakable(true);
            m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            armor.setItemMeta(m);
        }
        player.getInventory().setBoots(boots);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setChestplate(chestplate);

        player.getInventory().addItem(makeUnbreakable(new ItemStack(Material.WOODEN_SWORD)));
        player.getInventory().addItem(makeUnbreakable(new ItemStack(Material.STONE_PICKAXE)));
        player.getInventory().addItem(new ItemStack(
                isRed ? Material.RED_WOOL : Material.BLUE_WOOL, getStartingWool()));

        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null && data.agent != null) {
            switch (data.agent.getName().toLowerCase()) {
                case "sembol" -> {
                    addAbilityItem(player, Material.BLAZE_ROD,    Lang.get(player, Lang.Key.ITEM_SEMBOL_C1));
                    addAbilityItem(player, Material.MAGMA_CREAM,  Lang.get(player, Lang.Key.ITEM_SEMBOL_C2));
                }
                case "fantom" -> {
                    addAbilityItem(player, Material.TURTLE_SCUTE, Lang.get(player, Lang.Key.ITEM_FANTOM_C1));
                    addAbilityItem(player, Material.ACACIA_DOOR,  Lang.get(player, Lang.Key.ITEM_FANTOM_C2));
                }
                case "gargamel" -> {
                    addAbilityItem(player, Material.GOLD_NUGGET,  Lang.get(player, Lang.Key.ITEM_GARGAMEL_C1));
                    addAbilityItem(player, Material.COPPER_INGOT, Lang.get(player, Lang.Key.ITEM_GARGAMEL_C2));
                }
                case "horcus" -> {
                    ItemStack bow = new ItemStack(Material.BOW);
                    ItemMeta bm = bow.getItemMeta();
                    bm.setDisplayName(Lang.get(player, Lang.Key.ITEM_HORCUS_C1));
                    bm.setUnbreakable(true);
                    bm.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                    bow.setItemMeta(bm);
                    AbilityManager.addGlow(bow);
                    player.getInventory().addItem(bow);
                    addAbilityItem(player, Material.SPONGE, Lang.get(player, Lang.Key.ITEM_HORCUS_C2));
                }
                case "bambouvore" -> {
                    addAbilityItem(player, Material.BAMBOO,   Lang.get(player, Lang.Key.ITEM_BAMBOUVORE_C1));
                    addAbilityItem(player, Material.EMERALD,  Lang.get(player, Lang.Key.ITEM_BAMBOUVORE_C2));
                }
                case "lolita" -> {
                    addAbilityItem(player, Material.BONE,  Lang.get(player, Lang.Key.ITEM_LOLITA_C1));
                    addAbilityItem(player, Material.FLINT, Lang.get(player, Lang.Key.ITEM_LOLITA_C2));
                }
                case "asky" -> {
                    addAbilityItem(player, Material.SWEET_BERRIES,   Lang.get(player, Lang.Key.ITEM_ASKY_C1));
                    addAbilityItem(player, Material.MUSIC_DISC_WARD, Lang.get(player, Lang.Key.ITEM_ASKY_C2));
                }
                case "carlos" -> {
                    addAbilityItem(player, Material.REDSTONE,       Lang.get(player, Lang.Key.ITEM_CARLOS_C1));
                    addAbilityItem(player, Material.RAW_IRON_BLOCK, Lang.get(player, Lang.Key.ITEM_CARLOS_C2));
                }
                case "larok" -> {
                    addAbilityItem(player, Material.SUGAR,      Lang.get(player, Lang.Key.ITEM_LAROK_C1));
                    addAbilityItem(player, Material.BREEZE_ROD, Lang.get(player, Lang.Key.ITEM_LAROK_C2));
                }
                case "ticksuspicious" -> {
                    addAbilityItem(player, Material.TNT,      Lang.get(player, Lang.Key.ITEM_TICKSUSPICIOUS_C1));
                    addAbilityItem(player, Material.MINECART, Lang.get(player, Lang.Key.ITEM_TICKSUSPICIOUS_C2));
                }
                case "mascord" -> {
                    addAbilityItem(player, Material.PHANTOM_MEMBRANE, Lang.get(player, Lang.Key.ITEM_MASCORD_C1));
                    addAbilityItem(player, Material.HONEYCOMB,        Lang.get(player, Lang.Key.ITEM_MASCORD_C2));
                }
                case "hijab" -> {
                    addAbilityItem(player, Material.RABBIT_HIDE,  Lang.get(player, Lang.Key.ITEM_HIJAB_C1));
                    addAbilityItem(player, Material.BLAZE_POWDER, Lang.get(player, Lang.Key.ITEM_HIJAB_C2));
                    HijabAbilities.hatMode.put(player.getName(), false);
                    player.getInventory().setHelmet(null);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 99999, 0, false, false));
                }
                case "ilargia" -> {
                    addAbilityItem(player, Material.RABBIT_FOOT, Lang.get(player, Lang.Key.ITEM_ILARGIA_C1));
                    addAbilityItem(player, Material.CAMPFIRE,     Lang.get(player, Lang.Key.ITEM_ILARGIA_C2));
                }
                case "gekko" -> {
                    addAbilityItem(player, Material.PUFFERFISH,    Lang.get(player, Lang.Key.ITEM_GEKKO_C1));
                    addAbilityItem(player, Material.REDSTONE_LAMP, Lang.get(player, Lang.Key.ITEM_GEKKO_C2));
                }
                case "doma" -> {
                    addAbilityItem(player, Material.ECHO_SHARD,    Lang.get(player, Lang.Key.ITEM_DOMA_C1));
                    addAbilityItem(player, Material.BLUE_ICE, Lang.get(player, Lang.Key.ITEM_DOMA_C2));
                }
            }
            player.getInventory().addItem(buildUltimate(player, data));
        }
    }

    private void addAbilityItem(Player player, Material mat, String name) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        AbilityManager.addGlow(stack);
        player.getInventory().addItem(stack);
    }

    private void applyShopPurchases(Player player) {
        Map<String, Integer> purchases = BuyMenu.roundPurchases
                .getOrDefault(instanceId + ":" + player.getName(), new HashMap<>());
        boolean isRed = isRedTeam(player);

        int apples = purchases.getOrDefault("apple", 0);
        for (int i = 0; i < apples; i++)
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));

        if (purchases.getOrDefault("pickaxe", 0) >= 1) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s != null && s.getType() == Material.STONE_PICKAXE) {
                    ItemStack iron = makeUnbreakable(new ItemStack(Material.IRON_PICKAXE));
                    player.getInventory().setItem(i, iron); break;
                }
            }
        }
        int prot = purchases.getOrDefault("prot", 0);
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int j = 0; j < Math.min(prot, armor.length); j++) {
            if (armor[j] != null && armor[j].getType() != Material.AIR)
                armor[j].addUnsafeEnchantment(Enchantment.PROTECTION, 1);
        }
        if (purchases.getOrDefault("sharp", 0) > 0) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s != null && s.getType().name().endsWith("_SWORD")) {
                    s.addUnsafeEnchantment(Enchantment.SHARPNESS, 1); break;
                }
            }
        }
        if (purchases.getOrDefault("pickaxe2", 0) >= 1) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s != null && s.getType() == Material.IRON_PICKAXE) {
                    ItemStack diamond = makeUnbreakable(new ItemStack(Material.DIAMOND_PICKAXE));
                    player.getInventory().setItem(i, diamond); break;
                }
            }
        }
        int shears = purchases.getOrDefault("shears", 0);
        for (int i = 0; i < shears; i++) player.getInventory().addItem(makeUnbreakable(new ItemStack(Material.SHEARS)));
        int wool = purchases.getOrDefault("wool", 0);
        if (wool > 0) player.getInventory().addItem(new ItemStack(
                isRed ? Material.RED_WOOL : Material.BLUE_WOOL, wool * 4));
        int absorb = purchases.getOrDefault("absorb", 0);
        if (absorb > 0) player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION, Integer.MAX_VALUE, absorb - 1, false, false));
    }

    public ItemStack buildUltimate(Player player, PlayerData data) {
        ItemStack ult = new ItemStack(Material.NETHER_STAR);
        int required = data.agent != null ? data.agent.getUltimateKillsRequired() : 5;
        int kills = Math.min(data.ultimateKills, required);
        String stars = "§e★".repeat(kills) + "§7☆".repeat(required - kills);
        ItemMeta meta = ult.getItemMeta();
        meta.setDisplayName("§6§lUbultimate §7[" + stars + "§7]");
        ult.setItemMeta(meta);
        if (data.isUltimateReady()) AbilityManager.addGlow(ult);
        return ult;
    }

    public void updateUltimateItem(Player player, PlayerData data) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType() != Material.NETHER_STAR) continue;
            int required = data.agent != null ? data.agent.getUltimateKillsRequired() : 5;
            int kills = Math.min(data.ultimateKills, required);
            String stars = "§e★".repeat(kills) + "§7☆".repeat(required - kills);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("§6§lUbultimate §7[" + stars + "§7]");
            stack.setItemMeta(meta);
            stack.removeEnchantment(Enchantment.UNBREAKING);
            if (data.isUltimateReady()) {
                AbilityManager.addGlow(stack);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
            }
            break;
        }
    }

    public void checkCenterCapture() {
        if (state != GameState.ROUND_ACTIVE) return;
        List<Location> center = getActiveCenterBlocks();
        if (center.isEmpty()) return;
        int redCount = 0, blueCount = 0;
        for (Location loc : center) {
            Material m = loc.getBlock().getType();
            if (m == Material.RED_WOOL)       redCount++;
            else if (m == Material.BLUE_WOOL) blueCount++;
        }
        if (redCount == center.size()) {
            getTeamRedPlayers().forEach(p -> addUbus(p, getCenterCaptureUbus()));
            broadcast(Lang.Key.CENTRE_CAPTURE_RED);
            getAllPlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f));
            endRound(true);
        } else if (blueCount == center.size()) {
            getTeamBluePlayers().forEach(p -> addUbus(p, getCenterCaptureUbus()));
            broadcast(Lang.Key.CENTRE_CAPTURE_BLUE);
            getAllPlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f));
            endRound(false);
        }
    }

    public void resetCenterBlocks() {
        List<Location> center = getActiveCenterBlocks();
        Random rng = new Random();
        for (Location loc : center)
            loc.getBlock().setType(rng.nextBoolean() ? Material.DIORITE : Material.IRON_BLOCK);
    }

    private void cleanWool() {
        MapConfig.UbuMap map = MapConfig.getSelectedMap();
        if (map == null) return;

        World world = Bukkit.getWorlds().getFirst();
        int r  = map.woolCleanRadius;
        int cx = map.woolCleanCenterX;
        int cy = map.woolCleanCenterY;
        int cz = map.woolCleanCenterZ;

        Set<Material> toClean = Set.of(
                Material.RED_WOOL, Material.BLUE_WOOL, Material.GRAVEL,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.PINK_WOOL, Material.CHERRY_LEAVES, Material.FIRE,
                Material.BAMBOO_PLANKS, Material.JUNGLE_FENCE
        );
        for (int x = cx-r; x <= cx+r; x++)
            for (int y = cy-40; y <= cy+20; y++)
                for (int z = cz-r; z <= cz+r; z++)
                    if (toClean.contains(world.getBlockAt(x, y, z).getType()))
                        world.getBlockAt(x, y, z).setType(Material.AIR);
    }

    public void addUbus(Player player, int amount) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.ubus += amount;
            player.sendMessage(Lang.get(player, Lang.Key.UBUS_GAINED, amount, data.ubus));
        }
    }

    public void broadcast(Lang.Key key) {
        for (Player p : getAllPlayers()) p.sendMessage(Lang.get(p, key));
    }

    public void broadcastFormatted(Lang.Key key, Object... args) {
        for (Player p : getAllPlayers()) p.sendMessage(Lang.get(p, key, args));
    }

    private void startCaptureChecker() {
        cancelCapture();
        captureTask = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), this::checkCenterCapture, 10L, 10L)
                .getTaskId();
    }

    private void startCountingTimer() {
        cancelCountingTimer();
        countingTask = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), () -> {
                    if (state != GameState.ROUND_ACTIVE) return;
                    UbuHUD.update(this, scoreRed, scoreBlue, ++roundSeconds, Lang.Key.HUD_ROUND, currentRound);
                }, 20L, 20L).getTaskId();
    }

    private void startTimer(int seconds, Lang.Key phaseKey, Runnable onFinish) {
        cancelTimer();
        timerTask = UbuWool.getInstance().getServer().getScheduler()
                .runTaskTimer(UbuWool.getInstance(), new Runnable() {
                    int sec = seconds;
                    public void run() {
                        if (sec <= 0) { cancelTimer(); onFinish.run(); return; }
                        UbuHUD.update(GameManager.this, scoreRed, scoreBlue, sec--, phaseKey);
                    }
                }, 0L, 20L).getTaskId();
    }

    private void cancelTimer() {
        if (timerTask != -1) { Bukkit.getScheduler().cancelTask(timerTask); timerTask = -1; }
    }
    private void cancelCapture() {
        if (captureTask != -1) { Bukkit.getScheduler().cancelTask(captureTask); captureTask = -1; }
    }
    private void cancelCountingTimer() {
        if (countingTask != -1) { Bukkit.getScheduler().cancelTask(countingTask); countingTask = -1; }
    }

    public void reset() {
        teamRed.clear(); teamBlue.clear(); playerDataMap.clear(); deadPlayers.clear();
        scoreRed = 0; scoreBlue = 0; currentRound = 0; roundSeconds = 0;
        state = GameState.WAITING;
        activeMapName = null;
        cancelCapture(); cancelTimer(); cancelCountingTimer();
        fantomAnvils.clear(); fantomFallingBlocks.clear();
        horcusSponges.clear(); bambouvoreWalls.clear(); askyJukeboxes.clear();
        banosPlayers.clear();
        confirmedTeam.clear();
        activeRockets.clear(); rocketLastPos.clear();
        BuyMenu.resetPurchases(this);
        fr.sannoxx.ubuwool.menu.MapVoteMenu.resetVotes();
        MapConfig.setSelectedMap(null);
        roundHistory.clear(); lastDamager.clear();
        CarlosAbilities.pendingRevive.clear();
        StarManager.reset(this);
        activeCenterBlocks = null;

        AbilityDispatcher.resetAllRounds();
        AbilityStateManager.resetAll();
        DeathRecap.clearAll();

        LolitaAbilities.resetRound();
        HorcusAbilities.resetRound();
        HorcusAbilities.spongePositions.clear();
        HijabAbilities.resetRound(); IlargiaAbilities.resetRound();
        GekkoAbilities.resetRound(); TicksuspiciousAbilities.resetRound();
        MascordAbilities.resetRound(); CarlosAbilities.resetRound();
    }
}