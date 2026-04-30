package fr.sannoxx.ubutag.listener;

import fr.sannoxx.ubutag.UbuTag;
import fr.sannoxx.ubutag.arena.Arena;
import fr.sannoxx.ubutag.arena.ArenaState;
import fr.sannoxx.ubutag.util.ItemUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Gère tous les événements Bukkit liés à UbuTag. */
public class GameListener implements Listener {

    private final UbuTag plugin;

    public GameListener(UbuTag plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.stats().touchName(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Arena a = plugin.arenas().findArenaOf(p);
        if (a != null) a.handleQuit(p, true);
    }

    /** Tag par clic gauche (attaque mêlée). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getEntity() instanceof Player target)) return;

        Arena a = plugin.arenas().findArenaOf(damager);
        Arena b = plugin.arenas().findArenaOf(target);
        if (a == null || b == null) return;
        if (a != b) { e.setCancelled(true); return; }
        if (a.getState() != ArenaState.IN_GAME) {
            // bloque le PvP en lobby
            e.setCancelled(true);
            return;
        }
        // pas de dégâts en jeu, juste passer la TNT
        e.setCancelled(true);
        e.setDamage(0);
        a.tryPassTag(damager, target);
    }

    /** Tag par clic droit sur un joueur. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Player damager = e.getPlayer();
        Entity ent = e.getRightClicked();
        if (!(ent instanceof Player target)) return;
        Arena a = plugin.arenas().findArenaOf(damager);
        if (a == null) return;
        if (a.getState() != ArenaState.IN_GAME) { e.setCancelled(true); return; }
        if (!a.isInArena(target.getUniqueId())) return;
        e.setCancelled(true);
        a.tryPassTag(damager, target);
    }

    /** Empêche tout dégâts hors mêlée (chute, feu, ...). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Arena a = plugin.arenas().findArenaOf(p);
        if (a == null) return;
        // les seuls dégâts viables sont via EntityDamageByEntityEvent (déjà cancel = pas de dégâts)
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Arena a = plugin.arenas().findArenaOf(e.getPlayer());
        if (a == null) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Arena a = plugin.arenas().findArenaOf(e.getPlayer());
        if (a == null) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Arena a = plugin.arenas().findArenaOf(p);
        if (a == null) return;
        e.setCancelled(true);
        e.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Arena a = plugin.arenas().findArenaOf(e.getPlayer());
        if (a == null) return;
        // Empêche de jeter la TNT
        if (ItemUtil.isTagItem(plugin, e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Arena a = plugin.arenas().findArenaOf(e.getPlayer());
        if (a == null) return;
        // Empêche d'utiliser la TNT comme un bloc / item normal
        if (e.getItem() != null && ItemUtil.isTagItem(plugin, e.getItem())) {
            e.setCancelled(true);
        }
    }

    /** Pas d'explosion réelle (les "explosions" du round sont déclenchées via createExplosion). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        // par défaut on garde les dégâts blocs si l'arène l'autorise ; les
        // explosions volontaires sont via createExplosion(...) avec setBlockDamage
        e.blockList().clear();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        // Au cas où un joueur meurt malgré nos cancels (ex. /kill), on le sort
        // proprement de l'arène pour éviter un état incohérent.
        Player p = e.getEntity();
        Arena a = plugin.arenas().findArenaOf(p);
        if (a == null) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Arena a = plugin.arenas().findArenaOf(p);
        if (a == null) return;
        if (a.getLobby() != null) e.setRespawnLocation(a.getLobby());
    }
}
