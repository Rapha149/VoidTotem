package de.rapha149.voidtotem;

import com.google.common.collect.Streams;
import de.rapha149.voidtotem.Config.ItemData;
import de.rapha149.voidtotem.Config.ItemData.RecipeData;
import de.rapha149.voidtotem.Metrics.AdvancedPie;
import de.rapha149.voidtotem.Metrics.DrilldownPie;
import de.rapha149.voidtotem.Metrics.SimplePie;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.rapha149.voidtotem.Messages.getMessage;

public final class VoidTotem extends JavaPlugin {

    private final NamespacedKey RECIPE_KEY = new NamespacedKey(this, "void_totem");

    private static VoidTotem instance;
    public VersionWrapper wrapper;

    @Override
    public void onEnable() {
        instance = this;

        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
        try {
            wrapper = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
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

        loadRecipe();
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
        metrics.addCustomChart(new DrilldownPie("player_data", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> totemStatistic = new HashMap<>();
            Map<String, Integer> advancement = new HashMap<>();
            totemStatistic.put(String.valueOf(Config.get().playerData.totemStatistic), 1);
            advancement.put(String.valueOf(Config.get().playerData.advancement), 1);
            map.put("Totem Statistic", totemStatistic);
            map.put("Advancement", advancement);
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
                PotionEffectType type = PotionEffectType.getById(effect.id);
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
        metrics.addCustomChart(new DrilldownPie("custom_recipe", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();
            Material mat = Material.getMaterial(Config.get().item.result.item.toUpperCase());
            if (mat != null)
                entry.put(mat.toString().toLowerCase(), 1);
            map.put(String.valueOf(Config.get().item.customRecipe), entry);
            return map;
        }));
    }

    public void loadRecipe() {
        wrapper.removeRecipe(RECIPE_KEY);

        if (Streams.stream(Bukkit.recipeIterator()).anyMatch(recipe ->
                (recipe instanceof ShapelessRecipe && ((ShapelessRecipe) recipe).getKey().equals(RECIPE_KEY) ||
                 (recipe instanceof ShapedRecipe && ((ShapedRecipe) recipe).getKey().equals(RECIPE_KEY))))) {
            getLogger().warning(getMessage("old_recipe_not_removed"));
            return;
        }

        ItemData itemData = Config.get().item;
        if (itemData.customRecipe && itemData.result.valid && itemData.recipe.valid) {
            ItemStack result = itemData.result.getItemStack();
            RecipeData recipeData = itemData.recipe;
            if (!recipeData.shaped) {
                ShapelessRecipe recipe = new ShapelessRecipe(RECIPE_KEY, result);
                recipeData.shapelessIngredients.forEach(ingredient -> recipe.addIngredient(Material.getMaterial(ingredient.toUpperCase())));
                Bukkit.addRecipe(recipe);
            } else {
                ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, result);
                String[] shape = new String[recipeData.shapedIngredients.size()];
                Map<String, Character> ingredients = new HashMap<>();
                for (int i = 0; i < recipeData.shapedIngredients.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    for (String str : recipeData.shapedIngredients.get(i).split("\\|")) {
                        String ingredient = str.trim();
                        Character character = ingredients.get(ingredient);
                        if (character == null) {
                            for (int c = 'a'; c <= 'z'; c++) {
                                if (!ingredients.containsValue((char) c)) {
                                    ingredients.put(ingredient, character = (char) c);
                                    break;
                                }
                            }
                        }
                        if (character != null)
                            sb.append(character);
                    }
                    shape[i] = sb.toString();
                }
                recipe.shape(shape);
                ingredients.forEach((ingredient, c) -> recipe.setIngredient(c, Material.getMaterial(ingredient.toUpperCase())));

                try {
                    Bukkit.addRecipe(recipe);
                } catch (IllegalStateException e) {
                    getLogger().warning(getMessage("old_recipe_not_removed"));
                }
            }
        }
    }
}
