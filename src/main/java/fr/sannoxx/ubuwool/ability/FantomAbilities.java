package fr.sannoxx.ubuwool.ability;

import fr.sannoxx.ubuwool.Lang;
import fr.sannoxx.ubuwool.UbuWool;
import fr.sannoxx.ubuwool.manager.GameManager;
import fr.sannoxx.ubuwool.manager.GameRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

class FantomAbilityImpl implements AgentAbility {
    @Override public boolean useC1(Player p) { FantomAbilities.turtleMaster(p); return true; }
    @Override public boolean useC2(Player p, Player t) { FantomAbilities.portaPorte(p); return true; }
    @Override public boolean useUltimate(Player p) { FantomAbilities.anvilRain(p); return true; }
    @Override public long cooldownC1Ms() { return 30_000L; }
    @Override public long cooldownC2Ms() { return 30_000L; }
    @Override public void resetRound() { /* Fantom : les FallingBlocks sont nettoyés par GameManager */ }
}

public class FantomAbilities {

    public static void turtleMaster(Player player) {
        ItemStack helmet = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        meta.setDisplayName("§b§lTurtle Helmet");
        meta.addEnchant(Enchantment.THORNS, 3, true);
        helmet.setItemMeta(meta);

        ItemStack oldHelmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(helmet);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0, false, true));
        player.sendMessage(Lang.get(player, Lang.Key.MSG_FANT_C1));

        UbuWool.getInstance().getServer().getScheduler()
                .runTaskLater(UbuWool.getInstance(), () -> {
                    player.getInventory().setHelmet(oldHelmet);
                    player.removePotionEffect(PotionEffectType.RESISTANCE);
                    player.sendMessage(Lang.get(player, Lang.Key.MSG_FANT_C1_1));
                }, 200L);
    }

    public static void portaPorte(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();

        Location dest = null;
        double maxDist = 14.0;
        double step = 0.5;

        for (double d = step; d <= maxDist; d += step) {
            Location check = eyeLoc.clone().add(dir.clone().multiply(d));

            Block headBlock = check.getBlock();
            Block feetBlock = headBlock.getRelative(0, -1, 0);

            if (headBlock.getType().isSolid() || feetBlock.getType().isSolid()) {
                break;
            }

            Location candidate = feetBlock.getLocation().clone().add(0.5, 1.0, 0.5);
            dest = candidate;
        }

        if (dest == null) {
            dest = player.getLocation();
        }

        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());
        dest.setWorld(player.getWorld());
        player.teleport(dest);
        player.sendMessage(Lang.get(player, Lang.Key.MSG_FANT_C2));
    }

    public static void anvilRain(Player player) {
        GameManager gm = GameRegistry.getInstanceOf(player);
        if (gm == null) return;

        World world = player.getWorld();
        Location target = player.getEyeLocation()
                .add(player.getEyeLocation().getDirection().multiply(10));

        BlockData anvilData = Bukkit.createBlockData(Material.ANVIL);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                final int fx = x, fz = z;
                long delay = (long)(Math.random() * 10);

                UbuWool.getInstance().getServer().getScheduler()
                        .runTaskLater(UbuWool.getInstance(), () -> {
                            Location spawnPos = target.clone().add(fx, 12, fz);
                            FallingBlock falling = world.spawnFallingBlock(spawnPos, anvilData);
                            falling.setDropItem(false);
                            falling.setHurtEntities(true);
                            falling.setDamagePerBlock(2.0f);
                            falling.setMaxDamage(40);
                            falling.setVelocity(new Vector(
                                    (Math.random() - 0.5) * 0.1,
                                    -0.5,
                                    (Math.random() - 0.5) * 0.1
                            ));
                            gm.fantomFallingBlocks.add(falling.getEntityId());
                        }, delay);
            }
        }

        player.sendMessage(Lang.get(player, Lang.Key.MSG_FANT_UBULT));
        for (Player p : player.getServer().getOnlinePlayers())
            p.sendMessage(Lang.get(p, Lang.Key.MSG_FANT_UBULT_1) + player.getName() + "§7 !");
    }
}