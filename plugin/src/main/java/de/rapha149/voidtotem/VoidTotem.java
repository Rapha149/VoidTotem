package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.ItemData;
import de.rapha149.voidtotem.Config.ItemData.RecipeData;
import de.rapha149.voidtotem.Config.ItemData.ResultData;
import de.rapha149.voidtotem.Metrics.AdvancedPie;
import de.rapha149.voidtotem.Metrics.DrilldownPie;
import de.rapha149.voidtotem.Metrics.SimplePie;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
            if (version == null)
                getLogger().info("Your version of this plugin is up to date!");
            else {
                getLogger().warning("There's a new version available for this plugin: " + version);
                getLogger().warning("You can download it from: " + Updates.SPIGOT_URL);
            }
        }

        getServer().getPluginManager().registerEvents(new Events(), this);
    }

    @Override
    public void onDisable() {
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
                    map.put(type.toString(), 1);
            });
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("animation", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> teleportParticles = new HashMap<>();
            Map<String, Integer> teleportSound = new HashMap<>();
            Map<String, Integer> totemParticles = new HashMap<>();
            Map<String, Integer> totemAnimation = new HashMap<>();
            teleportParticles.put(String.valueOf(Config.get().animation.teleportParticles), 1);
            teleportSound.put(String.valueOf(Config.get().animation.teleportSound), 1);
            totemParticles.put(String.valueOf(Config.get().animation.totemParticles), 1);
            totemAnimation.put(String.valueOf(Config.get().animation.totemAnimation), 1);
            map.put("Teleport particles", teleportParticles);
            map.put("Teleport sound", teleportSound);
            map.put("Totem particles", totemParticles);
            map.put("Totem animation", totemAnimation);
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
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof ShapelessRecipe) {
                if (((ShapelessRecipe) recipe).getKey().equals(RECIPE_KEY))
                    iterator.remove();
            } else if (recipe instanceof ShapedRecipe && ((ShapedRecipe) recipe).getKey().equals(RECIPE_KEY))
                iterator.remove();
        }

        ItemData itemData = Config.get().item;
        if (itemData.customRecipe && itemData.valid) {
            ResultData resultData = itemData.result;
            ItemStack result = wrapper.addIdentifier(wrapper.applyNBT(new ItemStack(Material.getMaterial(resultData.item.toUpperCase()),
                    resultData.count), resultData.nbt));

            RecipeData recipeData = itemData.recipe;
            if(!recipeData.shaped) {
                ShapelessRecipe recipe = new ShapelessRecipe(RECIPE_KEY, result);
                recipeData.shapelessIngredients.forEach(ingredient -> recipe.addIngredient(Material.getMaterial(ingredient.toUpperCase())));
                Bukkit.addRecipe(recipe);
            } else {
                ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, result);
                recipe.shape(recipeData.shapedRows);
                recipeData.shapedIngredients.forEach((c, ingredient) -> recipe.setIngredient(c, Material.getMaterial(ingredient.toUpperCase())));
                Bukkit.addRecipe(recipe);
            }
        }
    }
}
