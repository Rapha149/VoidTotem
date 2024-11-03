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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static de.rapha149.voidtotem.Messages.getMessage;

public final class VoidTotem extends JavaPlugin {

    private static VoidTotem instance;

    @Override
    public void onEnable() {
        instance = this;
        Util.LOGGER = getLogger();

        String nmsVersion = getNMSVersion();
        try {
            Util.WRAPPER = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new IllegalStateException("Failed to load support for server version \"" + nmsVersion + "\"");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("VoidTotem does not fully support the server version \"" + nmsVersion + "\"");
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
            String version = Updates.getAvailableVersion();
            if (version != null) {
                if (version.isEmpty())
                    getLogger().info(getMessage("plugin.up_to_date"));
                else {
                    for (String line : getMessage("plugin.outdated").split("\n"))
                        getLogger().warning(line.replace("%version%", version).replace("%url%", Updates.SPIGOT_URL));
                }
            }
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

    private String getNMSVersion() {
        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        if (craftBukkitPackage.contains("v"))
            return craftBukkitPackage.split("\\.")[3].substring(1);

        // Get NMS Version from the bukkit version
        String bukkitVersion = Bukkit.getBukkitVersion();

        // Try to get NMS Version from online list (https://github.com/Rapha149/NMSVersions)
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://raw.githubusercontent.com/Rapha149/NMSVersions/main/nms-versions.json").openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            if (conn.getResponseCode() / 100 != 2)
                throw new IOException("Failed to access online NMS versions list: " + conn.getResponseCode());

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                JSONObject json = new JSONObject(br.lines().collect(Collectors.joining()));
                if (json.has(bukkitVersion))
                    return json.getString(bukkitVersion);
            }
        } catch (IOException e) {
            getLogger().warning("Can't access online NMS versions list, falling back to hardcoded NMS versions. These could be outdated.");
        }

        // separating major and minor versions, example: 1.20.4-R0.1-SNAPSHOT -> major = 20, minor = 4
        final String[] versionNumbers = bukkitVersion.split("-")[0].split("\\.");
        int major = Integer.parseInt(versionNumbers[1]);
        int minor = versionNumbers.length > 2 ? Integer.parseInt(versionNumbers[2]) : 0;

        if (major == 20 && minor >= 5) { // 1.20.5, 1.20.6
            return "1_20_R4";
        } else if (major == 21 && minor <= 1) { // 1.21, 1.21.1
            return "1_21_R1";
        } else if (major == 21 && (minor == 2 || minor == 3)) { // 1.21.2, 1.21.3
            return "1_21_R2";
        }

        throw new IllegalStateException("VoiTtotem does not support bukkit server version \"" + bukkitVersion + "\"");
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
