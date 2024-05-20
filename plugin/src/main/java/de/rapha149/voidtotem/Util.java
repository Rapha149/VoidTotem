package de.rapha149.voidtotem;

import com.google.common.collect.Streams;
import de.rapha149.voidtotem.Config.ItemData;
import de.rapha149.voidtotem.Config.ItemData.RecipeData;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static de.rapha149.voidtotem.Messages.getMessage;

public class Util {

    private static final NamespacedKey KEY = new NamespacedKey(VoidTotem.getInstance(), "void_totem");

    public static Logger LOGGER;
    public static VersionWrapper WRAPPER;

    public static void loadRecipe() {
        WRAPPER.removeRecipe(KEY);

        if (Streams.stream(Bukkit.recipeIterator()).anyMatch(recipe ->
                (recipe instanceof ShapelessRecipe && ((ShapelessRecipe) recipe).getKey().equals(KEY) ||
                 (recipe instanceof ShapedRecipe && ((ShapedRecipe) recipe).getKey().equals(KEY))))) {
            LOGGER.warning(getMessage("old_recipe_not_removed"));
            return;
        }

        ItemData itemData = Config.get().item;
        if (itemData.customItem && itemData.enableRecipe && itemData.result.valid && itemData.recipe.valid) {
            ItemStack result = itemData.result.getItemStack();
            RecipeData recipeData = itemData.recipe;
            if (!recipeData.shaped) {
                ShapelessRecipe recipe = new ShapelessRecipe(KEY, result);
                recipeData.shapelessIngredients.forEach(ingredient -> recipe.addIngredient(Material.getMaterial(ingredient.toUpperCase())));
                Bukkit.addRecipe(recipe);
            } else {
                ShapedRecipe recipe = new ShapedRecipe(KEY, result);
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
                    LOGGER.warning(getMessage("old_recipe_not_removed"));
                }
            }
        }
    }

    public static void addIdentifier(ItemStack item) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public static boolean hasIdentifier(ItemStack item) {
        PersistentDataContainer pdc;
        return item.hasItemMeta() && (pdc = item.getItemMeta().getPersistentDataContainer()).has(KEY, PersistentDataType.BYTE) &&
               pdc.get(KEY, PersistentDataType.BYTE) == (byte) 1;
    }
}
