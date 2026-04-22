package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.AbilityManager;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.*;

class HorcusAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { return false; } // C1 = arc, pas de capacité active
    @Override public boolean useC2(Player p, Player t) { return false; } // géré dans dispatcher
    @Override public boolean useUltimate(Player p) { HorcusAbilities.tourDeForce(p); return true; }
    @Override public long cooldownC1Ms() { return 0L; }
    @Override public long cooldownC2Ms() { return 30_000L; }
    @Override public void resetRound() { HorcusAbilities.resetRound(); }
}

public class HorcusAbilities {

    public static Map<String, Location> spongePositions = new HashMap<>();
    private static final Map<UUID, Boolean> lastRendezVousWasTpMap = new HashMap<>();
    @Deprecated
    public static boolean lastRendezVousWasTp = false;

    public static void setLastRendezVousTp(UUID uuid, boolean value) {
        lastRendezVousWasTpMap.put(uuid, value);
        lastRendezVousWasTp = value;
    }

    public static boolean wasLastRendezVousTp(UUID uuid) {
        return lastRendezVousWasTpMap.getOrDefault(uuid, false);
    }
    public static Set<UUID> headHunterArrows = new HashSet<>();

    public static void rendezVous(Player player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        Location spongePos = spongePositions.get(name);

        if (spongePos == null) {
            RayTraceResult hit = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(), player.getEyeLocation().getDirection(), 10);
            if (hit == null || hit.getHitBlock() == null) {
                player.sendMessage(Lang.get(player, Lang.Key.MSG_HORC_C2));
                setLastRendezVousTp(uuid, false);
                return;
            }
            assert hit.getHitBlockFace() != null;
            Location target = hit.getHitBlock().getRelative(hit.getHitBlockFace()).getLocation();
            if (target.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                player.sendMessage(Lang.get(player, Lang.Key.MSG_HORC_C2_1));
                setLastRendezVousTp(uuid, false);
                return;
            }
            target.getBlock().setType(Material.SPONGE);
            spongePositions.put(name, target);
            GameManager gm = GameRegistry.getInstanceOf(player);
            if (gm != null) gm.horcusSponges.add(target);
            swapRendezVousItem(player, false);
            player.sendMessage(Lang.get(player, Lang.Key.MSG_HORC_C2_2));
            setLastRendezVousTp(uuid, false);
        } else {
            if (spongePos.getBlock().getType() != Material.SPONGE) {
                spongePositions.remove(name);
                swapRendezVousItem(player, true);
                player.sendMessage(Lang.get(player, Lang.Key.MSG_HORC_C2_3));
                setLastRendezVousTp(uuid, false);
                return;
            }
            Location tp = spongePos.clone().add(0.5, 1, 0.5);
            tp.setYaw(player.getLocation().getYaw());
            tp.setPitch(player.getLocation().getPitch());
            player.teleport(tp);
            player.sendMessage(Lang.get(player, Lang.Key.MSG_HORC_C2_4));
            setLastRendezVousTp(uuid, true);
        }
    }

    public static void swapRendezVousItem(Player player, boolean toSponge) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s == null || !s.hasItemMeta() || !s.getItemMeta().hasDisplayName()) continue;
            if (!s.getItemMeta().getDisplayName().contains("C2 ∙ Rendez")) continue;
            ItemStack newItem = toSponge
                    ? new ItemStack(Material.SPONGE)
                    : new ItemStack(Material.END_PORTAL_FRAME);
            ItemMeta meta = newItem.getItemMeta();
            meta.setDisplayName("§4§lC2 ∙ Rendez-Vous");
            newItem.setItemMeta(meta);
            AbilityManager.addGlow(newItem);
            player.getInventory().setItem(i, newItem);
            break;
        }
    }

    public static void removeRendezVousItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s == null || !s.hasItemMeta() || !s.getItemMeta().hasDisplayName()) continue;
            if (!s.getItemMeta().getDisplayName().contains("C2 ∙ Rendez")) continue;
            player.getInventory().setItem(i, null);
            break;
        }
    }

    public static void tourDeForce(Player player) {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();
        meta.setDisplayName("§6§l★ Tour de Force ★");
        meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
        meta.addEnchant(Enchantment.POWER, 2, true);
        crossbow.setItemMeta(meta);
        AbilityManager.addGlow(crossbow);
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s != null && s.getType() == Material.NETHER_STAR) {
                player.getInventory().setItem(i, crossbow); break;
            }
        }
        player.getInventory().setItemInOffHand(new ItemStack(Material.SPECTRAL_ARROW, 5));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_HORC_UBULT));
    }

    public static void onHeadHunterShoot(Arrow arrow) {
        headHunterArrows.add(arrow.getUniqueId());
        UbuWool.getInstance().getServer().getScheduler().runTaskLater(UbuWool.getInstance(),
                () -> headHunterArrows.remove(arrow.getUniqueId()), 200L);
    }

    public static boolean onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return false;
        if (!headHunterArrows.contains(arrow.getUniqueId())) return false;
        if (!(event.getEntity() instanceof Player victim)) return false;
        if (!(arrow.getShooter() instanceof Player shooter)) return false;

        headHunterArrows.remove(arrow.getUniqueId());

        double arrowY = arrow.getLocation().getY();
        double eyeY   = victim.getEyeLocation().getY();
        boolean isHeadshot = arrowY >= eyeY - 0.1;

        if (isHeadshot) {
            victim.getWorld().spawnParticle(Particle.CRIT,
                    victim.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0.1);
            victim.getWorld().playSound(victim.getLocation(),
                    Sound.ENTITY_ARROW_HIT_PLAYER, 1.5f, 0.5f);
            shooter.sendMessage(Lang.get(shooter, Lang.Key.MSG_HORC_HEADSHOT, victim.getName()));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true));
            return true;
        }
        return false;
    }

    public static void resetRound() {
        headHunterArrows.clear();
        lastRendezVousWasTpMap.clear();
        lastRendezVousWasTp = false;
    }
}