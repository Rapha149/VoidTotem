package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.PlayerData.AdvancementData;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Events implements Listener {

    private Map<Long, Entry<Entity, Location>> teleports = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        Config config = Config.get();
        if (!config.item.customRecipe || !config.item.noNormalResurrection)
            return;

        VersionWrapper wrapper = VoidTotem.getInstance().wrapper;
        LivingEntity entity = event.getEntity();
        EntityEquipment equipment = entity.getEquipment();
        ItemStack mainHandItem = equipment.getItemInMainHand(),
                offHandItem = equipment.getItemInOffHand();
        boolean mainHandNormal = mainHandItem.getType() == Material.TOTEM_OF_UNDYING,
                offHandNormal = offHandItem.getType() == Material.TOTEM_OF_UNDYING,
                mainHandCustom = wrapper.hasIdentifier(mainHandItem),
                offHandCustom = wrapper.hasIdentifier(offHandItem);

        if (!mainHandCustom && !offHandCustom)
            return;

        if ((mainHandCustom && !offHandNormal) || (offHandCustom && !mainHandNormal)) {
            event.setCancelled(true);
            return;
        }

        if (mainHandCustom && offHandNormal) {
            equipment.setItemInOffHand(null);
            ItemStack item = mainHandItem.clone();
            Bukkit.getScheduler().runTask(VoidTotem.getInstance(), () -> equipment.setItemInMainHand(item));
        }
    }

    @EventHandler
    public void onVoidDamage(EntityDamageEvent event) {
        Config config = Config.get();
        boolean isPlayer = event.getEntity() instanceof Player;
        if (event.getCause() != DamageCause.VOID || !(event.getEntity() instanceof LivingEntity) ||
            (config.onlySavePlayers && !isPlayer)) {
            return;
        }

        LivingEntity entity = (LivingEntity) event.getEntity();
        Player player = isPlayer ? (Player) entity : null;
        VersionWrapper wrapper = VoidTotem.getInstance().wrapper;

        double health = entity.getHealth() + wrapper.getAbsorptionHearts(entity) - event.getDamage();
        if (health > config.healthTrigger)
            return;

        if (config.patchKillCommand && entity.getLocation().getY() >= wrapper.getDownwardHeightLimit(entity.getWorld()))
            return;

        ItemStack usedItem = null;
        Boolean mainHand = null;
        EntityEquipment equipment = entity.getEquipment();
        PlayerInventory inv = isPlayer ? player.getInventory() : null;
        if (config.item.customRecipe) {
            if (!config.item.result.valid)
                return;

            if (config.item.hasToBeInHand || !isPlayer) {
                mainHand = wrapper.hasIdentifier(equipment.getItemInMainHand());
                if (!mainHand && !wrapper.hasIdentifier(equipment.getItemInOffHand()))
                    return;
                usedItem = mainHand ? equipment.getItemInMainHand() : equipment.getItemInOffHand();
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
            if (config.item.hasToBeInHand || !isPlayer) {
                mainHand = equipment.getItemInMainHand().getType() == mat;
                if (!mainHand && equipment.getItemInOffHand().getType() != mat)
                    return;
                usedItem = mainHand ? equipment.getItemInMainHand() : equipment.getItemInOffHand();
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

        Location loc = entity.getLocation();
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
                    newLoc.setYaw(entity.getLocation().getYaw());
                    newLoc.setPitch(entity.getLocation().getPitch());

                    event.setCancelled(true);
                    if (!isPlayer || player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                        usedItem.setAmount(usedItem.getAmount() - 1);
                        if (isPlayer)
                            player.updateInventory();
                        else if (mainHand)
                            equipment.setItemInMainHand(usedItem);
                        else
                            equipment.setItemInOffHand(usedItem);
                    }

                    boolean removeExisting = config.effects.removeExistingEffects;
                    List<String> effectNames = removeExisting ? null : config.effects.list.stream().map(effectData -> effectData.name).collect(Collectors.toList());
                    entity.getActivePotionEffects().forEach(effect -> {
                        if (removeExisting || effectNames.contains(effect.getType().getName()))
                            entity.removePotionEffect(effect.getType());
                    });

                    entity.setGliding(false);
                    entity.setFallDistance(0);

                    if (config.forceTeleport) {
                        long time = System.currentTimeMillis();
                        teleports.put(time, new SimpleEntry<>(entity, newLoc));
                        Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), () -> teleports.remove(time), 5);
                    }
                    entity.teleport(newLoc);

                    entity.setHealth(Math.min(Math.max(health, 0) + 0.5, 20));
                    entity.addPotionEffects(config.effects.list.stream().map(effectData -> {
                        PotionEffectType type = PotionEffectType.getByName(effectData.name);
                        return type != null ? new PotionEffect(type, effectData.duration * 20, effectData.amplifier) : null;
                    }).filter(Objects::nonNull).collect(Collectors.toList()));

                    if (isPlayer) {
                        if (config.effects.restoreFoodLevel) {
                            player.setFoodLevel(20);
                            player.setSaturation(20F);
                            player.setExhaustion(0F);
                        }

                        if (config.playerData.totemStatistic)
                            player.incrementStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING);

                        AdvancementData advancementData = config.playerData.advancement;
                        if (advancementData.enabled && advancementData.valid) {
                            Advancement advancement = Bukkit.getAdvancement(advancementData.key);
                            if (advancement != null) {
                                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                                if (!progress.isDone()) {
                                    List<String> criteria = advancementData.criteria;
                                    progress.getRemainingCriteria().stream()
                                            .filter(criterion -> criteria.isEmpty() || criteria.contains(criterion))
                                            .forEach(progress::awardCriteria);
                                }
                            }
                        }
                    }

                    Runnable totemEffects = () -> {
                        if (config.animation.totemEffects)
                            entity.playEffect(EntityEffect.TOTEM_RESURRECT);
                    };

                    Bukkit.getScheduler().runTask(VoidTotem.getInstance(), () -> {
                        if (config.animation.teleportParticles)
                            world.spawnParticle(Particle.PORTAL, newLoc.clone().add(0, 1, 0), 150, 0.3, 0.4, 0.3, 0.1);

                        if (config.animation.teleportSound) {
                            world.playSound(newLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1F, 1F);
                            Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), totemEffects, 6);
                        } else
                            totemEffects.run();
                    });

                    found = true;
                    break;
                }
            }

            if (found)
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(EntityTeleportEvent event) {
        onTeleport(event, event.getEntity(), event.getTo());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.PLUGIN)
            return;
        onTeleport(event, event.getPlayer(), event.getTo());
    }

    private void onTeleport(Cancellable event, Entity entity, Location loc) {
        if (!Config.get().forceTeleport)
            return;
        teleports.values().removeIf(entry -> {
            if (entry.getKey().equals(entity) && entry.getValue().equals(loc)) {
                event.setCancelled(false);
                return true;
            }
            return false;
        });
    }
}
