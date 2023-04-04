package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.PlayerData.AdvancementData;
import de.rapha149.voidtotem.Config.SearchData.PlatformData;
import de.rapha149.voidtotem.Config.SearchData.PlatformData.DisappearData;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Events implements Listener {

    private static final double HOLOGRAM_LINE_DISTANCE = 0.25;

    private Map<Long, UUID> preventDamage = new HashMap<>();
    private Map<Long, Entry<Entity, Location>> teleports = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        Config config = Config.get();
        if (!config.item.customItem || !config.item.noNormalResurrection)
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
    public void onDamage(EntityDamageEvent event) {
        if (preventDamage.containsValue(event.getEntity().getUniqueId()))
            event.setCancelled(true);

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
        if (config.item.customItem) {
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

        Location currentLoc = entity.getLocation();
        World world = currentLoc.getWorld();
        int x = currentLoc.getBlockX(), z = currentLoc.getBlockZ();

        List<Integer> distances = new ArrayList<>();
        if (config.randomization.enabled && config.randomization.distanceStack > 0) {
            if (config.randomization.distanceStack > 1) {
                List<Integer> distanceStack = new ArrayList<>();
                for (int i = 0; i < config.search.distance; i++) {
                    distanceStack.add(i);
                    if (distanceStack.size() >= config.randomization.distanceStack || i + 1 >= config.search.distance
                        || (!config.randomization.randomizeZeroDistance && i == 0)) {
                        Collections.shuffle(distanceStack);
                        distances.addAll(distanceStack);
                        distanceStack.clear();
                    }
                }
            } else {
                boolean randomizeZero = config.randomization.randomizeZeroDistance;
                for (int i = (randomizeZero ? 0 : 1); i < config.search.distance; i++)
                    distances.add(i);
                Collections.shuffle(distances);
                if (!randomizeZero)
                    distances.add(0, 0);
            }
        } else {
            for (int i = 0; i < config.search.distance; i++)
                distances.add(i);
        }

        Location loc = null;
        for (Integer distance : distances) {
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
                    loc = block.getLocation();
                    break;
                }
            }

            if (loc != null)
                break;
        }

        if (loc == null) {
            PlatformData platform = config.search.platform;
            if (!platform.enabled || !isPlayer)
                return;

            Location l = new Location(world, x, platform.height, z);
            new Platform(player, l);
            loc = l.clone().add(0, 1, 0);
        }

        loc.add(0.5, 0, 0.5);
        loc.setYaw(entity.getLocation().getYaw());
        loc.setPitch(entity.getLocation().getPitch());

        event.setCancelled(true);
        long preventDamageTime = System.currentTimeMillis();
        preventDamage.put(preventDamageTime, entity.getUniqueId());
        Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), () -> preventDamage.remove(preventDamageTime), 5);

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
        if (!entity.isInvulnerable()) {
            entity.setInvulnerable(true);
            Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), () -> entity.setInvulnerable(false), 5);
        }

        if (config.forceTeleport) {
            long teleportTime = System.currentTimeMillis();
            teleports.put(teleportTime, new SimpleEntry<>(entity, loc));
            Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), () -> teleports.remove(teleportTime), 5);
        }
        entity.teleport(loc);

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

        Location finalLoc = loc;
        Bukkit.getScheduler().runTask(VoidTotem.getInstance(), () -> {
            if (config.animation.teleportParticles)
                world.spawnParticle(Particle.PORTAL, finalLoc.clone().add(0, 1, 0), 150, 0.3, 0.4, 0.3, 0.1);

            if (config.animation.teleportSound) {
                world.playSound(finalLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1F, 1F);
                Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), totemEffects, 6);
            } else
                totemEffects.run();
        });
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

    private class Platform implements Listener {

        private Player player;
        private Location loc;
        private Map<Block, BlockData> replacedBlocks = new HashMap<>();

        private BukkitTask hologramTask = null;
        private Map<ArmorStand, String> holograms = new HashMap<>();

        private long disappearTime = -1;
        private Vector areaMin, areaMax;

        public Platform(Player player, Location loc) {
            this.player = player;
            this.loc = loc;
            Bukkit.getPluginManager().registerEvents(this, VoidTotem.getInstance());

            PlatformData data = Config.get().search.platform;
            Material mat = Material.matchMaterial(data.block);
            if (mat == null || !mat.isSolid())
                mat = Material.COBBLESTONE;

            int size = data.size;
            for (int blockX = loc.getBlockX() - size; blockX <= loc.getBlockX() + size; blockX++) {
                for (int blockZ = loc.getBlockZ() - size; blockZ <= loc.getBlockZ() + size; blockZ++) {
                    Block block = loc.getWorld().getBlockAt(blockX, data.height, blockZ);
                    if (block.getType() != mat) {
                        replacedBlocks.put(block, block.getBlockData());
                        block.setType(mat);
                    }
                }
            }

            DisappearData disappear = data.disappear;
            if (disappear.enabled) {
                if (disappear.waitForPlayer) {
                    areaMin = new Vector(loc.getBlockX() - size - 1, data.height, loc.getBlockZ() - size - 1);
                    areaMax = new Vector(loc.getBlockX() + size + 1, data.height + 2, loc.getBlockZ() + size + 1);
                } else {
                    disappearTime = System.currentTimeMillis() / 1000 + disappear.delay;
                    scheduleDisappear(disappear.delay);
                }

                if (disappear.hologram)
                    loadHolograms();
            }
        }

        private void loadHolograms() {
            List<ArmorStand> previousArmorStands = removeHolograms(false);

            String[] lines = Messages.getMessage("platform_hologram." + (disappearTime == -1 ? "wait_for_player" : "delay")).split("\\n");
            double startOffset = 2.5 + Math.floor(lines.length / 2D) * HOLOGRAM_LINE_DISTANCE + (lines.length % 2 == 0 ? HOLOGRAM_LINE_DISTANCE / 2D : 0);
            Location currentLoc = loc.clone().add(0.5, startOffset, 0.5);

            boolean adaptingLines = false;
            for (String line : lines) {
                ArmorStand armorStand;
                if (previousArmorStands.isEmpty()) {
                    armorStand = (ArmorStand) loc.getWorld().spawnEntity(currentLoc, EntityType.ARMOR_STAND);
                    armorStand.setCustomNameVisible(true);
                    armorStand.setMarker(true);
                    armorStand.setVisible(false);
                    armorStand.setSilent(true);
                } else {
                    armorStand = previousArmorStands.remove(0);
                    armorStand.teleport(currentLoc);
                }

                if (line.contains("%time%")) {
                    adaptingLines = true;
                    armorStand.setCustomName(line.replace("%time%", String.valueOf(disappearTime - System.currentTimeMillis() / 1000)));
                } else
                    armorStand.setCustomName(line);
                holograms.put(armorStand, line);
                currentLoc.subtract(0, HOLOGRAM_LINE_DISTANCE, 0);
            }

            previousArmorStands.forEach(ArmorStand::remove);

            if (adaptingLines) {
                hologramTask = Bukkit.getScheduler().runTaskTimer(VoidTotem.getInstance(), () -> {
                    long seconds = disappearTime == -1 ? -1 : disappearTime - System.currentTimeMillis() / 1000;
                    holograms.forEach((armorStand, line) -> armorStand.setCustomName(line.replace("%time%", String.valueOf(seconds))));
                }, 20, 20);
            }
        }

        private List<ArmorStand> removeHolograms(boolean removeArmorStands) {
            List<ArmorStand> armorStands;
            if (removeArmorStands) {
                holograms.keySet().forEach(ArmorStand::remove);
                armorStands = Collections.emptyList();
            } else
                armorStands = new ArrayList<>(holograms.keySet());

            holograms.clear();
            if (hologramTask != null && !hologramTask.isCancelled())
                hologramTask.cancel();

            return armorStands;
        }

        private void scheduleDisappear(int delay) {
            Bukkit.getScheduler().runTaskLater(VoidTotem.getInstance(), () -> {
                removeHolograms(true);
                replacedBlocks.forEach((block, data) -> {
                    if (Config.get().search.platform.disappear.sound)
                        VoidTotem.getInstance().wrapper.playBreakSound(block);
                    block.setBlockData(data);
                });
                replacedBlocks.clear();
                HandlerList.unregisterAll(this);
            }, delay * 20L);
        }

        @EventHandler
        public void onBreak(BlockBreakEvent event) {
            if (!Config.get().search.platform.breakable && replacedBlocks.containsKey(event.getBlock()))
                event.setCancelled(true);
        }

        @EventHandler
        public void onMove(PlayerMoveEvent event) {
            if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
                return;
            if (disappearTime != -1 || areaMin == null || areaMax == null)
                return;

            Location to = event.getTo().getBlock().getLocation();
            if (!to.getWorld().getUID().equals(loc.getWorld().getUID()) || !to.toVector().isInAABB(areaMin, areaMax)) {
                DisappearData data = Config.get().search.platform.disappear;
                int delay = data.delay;
                disappearTime = System.currentTimeMillis() / 1000 + delay;
                scheduleDisappear(delay);

                if (data.hologram)
                    loadHolograms();
            }
        }
    }
}
