package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.ItemData.RecipeData;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

    private static Config config;

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

        try (FileWriter writer = new FileWriter(file)) {
            Pattern pattern = Pattern.compile("\\s+id: (\\d+)");
            String[] lines = yaml.dumpAsMap(config).split("\n");
            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = pattern.matcher(lines[i]);
                if (matcher.matches()) {
                    PotionEffectType type = PotionEffectType.getById(Integer.parseInt(matcher.group(1)));
                    if (type != null)
                        lines[i] = lines[i] + "  # " + type;
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
            public String nbt = "{display: {Name: '{\"text\": \"§7Void §eTotem\"}'}, HideFlags:1, Enchantments:[{id:\"minecraft:unbreaking\",lvl:1}]}";
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
