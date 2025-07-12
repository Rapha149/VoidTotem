package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.ItemData;
import de.rapha149.voidtotem.Metrics.AdvancedPie;
import de.rapha149.voidtotem.Metrics.DrilldownPie;
import de.rapha149.voidtotem.Metrics.SimplePie;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.rapha149.voidtotem.Messages.getMessage;

public final class VoidTotem extends JavaPlugin {

    private static final Map<String, String> VERSIONS;
    private static final String NEWEST_VERSION = "1_21_R5";

    static {
        Map<String, String> versions = new HashMap<>();
        versions.put("1.20.5", "1_20_R4");
        versions.put("1.20.6", "1_20_R4");
        versions.put("1.21.1", "1_21_R1");
        versions.put("1.21.3", "1_21_R2");
        versions.put("1.21.4", "1_21_R3");
        versions.put("1.21.5", "1_21_R4");
        versions.put("1.21.6", "1_21_R5");
        versions.put("1.21.7", "1_21_R5");
        VERSIONS = Collections.unmodifiableMap(versions);
    }

    private static VoidTotem instance;

    @Override
    public void onEnable() {
        instance = this;
        Util.LOGGER = getLogger();

        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        String nmsVersion = craftBukkitPackage.contains(".v") ? craftBukkitPackage.split("\\.")[3].substring(1) :
                VERSIONS.getOrDefault(Bukkit.getBukkitVersion().split("-")[0], NEWEST_VERSION);
        try {
            Util.WRAPPER = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new IllegalStateException("Failed to load support for server version \"" + nmsVersion + "\"");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("VoidTotem does not support the server version \"" + nmsVersion + "\"");
        }

        Messages.loadMessages();

        try {
            Config.load();
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().severe("Failed to load config.");
            getServer().getPluginManager().disablePlugin(this);
        }

        loadMetrics();

        if (Config.get().checkForUpdates) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                String version = Updates.getAvailableVersion();
                if (version != null) {
                    if (version.isEmpty())
                        getLogger().info(getMessage("plugin.up_to_date"));
                    else {
                        for (String line : getMessage("plugin.outdated").split("\n"))
                            getLogger().warning(line.replace("%version%", version).replace("%url%", Updates.SPIGOT_URL));
                    }
                }
            });
        }

        Util.loadRecipe();
        new VoidTotemCommand(getCommand("voidtotem"));
        getServer().getPluginManager().registerEvents(new Events(), this);
        getLogger().info(getMessage("plugin.enable"));
    }

    @Override
    public void onDisable() {
        getLogger().info(getMessage("plugin.disable"));
    }

    public static VoidTotem getInstance() {
        return instance;
    }

    private void loadMetrics() {
        Metrics metrics = new Metrics(this, 13802);
        metrics.addCustomChart(new DrilldownPie("check_for_updates", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();
            entry.put(getDescription().getVersion(), 1);
            map.put(String.valueOf(Config.get().checkForUpdates), entry);
            return map;
        }));
        metrics.addCustomChart(new SimplePie("totem_statistic", () -> String.valueOf(Config.get().playerData.totemStatistic)));
        metrics.addCustomChart(new DrilldownPie("advancement", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();
            entry.put(Config.get().playerData.advancement.advancement, 1);
            map.put(String.valueOf(Config.get().playerData.advancement.enabled), entry);
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("effects", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> restoreFoodLevel = new HashMap<>();
            Map<String, Integer> removeExistingEffects = new HashMap<>();
            restoreFoodLevel.put(String.valueOf(Config.get().effects.restoreFoodLevel), 1);
            removeExistingEffects.put(String.valueOf(Config.get().effects.removeExistingEffects), 1);
            map.put("Restore food level", restoreFoodLevel);
            map.put("Remove existing effects", removeExistingEffects);
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("potion_effects", () -> {
            Map<String, Integer> map = new HashMap<>();
            Config.get().effects.list.forEach(effect -> {
                PotionEffectType type = PotionEffectType.getByName(effect.name);
                if (type != null)
                    map.put(type.getName().toLowerCase(), 1);
            });
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("animation", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> teleportParticles = new HashMap<>();
            Map<String, Integer> teleportSound = new HashMap<>();
            Map<String, Integer> totemEffects = new HashMap<>();
            teleportParticles.put(String.valueOf(Config.get().animation.teleportParticles), 1);
            teleportSound.put(String.valueOf(Config.get().animation.teleportSound), 1);
            totemEffects.put(String.valueOf(Config.get().animation.totemEffects), 1);
            map.put("Teleport particles", teleportParticles);
            map.put("Teleport sound", teleportSound);
            map.put("Totem effects", totemEffects);
            return map;
        }));
        metrics.addCustomChart(new SimplePie("item_in_hand", () -> String.valueOf(Config.get().item.hasToBeInHand)));
        metrics.addCustomChart(new DrilldownPie("custom_item", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();
            Material mat = Material.getMaterial(Config.get().item.result.item.toUpperCase());
            if (mat != null)
                entry.put(mat.toString().toLowerCase(), 1);
            map.put(String.valueOf(Config.get().item.customItem), entry);
            return map;
        }));
        metrics.addCustomChart(new SimplePie("enable_recipe", () -> {
            ItemData item = Config.get().item;
            if (!item.customItem)
                return "No custom item";
            return String.valueOf(item.enableRecipe);
        }));
    }
}
