package fr.sannoxx.ubuwool.menu;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.PlayerData;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.listener.PlayerListener;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
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

/**
 * TeamMenu — avec refresh temps réel et blocage de fermeture.
 *
 * Tant qu'un joueur n'a pas choisi d'équipe, le menu se rouvre automatiquement
 * s'il tente de le fermer.
 *
 * Le refresh temps réel met à jour le contenu du menu pour tous les joueurs
 * qui l'ont ouvert dès qu'un joueur rejoint une équipe.
 */
public class TeamMenu implements InventoryHolder {

    private final Inventory inventory;
    private final GameManager gm;

    private TeamMenu(Player player, GameManager gm) {
        this.gm = gm;
        String title = Lang.get(player, Lang.Key.TEAM_MENU_TITLE)
                + " §7[#" + gm.getInstanceId() + "]";
        this.inventory = Bukkit.createInventory(this, 27, title);
        buildContents(player);
    }

    @Override
    public @NonNull Inventory getInventory() { return inventory; }

    // =========================================================
    // Ouverture
    // =========================================================

    public static void open(Player player, GameManager gm) {
        TeamMenu menu = new TeamMenu(player, gm);
        player.openInventory(menu.inventory);
    }

    // =========================================================
    // Blocage de fermeture
    // =========================================================

    /**
     * Appelé depuis PlayerListener.onInventoryClose().
     * Si le joueur n'a pas encore choisi d'équipe, on rouvre le menu 1 tick plus tard.
     */
    public static void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof TeamMenu menu)) return;

        GameManager gm = menu.gm;

        // Si la partie a avancé (AGENT_SELECT ou plus), on laisse fermer
        if (gm.state != GameManager.GameState.WAITING) return;

        // Si le joueur a déjà choisi une équipe, on laisse fermer
        boolean hasTeam = gm.teamRed.contains(player.getUniqueId())
                || gm.teamBlue.contains(player.getUniqueId());
        if (hasTeam) return;

        // Sinon, rouvrir 1 tick plus tard
        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    if (!player.isOnline()) return;
                    // Vérifier à nouveau (la partie a peut-être avancé entre-temps)
                    if (gm.state != GameManager.GameState.WAITING) return;
                    boolean stillNoTeam = !gm.teamRed.contains(player.getUniqueId())
                            && !gm.teamBlue.contains(player.getUniqueId());
                    if (stillNoTeam) {
                        open(player, gm);
                    }
                }, 1L);
    }

    // =========================================================
    // Refresh en place (sans fermer le menu)
    // =========================================================

    public static void refresh(Player player, GameManager gm) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof TeamMenu menu)) return;
        if (menu.gm.getInstanceId() != gm.getInstanceId()) return;
        menu.buildContents(player);
        player.updateInventory();
    }

    // =========================================================
    // Construction du contenu
    // =========================================================

    private void buildContents(Player player) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        // --- Équipe Rouge — slot 2 ---
        List<String> redLore = new ArrayList<>();
        for (UUID id : gm.teamRed) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) redLore.add("§f" + p.getName());
        }
        if (redLore.isEmpty()) redLore.add(Lang.get(player, Lang.Key.TEAM_NO_PLAYER));
        redLore.add("");
        redLore.add(Lang.get(player, Lang.Key.TEAM_CLICK_JOIN));
        inventory.setItem(2, item(Material.RED_WOOL,
                Lang.get(player, Lang.Key.TEAM_RED_NAME, gm.teamRed.size()),
                redLore));

        // --- Spectateur — slot 4 ---
        inventory.setItem(4, item(Material.GRAY_WOOL,
                Lang.get(player, Lang.Key.TEAM_SPECTATOR_NAME),
                Collections.singletonList(Lang.get(player, Lang.Key.TEAM_SPECTATOR_LORE))));

        // --- Équipe Bleue — slot 6 ---
        List<String> blueLore = new ArrayList<>();
        for (UUID id : gm.teamBlue) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) blueLore.add("§f" + p.getName());
        }
        if (blueLore.isEmpty()) blueLore.add(Lang.get(player, Lang.Key.TEAM_NO_PLAYER));
        blueLore.add("");
        blueLore.add(Lang.get(player, Lang.Key.TEAM_CLICK_JOIN));
        inventory.setItem(6, item(Material.BLUE_WOOL,
                Lang.get(player, Lang.Key.TEAM_BLUE_NAME, gm.teamBlue.size()),
                blueLore));
    }

    // =========================================================
    // Gestion des clics
    // =========================================================

    public static void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof TeamMenu menu)) return;

        GameManager gm = menu.gm;
        int slot = event.getRawSlot();

        switch (slot) {
            case 2 -> {
                gm.teamBlue.remove(player.getUniqueId());
                if (!gm.teamRed.contains(player.getUniqueId())) {
                    gm.teamRed.add(player.getUniqueId());
                }
                gm.playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
                player.sendMessage(Lang.get(player, Lang.Key.JOIN_RED));
                gm.getAllPlayers().forEach(p -> p.sendMessage("§c" + player.getName() + " → RED"));

                PlayerListener.refreshTeamMenus(gm);

                player.closeInventory();
                MapVoteMenu.open(player);
            }
            case 6 -> {
                gm.teamRed.remove(player.getUniqueId());
                if (!gm.teamBlue.contains(player.getUniqueId())) {
                    gm.teamBlue.add(player.getUniqueId());
                }
                gm.playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
                player.sendMessage(Lang.get(player, Lang.Key.JOIN_BLUE));
                gm.getAllPlayers().forEach(p -> p.sendMessage("§9" + player.getName() + " → BLUE"));

                PlayerListener.refreshTeamMenus(gm);

                player.closeInventory();
                MapVoteMenu.open(player);
            }
            case 4 -> {
                // Spectateur — autorisé à fermer sans équipe
                gm.teamRed.remove(player.getUniqueId());
                gm.teamBlue.remove(player.getUniqueId());
                gm.playerDataMap.remove(player.getUniqueId());
                player.sendMessage(Lang.get(player, Lang.Key.SPECTATOR));
                gm.getAllPlayers().forEach(p -> p.sendMessage("§7" + player.getName() + " est spectateur."));

                PlayerListener.refreshTeamMenus(gm);

                player.closeInventory();
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