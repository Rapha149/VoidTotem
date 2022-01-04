package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.ItemData.RecipeData;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

    private static Map<String, String> comments = new HashMap<>();
    private static Config config;

    static {
        comments.put("checkForUpdates", "Whether to check for updates on enabling.");
        comments.put("healthTrigger", "If the health of the player is be below or equal to this, the totem will try to resurrect the player.");
        comments.put("searchDistance", "Specifies the distance to search for suitable blocks.");
        comments.put("randomization.enabled", "Whether to randomize search for suitable blocks.");
        comments.put("randomization.distanceStack", "How far to spread distance randomization.");
        comments.put("randomization.randomizeZeroDistance", "If disabled and there is a block directly above you, that block will be chosen.");
        comments.put("effects.restoreFoodLevel", "Whether to restore the food level and saturation after resurrection.");
        comments.put("effects.removeExistingEffects", "Whether to remove existing potion effects after resurrection. (Normal totem behaviour)");
        comments.put("effects.list", "Potion effects to apply after resurrection. A list of ids can be found here: https://minecraft.fandom.com/wiki/Effect#Effect_list");
        comments.put("animation.teleportParticles", "Whether to display teleport particles after resurrection.");
        comments.put("animation.teleportSound", "Whether to play a teleport sound and delay totem effects for a short amount of time.");
        comments.put("animation.totemParticles", "Whether to display totem particles.");
        comments.put("animation.totemAnimation", "Whether to display the totem animation.");
        comments.put("item.hasToBeInHand", "If disabled, the totem does not has to be hold in the hand to work.");
        comments.put("item.customRecipe", "Whether to use a custom item and recipe for the totem item.");
        comments.put("item.result", "The item to use as a totem item and the result of the recipe.");
        comments.put("item.recipe.shaped", "Whether the recipe should be a shaped recipe.");
        comments.put("item.recipe.shapelessIngredients", "The ingredients in case \"shaped\" is disabled.");
        comments.put("item.recipe.shapedRows", "The shape of the recipe in case \"shaped\" is enabled.");
        comments.put("item.recipe.shapedIngredients", "The ingredients of the recipe in case \"shaped\" is enabled.");
    }

    public static void load() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Representer representer = new Representer();
        representer.setPropertyUtils(new CustomPropertyUtils());
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(VoidTotem.getInstance().getClass().getClassLoader()), representer, options);

        File file = new File(VoidTotem.getInstance().getDataFolder(), "config.yml");
        if (file.exists())
            config = yaml.loadAs(new FileReader(file), Config.class);
        else {
            file.getParentFile().mkdirs();
            config = new Config();
        }

        VersionWrapper wrapper = VoidTotem.getInstance().wrapper;
        try (FileWriter writer = new FileWriter(file)) {
            Pattern pattern = Pattern.compile("((\\s|-)*)(\\w+):( .+)?");
            Pattern potionPattern = Pattern.compile("  - id: (\\d+)");
            Map<Integer, String> parents = new HashMap<>();
            String[] lines = yaml.dumpAsMap(config).split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    int indent = matcher.group(1).length();
                    parents.put(indent, matcher.group(3));

                    List<String> tree = new ArrayList<>();
                    for (int j = 0; j <= indent; j += options.getIndent())
                        tree.add(parents.get(j));
                    String key = String.join(".", tree);
                    if (comments.containsKey(key))
                        lines[i] = line + "  # " + comments.get(key);
                }

                Matcher potionMatcher = potionPattern.matcher(line);
                if (potionMatcher.matches()) {
                    PotionEffectType type = PotionEffectType.getById(Integer.parseInt(potionMatcher.group(1)));
                    if (type != null)
                        lines[i] = line + "  # " + wrapper.getPotionEffectName(type);
                }
            }

            writer.write(String.join("\n", lines));
        }

        Logger logger = VoidTotem.getInstance().getLogger();
        config.effects.list.forEach(effect -> {
            if (PotionEffectType.getById(effect.id) == null) {
                logger.warning("There's no potion effect with the id \"" + effect.id + "\"");
                effect.valid = false;
            }
        });

        ItemData item = config.item;
        if (item.customRecipe) {
            String resultItem = config.item.result.item;
            if (Material.getMaterial(resultItem.toUpperCase()) == null) {
                logger.warning("The result item \"" + resultItem + "\" does not exist.");
                item.valid = false;
            }

            RecipeData recipe = item.recipe;
            (recipe.shaped ? recipe.shapedIngredients.values() : recipe.shapelessIngredients).forEach(ingredient -> {
                if (Material.getMaterial(ingredient.toUpperCase()) == null) {
                    logger.warning("The ingredient item \"" + ingredient + "\" does not exist.");
                    item.valid = false;
                }
            });
        }
    }

    public static Config get() {
        return config;
    }

    public boolean checkForUpdates = true;
    public double healthTrigger = 0;
    public int searchDistance = 100;
    public RandomizationData randomization = new RandomizationData();
    public EffectsData effects = new EffectsData();
    public AnimationData animation = new AnimationData();
    public ItemData item = new ItemData();

    public static class RandomizationData {

        public boolean enabled = true;
        public int distanceStack = 10;
        public boolean randomizeZeroDistance = true;
    }

    public static class EffectsData {

        public boolean restoreFoodLevel = false;
        public boolean removeExistingEffects = true;
        public List<EffectData> list = Arrays.asList(new EffectData(PotionEffectType.REGENERATION.getId(), 45, 1),
                new EffectData(PotionEffectType.FIRE_RESISTANCE.getId(), 40, 0),
                new EffectData(PotionEffectType.ABSORPTION.getId(), 5, 1));

        public static class EffectData {

            public int id = 1;
            public int duration = 30;
            public int amplifier = 0;

            public transient boolean valid = true;

            public EffectData() {
            }

            EffectData(int id, int duration, int amplifier) {
                this.id = id;
                this.duration = duration;
                this.amplifier = amplifier;
            }
        }
    }

    public static class AnimationData {

        public boolean teleportParticles = true;
        public boolean teleportSound = false;
        public boolean totemParticles = true;
        public boolean totemAnimation = true;
    }

    public static class ItemData {

        public boolean hasToBeInHand = true;
        public boolean customRecipe = false;
        public ResultData result = new ResultData();
        public RecipeData recipe = new RecipeData();

        public transient boolean valid = true;

        public static class ResultData {

            public String item = "totem_of_undying";
            public int count = 1;
            public String nbt = "{display: {Name: \"{\\\"text\\\": \\\"§7Void §eTotem\\\"}\"}, HideFlags:1, Enchantments:[{id:\"minecraft:unbreaking\",lvl:1}]}";
        }

        public static class RecipeData {

            public boolean shaped = true;
            public List<String> shapelessIngredients = Arrays.asList("totem_of_undying", "ender_pearl", "chorus_fruit");
            public String[] shapedRows = new String[]{"cdc", "ete", "cdc"};
            public Map<Character, String> shapedIngredients = new HashMap<>();

            {
                shapedIngredients.put('c', "chorus_fruit");
                shapedIngredients.put('d', "diamond");
                shapedIngredients.put('e', "ender_pearl");
                shapedIngredients.put('t', "totem_of_undying");
            }
        }
    }
}
