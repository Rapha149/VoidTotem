package de.rapha149.voidtotem;

import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Events implements Listener {

    @EventHandler
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != DamageCause.VOID || !(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        Config config = Config.get();
        VoidTotem plugin = VoidTotem.getInstance();
        VersionWrapper wrapper = plugin.wrapper;

        double health = player.getHealth() - event.getFinalDamage();
        if (health > config.healthTrigger)
            return;

        int slot = -1;
        Boolean mainHand = null;
        PlayerInventory inv = player.getInventory();
        if (config.item.customRecipe) {
            if (!config.item.valid || wrapper == null)
                return;

            if (config.item.hasToBeInHand) {
                if (!(mainHand = wrapper.hasIdentifier(inv.getItemInMainHand())) && !wrapper.hasIdentifier(inv.getItemInOffHand()))
                    return;
            } else {
                int found = -1;
                for (int i = 0; i < inv.getSize(); i++) {
                    if (wrapper.hasIdentifier(inv.getItem(i))) {
                        found = i;
                        break;
                    }
                }
                if (found == -1)
                    return;
                slot = found;
            }
        } else {
            Material mat = Material.TOTEM_OF_UNDYING;
            if (config.item.hasToBeInHand) {
                if (!(mainHand = inv.getItemInMainHand().getType() == mat) && inv.getItemInOffHand().getType() != mat)
                    return;
            } else {
                int found = -1;
                for (int i = 0; i < inv.getSize(); i++) {
                    if (inv.getItem(i).getType() == mat) {
                        found = i;
                        break;
                    }
                }
                if (found == -1)
                    return;
                slot = found;
            }
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();
        int x = loc.getBlockX(), z = loc.getBlockZ();

        List<Integer> distances = new ArrayList<>();
        if (config.randomization.enabled && config.randomization.distanceStack > 0) {
            if (config.randomization.distanceStack > 1) {
                List<Integer> distanceStack = new ArrayList<>();
                for (int i = 0; i < config.searchDistance; i++) {
                    distanceStack.add(i);
                    if (distanceStack.size() >= config.randomization.distanceStack || i + 1 >= config.searchDistance) {
                        Collections.shuffle(distanceStack);
                        distances.addAll(distanceStack);
                        distanceStack.clear();
                    }
                }
            } else {
                for (int i = 0; i < config.searchDistance; i++)
                    distances.add(i);
                Collections.shuffle(distances);
            }
        } else {
            for (int i = 0; i < config.searchDistance; i++)
                distances.add(i);
        }

        for (Integer distance : distances) {
            boolean found = false;
            int x1 = x - distance, z1 = z - distance, x2 = x + distance, z2 = z + distance, cX = x1, cZ = z1;
            List<int[]> blocks = new ArrayList<>();
            for (; cX < x2; cX++)
                blocks.add(new int[]{cX, cZ});
            for (; cZ < z2; cZ++)
                blocks.add(new int[]{cX, cZ});
            for (; cX > x1; cX--)
                blocks.add(new int[]{cX, cZ});
            for (; cZ > z1; cZ--)
                blocks.add(new int[]{cX, cZ});

            if (config.randomization.enabled)
                Collections.shuffle(blocks);

            for (int[] coords : blocks) {
                Block block = world.getHighestBlockAt(coords[0], coords[1]);
                if (block.getType().isSolid() && wrapper.isPassable(block = block.getRelative(BlockFace.UP)) &&
                    wrapper.isPassable(block.getRelative(BlockFace.UP))) {
                    Location newLoc = block.getLocation().add(0.5, 0, 0.5);
                    newLoc.setYaw(player.getLocation().getYaw());
                    newLoc.setPitch(player.getLocation().getPitch());

                    event.setCancelled(true);
                    if (mainHand != null) {
                        if (mainHand)
                            inv.setItemInMainHand(null);
                        else
                            inv.setItemInOffHand(null);
                    } else
                        inv.setItem(slot, null);
                    player.updateInventory();

                    player.setGliding(false);
                    player.setFallDistance(0);
                    player.teleport(newLoc);
                    player.setHealth(Math.max(health, 0) + 0.5);

                    if (config.effects.removeExistingEffects)
                        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
                    config.effects.list.forEach(effectData -> {
                        PotionEffectType type = PotionEffectType.getById(effectData.id);
                        if (type != null)
                            type.createEffect(effectData.duration, effectData.amplifier).apply(player);
                    });

                    if (config.effects.restoreFoodLevel) {
                        player.setFoodLevel(20);
                        player.setSaturation(20F);
                        player.setExhaustion(0F);
                    }

                    Runnable totemAnimation = () -> {
                        if (config.animation.totemAnimation)
                            player.playEffect(EntityEffect.TOTEM_RESURRECT);

                        if (config.animation.totemParticles) {
                            AtomicReference<BukkitTask> task = new AtomicReference<>();
                            task.set(Bukkit.getScheduler().runTaskTimer(plugin, () -> new Runnable() {
                                int count = 0;

                                @Override
                                public void run() {
                                    count++;
                                    if (count > 7) {
                                        task.get().cancel();
                                        return;
                                    }
                                    player.spawnParticle(Particle.TOTEM, newLoc, 60, 0, 0, 0, 0.4);
                                }
                            }, 0, 5));
                        }
                    };

                    if (config.animation.teleportParticles)
                        player.spawnParticle(Particle.PORTAL, newLoc.clone().add(0, 1, 0), 150, 0.3, 0.4, 0.3, 0.1);

                    if (config.animation.teleportSound) {
                        player.playSound(newLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1F, 1F);
                        Bukkit.getScheduler().runTaskLater(plugin, totemAnimation, 6);
                    } else
                        totemAnimation.run();

                    found = true;
                    break;
                }
            }

            if (found)
                break;
        }
    }
}
