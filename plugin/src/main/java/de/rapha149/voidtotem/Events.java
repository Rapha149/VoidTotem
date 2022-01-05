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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        if (config.patchKillCommand && player.getLocation().getY() >= wrapper.getDownwardHeightLimit(player.getWorld()))
            return;

        ItemStack usedItem = null;
        PlayerInventory inv = player.getInventory();
        if (config.item.customRecipe) {
            if (!config.item.valid)
                return;

            if (config.item.hasToBeInHand) {
                boolean mainHand = wrapper.hasIdentifier(inv.getItemInMainHand());
                if (!mainHand && !wrapper.hasIdentifier(inv.getItemInOffHand()))
                    return;
                usedItem = mainHand ? inv.getItemInMainHand() : inv.getItemInOffHand();
            } else {
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && wrapper.hasIdentifier(item)) {
                        usedItem = item;
                        break;
                    }
                }
                if (usedItem == null)
                    return;
            }
        } else {
            Material mat = Material.TOTEM_OF_UNDYING;
            if (config.item.hasToBeInHand) {
                boolean mainHand = inv.getItemInMainHand().getType() == mat;
                if (!mainHand && inv.getItemInOffHand().getType() != mat)
                    return;
                usedItem = mainHand ? inv.getItemInMainHand() : inv.getItemInOffHand();
            } else {
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() == mat) {
                        usedItem = item;
                        break;
                    }
                }
                if (usedItem == null)
                    return;
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
                    if (distanceStack.size() >= config.randomization.distanceStack || i + 1 >= config.searchDistance
                        || (!config.randomization.randomizeZeroDistance && i == 0)) {
                        Collections.shuffle(distanceStack);
                        distances.addAll(distanceStack);
                        distanceStack.clear();
                    }
                }
            } else {
                boolean randomizeZero = config.randomization.randomizeZeroDistance;
                for (int i = (randomizeZero ? 0 : 1); i < config.searchDistance; i++)
                    distances.add(i);
                Collections.shuffle(distances);
                if (!randomizeZero)
                    distances.add(0, 0);
            }
        } else {
            for (int i = 0; i < config.searchDistance; i++)
                distances.add(i);
        }

        for (Integer distance : distances) {
            boolean found = false;
            List<int[]> blocks = new ArrayList<>();
            if (distance > 0) {
                int x1 = x - distance, z1 = z - distance, x2 = x + distance, z2 = z + distance, cX = x1, cZ = z1;
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
            } else
                blocks.add(new int[]{x, z});

            for (int[] coords : blocks) {
                Block block = wrapper.getHighestEmptyBlockAt(world, coords[0], coords[1]);
                if (block.getRelative(BlockFace.DOWN).getType().isSolid() && wrapper.isPassable(block) &&
                    wrapper.isPassable(block.getRelative(BlockFace.UP))) {
                    Location newLoc = block.getLocation().add(0.5, 0, 0.5);
                    newLoc.setYaw(player.getLocation().getYaw());
                    newLoc.setPitch(player.getLocation().getPitch());

                    event.setCancelled(true);
                    if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                        usedItem.setAmount(usedItem.getAmount() - 1);
                    player.updateInventory();

                    boolean removeExisting = config.effects.removeExistingEffects;
                    List<Integer> effectIds = removeExisting ? null : config.effects.list.stream().map(effectData -> effectData.id).collect(Collectors.toList());
                    player.getActivePotionEffects().forEach(effect -> {
                        if (removeExisting || effectIds.contains(effect.getType().getId()))
                            player.removePotionEffect(effect.getType());
                    });

                    player.setGliding(false);
                    player.setFallDistance(0);
                    player.teleport(newLoc);
                    player.setHealth(Math.min(Math.max(health, 0) + 0.5, 20));

                    player.addPotionEffects(config.effects.list.stream().map(effectData -> {
                        PotionEffectType type = PotionEffectType.getById(effectData.id);
                        return type != null ? new PotionEffect(type, effectData.duration * 20, effectData.amplifier) : null;
                    }).filter(Objects::nonNull).collect(Collectors.toList()));

                    if (config.effects.restoreFoodLevel) {
                        player.setFoodLevel(20);
                        player.setSaturation(20F);
                        player.setExhaustion(0F);
                    }

                    Runnable totemAnimation = () -> {
                        if (config.animation.totemAnimation)
                            player.playEffect(EntityEffect.TOTEM_RESURRECT);

                        /*if (config.animation.totemParticles) {
                            AtomicReference<BukkitTask> task = new AtomicReference<>();
                            task.set(Bukkit.getScheduler().runTaskTimer(plugin, () -> new Runnable() {
                                int count = 0;

                                @Override
                                public void run() {
                                    VoidTotem.getInstance().getLogger().info("Test");
                                    count++;
                                    if (count > 7) {
                                        task.get().cancel();
                                        return;
                                    }
                                    player.spawnParticle(Particle.TOTEM, newLoc, 60, 0, 0, 0, 0.4);
                                }
                            }, 0, 5));
                        }*/
                    };

                    Bukkit.getScheduler().runTask(VoidTotem.getInstance(), () -> {
                        if (config.animation.teleportParticles)
                            world.spawnParticle(Particle.PORTAL, newLoc.clone().add(0, 1, 0), 150, 0.3, 0.4, 0.3, 0.1);

                        if (config.animation.teleportSound) {
                            world.playSound(newLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1F, 1F);
                            Bukkit.getScheduler().runTaskLater(plugin, totemAnimation, 6);
                        } else
                            totemAnimation.run();
                    });

                    found = true;
                    break;
                }
            }

            if (found)
                break;
        }
    }
}
