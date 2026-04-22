package fr.sannoxx.ubuwool.menu;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.PlayerProfile;
import fr.sannoxx.ubuwool.PlayerStats;
import fr.sannoxx.ubuwool.manager.AbilityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class ProfileMenu implements InventoryHolder {

    private static final Map<UUID, Integer> currentPage  = new HashMap<>();
    private static final Map<UUID, UUID>    targetPlayer = new HashMap<>();

    private final Inventory inventory;
    private final Player viewer;
    private final Player target;
    private final int    page;

    private static final int SLOT_TAB_PROFILE    = 45;
    private static final int SLOT_TAB_STATS       = 46;
    private static final int SLOT_TAB_LEADERBOARD = 47;
    private static final int SLOT_CLOSE           = 53;

    private ProfileMenu(Player viewer, Player target, int page) {
        this.viewer = viewer;
        this.target = target;
        this.page   = page;
        String title = switch (page) {
            case 1  -> Lang.get(viewer, Lang.Key.STATS_MENU_TITLE, target.getName());
            case 2  -> Lang.get(viewer, Lang.Key.LB_TITLE);
            default -> Lang.get(viewer, Lang.Key.PROFILE_TITLE, viewer.getName());
        };
        this.inventory = Bukkit.createInventory(this, 54, title);
        build();
    }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }

    public static void open(Player viewer) {
        open(viewer, viewer, 0);
    }

    public static void openStatsPage(Player viewer, Player target) {
        open(viewer, target, 1);
    }

    public static void openLeaderboardPage(Player viewer) {
        open(viewer, viewer, 2);
    }

    private static void open(Player viewer, Player target, int page) {
        currentPage.put(viewer.getUniqueId(), page);
        targetPlayer.put(viewer.getUniqueId(), target.getUniqueId());
        ProfileMenu menu = new ProfileMenu(viewer, target, page);
        viewer.openInventory(menu.inventory);
    }

    private void build() {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        switch (page) {
            case 0 -> buildProfile();
            case 1 -> buildStats();
            case 2 -> buildLeaderboard();
        }
        buildNav();
    }

    private void buildProfile() {
        String lang = PlayerProfile.getLanguage(viewer);
        String langDisplay = lang.equals("FR") ? "Français" : "English";

        inventory.setItem(4, item(Material.PLAYER_HEAD,
                "§e§l" + viewer.getName(),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.PROFILE_LANGUAGE_LABEL) + " §f" + langDisplay,
                        "",
                        "§7" + Lang.get(viewer, Lang.Key.PROFILE_LANGUAGE_CHANGE_HINT)
                )));

        ItemStack sep = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int s : new int[]{9,10,11,12,13,14,15,16,17}) inventory.setItem(s, sep);

        ItemStack fr = item(Material.BLUE_TERRACOTTA,
                "§9§lFrançais / French",
                Arrays.asList(
                        lang.equals("FR")
                                ? "§a✔ " + Lang.get(viewer, Lang.Key.PROFILE_CURRENT_LANG)
                                : "§7" + Lang.get(viewer, Lang.Key.PROFILE_SELECT_LANG),
                        "", "§8FR"
                ));
        if (lang.equals("FR")) AbilityManager.addGlow(fr);
        inventory.setItem(20, fr);

        ItemStack enItem = item(Material.RED_TERRACOTTA,
                "§c§lEnglish / Anglais",
                Arrays.asList(
                        lang.equals("EN")
                                ? "§a✔ " + Lang.get(viewer, Lang.Key.PROFILE_CURRENT_LANG)
                                : "§7" + Lang.get(viewer, Lang.Key.PROFILE_SELECT_LANG),
                        "", "§8EN"
                ));
        if (lang.equals("EN")) AbilityManager.addGlow(enItem);
        inventory.setItem(24, enItem);

        PlayerStats.Stats stats = PlayerStats.get(viewer.getUniqueId());
        inventory.setItem(31, item(Material.BOOK,
                "§6§l" + Lang.get(viewer, Lang.Key.PROFILE_STATS_SHORTCUT),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.STATS_KILLS_LABEL) + " : §e" + stats.totalKills
                                + "  §7" + Lang.get(viewer, Lang.Key.STATS_DEATHS_LABEL) + " : §c" + stats.totalDeaths,
                        "§7KD : §a" + String.format("%.2f", stats.getKDA()),
                        "§7" + Lang.get(viewer, Lang.Key.STATS_WIN_RATE) + " : §a" + String.format("%.1f%%", stats.getWinRate()),
                        "",
                        "§e" + Lang.get(viewer, Lang.Key.PROFILE_CLICK_TO_SEE)
                )));
    }

    private void buildStats() {
        PlayerStats.Stats stats = PlayerStats.get(target.getUniqueId());

        inventory.setItem(4, item(Material.PLAYER_HEAD, "§e§l" + target.getName(),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.STATS_GAMES_PLAYED) + " : §f" + stats.gamesPlayed,
                        "§7" + Lang.get(viewer, Lang.Key.STATS_WIN_RATE) + " : §a" + String.format("%.1f%%", stats.getWinRate()),
                        "§7KD : §e" + String.format("%.2f", stats.getKDA())
                )));

        ItemStack sep = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int s : new int[]{9,10,11,12,13,14,15,16,17}) inventory.setItem(s, sep);

        inventory.setItem(19, item(Material.DIAMOND_SWORD,
                "§c" + Lang.get(viewer, Lang.Key.STATS_KILLS_DEATHS),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.STATS_KILLS_LABEL) + " : §e" + stats.totalKills,
                        "§7" + Lang.get(viewer, Lang.Key.STATS_DEATHS_LABEL) + " : §c" + stats.totalDeaths,
                        "§7KD : §a" + String.format("%.2f", stats.getKDA())
                )));

        inventory.setItem(21, item(Material.GOLDEN_APPLE,
                "§a" + Lang.get(viewer, Lang.Key.STATS_WINS_LOSSES),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.STATS_WINS_LABEL) + " : §a" + stats.totalWins,
                        "§7" + Lang.get(viewer, Lang.Key.STATS_LOSSES_LABEL) + " : §c" + stats.totalLosses,
                        "§7" + Lang.get(viewer, Lang.Key.STATS_WIN_RATE) + " : §e" + String.format("%.1f%%", stats.getWinRate())
                )));

        inventory.setItem(23, item(Material.NETHER_STAR,
                "§6" + Lang.get(viewer, Lang.Key.STATS_ROUNDS_LABEL),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.STATS_ROUNDS_WON) + " : §a" + stats.roundsWon,
                        "§7" + Lang.get(viewer, Lang.Key.STATS_ROUNDS_LOST) + " : §c" + stats.roundsLost
                )));

        inventory.setItem(25, item(Material.COMPASS,
                "§b" + Lang.get(viewer, Lang.Key.STATS_AGENTS_LABEL),
                Arrays.asList(
                        "§7" + Lang.get(viewer, Lang.Key.STATS_MOST_PLAYED) + " : §f§l" + stats.getFavoriteAgent(),
                        "§7" + Lang.get(viewer, Lang.Key.STATS_BEST_KILLS_AGENT) + " : §f§l" + stats.getBestAgent()
                )));

        List<Map.Entry<String, Integer>> killEntries = new ArrayList<>(stats.killsByAgent.entrySet());
        killEntries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int[] agentSlots = {28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        for (int i = 0; i < Math.min(killEntries.size(), agentSlots.length); i++) {
            Map.Entry<String, Integer> entry = killEntries.get(i);
            int games = stats.gamesByAgent.getOrDefault(entry.getKey(), 0);
            int wins  = stats.winsByAgent.getOrDefault(entry.getKey(), 0);
            double wr = games > 0 ? (double) wins / games * 100.0 : 0.0;
            inventory.setItem(agentSlots[i], item(Material.PAPER,
                    "§f§l" + entry.getKey(),
                    Arrays.asList(
                            "§7" + Lang.get(viewer, Lang.Key.STATS_AGENT_KILLS) + " : §e" + entry.getValue(),
                            "§7" + Lang.get(viewer, Lang.Key.STATS_AGENT_GAMES) + " : §f" + games,
                            "§7" + Lang.get(viewer, Lang.Key.STATS_AGENT_WINRATE) + " : §a" + String.format("%.0f%%", wr)
                    )));
        }
    }

    private void buildLeaderboard() {
        ItemStack sep = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int s : new int[]{9,10,11,12,13,14,15,16,17}) inventory.setItem(s, sep);

        List<PlayerStats.LeaderboardEntry> board = PlayerStats.buildLeaderboard();

        Material[] medals = {
                Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
                Material.STONE, Material.STONE, Material.STONE, Material.STONE,
                Material.STONE, Material.STONE, Material.STONE
        };
        String[] ranks = {"§6#1","§7#2","§c#3","§f#4","§f#5","§f#6","§f#7","§f#8","§f#9","§f#10"};
        int[] slots = {19,20,21,22,23,24,25,28,29,30};

        for (int i = 0; i < Math.min(board.size(), 10); i++) {
            PlayerStats.LeaderboardEntry entry = board.get(i);
            inventory.setItem(slots[i], item(medals[i],
                    ranks[i] + " §f§l" + entry.playerName,
                    Arrays.asList(
                            "§7" + Lang.get(viewer, Lang.Key.LB_KILLS_LABEL) + " : §e" + entry.stats.totalKills,
                            "§7" + Lang.get(viewer, Lang.Key.LB_KDA_LABEL) + " : §a" + String.format("%.2f", entry.stats.getKDA()),
                            "§7" + Lang.get(viewer, Lang.Key.LB_WINRATE_LABEL) + " : §a" + String.format("%.0f%%", entry.stats.getWinRate()),
                            "§7" + Lang.get(viewer, Lang.Key.LB_FAV_AGENT_LABEL) + " : §f" + entry.stats.getFavoriteAgent()
                    )));
        }

        if (board.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER,
                    "§c" + Lang.get(viewer, Lang.Key.LB_NO_DATA),
                    Collections.singletonList("§7" + Lang.get(viewer, Lang.Key.LB_NO_DATA_LORE))));
        }
    }

    private void buildNav() {
        // Onglet Profil
        inventory.setItem(SLOT_TAB_PROFILE, item(
                page == 0 ? Material.LIME_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE,
                (page == 0 ? "§a§l▶ " : "§7") + Lang.get(viewer, Lang.Key.NAV_PROFILE),
                Collections.singletonList(page == 0
                        ? "§a" + Lang.get(viewer, Lang.Key.NAV_CURRENT_PAGE)
                        : "§7" + Lang.get(viewer, Lang.Key.NAV_CLICK_TO_OPEN))));

        inventory.setItem(SLOT_TAB_STATS, item(
                page == 1 ? Material.LIME_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE,
                (page == 1 ? "§a§l▶ " : "§7") + Lang.get(viewer, Lang.Key.NAV_STATS),
                Collections.singletonList(page == 1
                        ? "§a" + Lang.get(viewer, Lang.Key.NAV_CURRENT_PAGE)
                        : "§7" + Lang.get(viewer, Lang.Key.NAV_CLICK_TO_OPEN))));

        inventory.setItem(SLOT_TAB_LEADERBOARD, item(
                page == 2 ? Material.LIME_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE,
                (page == 2 ? "§a§l▶ " : "§7") + Lang.get(viewer, Lang.Key.NAV_LEADERBOARD),
                Collections.singletonList(page == 2
                        ? "§a" + Lang.get(viewer, Lang.Key.NAV_CURRENT_PAGE)
                        : "§7" + Lang.get(viewer, Lang.Key.NAV_CLICK_TO_OPEN))));

        inventory.setItem(SLOT_CLOSE, item(Material.BARRIER,
                "§c" + Lang.get(viewer, Lang.Key.NAV_CLOSE), null));
    }

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!(event.getInventory().getHolder() instanceof ProfileMenu menu)) return;

        int slot = event.getRawSlot();
        int curPage = menu.page;

        if (slot == SLOT_TAB_PROFILE && curPage != 0) { open(viewer, viewer, 0); return; }
        if (slot == SLOT_TAB_STATS   && curPage != 1) { open(viewer, viewer, 1); return; }
        if (slot == SLOT_TAB_LEADERBOARD && curPage != 2) { open(viewer, viewer, 2); return; }
        if (slot == SLOT_CLOSE) { viewer.closeInventory(); return; }

        if (curPage == 0) {
            String lang = PlayerProfile.getLanguage(viewer);
            if (slot == 20 && !lang.equals("FR")) {
                PlayerProfile.setLanguage(viewer, "FR");
                viewer.sendMessage(Lang.get(viewer, Lang.Key.PROFILE_LANGUAGE_CHANGED));
                open(viewer, viewer, 0);
            } else if (slot == 24 && !lang.equals("EN")) {
                PlayerProfile.setLanguage(viewer, "EN");
                viewer.sendMessage(Lang.get(viewer, Lang.Key.PROFILE_LANGUAGE_CHANGED));
                open(viewer, viewer, 0);
            } else if (slot == 31) {
                open(viewer, viewer, 1);
            }
        }
    }

    static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}