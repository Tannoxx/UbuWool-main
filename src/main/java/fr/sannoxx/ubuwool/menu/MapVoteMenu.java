package fr.sannoxx.ubuwool.menu;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import fr.sannoxx.ubuwool.manager.MapConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class MapVoteMenu implements InventoryHolder {

    /** Votes : playerName → mapName */
    public static final Map<String, String> votes = new HashMap<>();

    public static void resetVotes() { votes.clear(); }

    public static String getWinningMap() {
        List<MapConfig.UbuMap> enabled = MapConfig.getEnabledMaps();
        if (enabled.isEmpty()) return null;
        if (votes.isEmpty()) return enabled.get(new Random().nextInt(enabled.size())).name;
        Map<String, Integer> counts = new HashMap<>();
        for (String map : votes.values()) counts.merge(map, 1, Integer::sum);
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(enabled.getFirst().name);
    }

    private static final int[] MAP_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final Inventory inventory;
    private final List<MapConfig.UbuMap> maps;

    private MapVoteMenu(Player player) {
        this.maps = MapConfig.getEnabledMaps();
        this.inventory = Bukkit.createInventory(this, 36,
                Lang.get(player, Lang.Key.MAP_VOTE_MENU_TITLE));
        build(player);
    }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }

    public static void open(Player player) {
        List<MapConfig.UbuMap> enabled = MapConfig.getEnabledMaps();
        if (enabled.isEmpty()) {
            player.sendMessage(Lang.get(player, Lang.Key.NO_VALID_MAP));
            return;
        }
        MapVoteMenu menu = new MapVoteMenu(player);
        player.openInventory(menu.inventory);
    }

    // =========================================================
    // Blocage de fermeture
    // =========================================================

    /**
     * Appelé depuis PlayerListener.onInventoryClose().
     * Si le joueur n'a pas encore voté, on rouvre le menu 1 tick plus tard.
     */
    public static void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MapVoteMenu)) return;

        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        // Si la partie a déjà avancé au-delà de WAITING, on laisse fermer
        if (gm.state != GameManager.GameState.WAITING) return;

        // Si le joueur a déjà voté, on laisse fermer
        if (votes.containsKey(player.getName())) return;

        // Sinon, rouvrir 1 tick plus tard
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    if (!player.isOnline()) return;
                    if (gm.state != GameManager.GameState.WAITING) return;
                    if (!votes.containsKey(player.getName())) {
                        open(player);
                    }
                }, 1L);
    }

    // =========================================================
    // Construction
    // =========================================================

    private void build(Player player) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 36; i++) inventory.setItem(i, filler);

        Map<String, Integer> counts = new HashMap<>();
        for (String v : votes.values()) counts.merge(v, 1, Integer::sum);

        List<String> loreSummary = new ArrayList<>();
        for (MapConfig.UbuMap m : maps) {
            int c = counts.getOrDefault(m.name, 0);
            String dn = m.displayName != null ? m.displayName : m.name;
            loreSummary.add(Lang.get(player, Lang.Key.MAP_VOTE_LORE_LINE, dn, c));
        }
        inventory.setItem(4, item(Material.PAPER,
                Lang.get(player, Lang.Key.MAP_VOTE_TITLE), loreSummary));

        String myVote = votes.get(player.getName());
        for (int i = 0; i < Math.min(maps.size(), MAP_SLOTS.length); i++) {
            MapConfig.UbuMap map = maps.get(i);
            int voteCount = counts.getOrDefault(map.name, 0);
            boolean voted = map.name.equals(myVote);

            Material icon = Material.GRASS_BLOCK;
            if (map.iconBlock != null) {
                try { icon = Material.valueOf(map.iconBlock.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }
            String displayName = map.displayName != null ? map.displayName : map.name;

            inventory.setItem(MAP_SLOTS[i], item(icon,
                    (voted ? "§a§l✔ " : "§f§l") + displayName,
                    Arrays.asList(
                            Lang.get(player, Lang.Key.MAP_VOTE_COUNT, voteCount),
                            voted ? Lang.get(player, Lang.Key.MAP_VOTE_CURRENT)
                                    : Lang.get(player, Lang.Key.MAP_VOTE_CLICK)
                    )));
        }
    }

    // =========================================================
    // Gestion des clics
    // =========================================================

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MapVoteMenu menu)) return;

        int raw = event.getRawSlot();
        for (int i = 0; i < MAP_SLOTS.length; i++) {
            if (raw == MAP_SLOTS[i] && i < menu.maps.size()) {
                MapConfig.UbuMap votedMap = menu.maps.get(i);
                String displayName = votedMap.displayName != null ? votedMap.displayName : votedMap.name;
                votes.put(player.getName(), votedMap.name);
                player.sendMessage(Lang.get(player, Lang.Key.MAP_VOTE_REGISTERED, displayName));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(Lang.get(p, Lang.Key.MAP_VOTE_BROADCAST,
                            player.getName(), displayName));
                }
                player.closeInventory();

                GameManager gm = GameRegistry.getInstanceOf(player);
                if (gm != null) {
                    gm.tryStartGame();
                }
                return;
            }
        }
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
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