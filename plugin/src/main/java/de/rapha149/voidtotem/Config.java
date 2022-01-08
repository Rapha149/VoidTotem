package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.ItemData.RecipeData;
import de.rapha149.voidtotem.Config.ItemData.ResultData;
import de.rapha149.voidtotem.version.VersionWrapper;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.rapha149.voidtotem.Messages.getMessage;

public class Config {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private static Map<String, String> comments = new HashMap<>();
    private static Config config;

    static {
        comments.put("checkForUpdates", "Whether to check for updates on enabling.");
        comments.put("healthTrigger", "If the health of the player is be below or equal to this, the totem will try to resurrect the player." +
                                      "\nIt's expressed in half hearts, that means if it's 0 the player will be resurrected when he would have 0 hearts left," +
                                      "\nif it's 10 the player will be resurrected when he would have 5 hearts left and if it's 20 the player will be resurrected on first void damage.");
        comments.put("searchDistance", "Specifies the distance to search for suitable blocks. It's measured in blocks in every direction from the player.");
        comments.put("patchKillCommand", "If disabled, the totem will save players from the /kill command." +
                                         "\nThis is due to the fact that the damage cause in the Spigot API is the same for the void and /kill." +
                                         "\nIf enabled the totem will only resurrect people if they are below the downward height limit.");
        comments.put("randomization.enabled", "Whether to randomize search for suitable blocks.");
        comments.put("randomization.distanceStack", "How far to spread distance randomization." +
                                                    "\nFor example: if it's 10, 10 distances will be shuffled. The distances 0-9 will be shuffled," +
                                                    "\nthe distances 10-19 will be shuffled and so on." +
                                                    "\nSet to 0 to disable distance shuffling." +
                                                    "\nSet to 1 to shuffle all distances (that might teleport the player far away).");
        comments.put("randomization.randomizeZeroDistance", "If disabled and there is a block directly above you, that block will be chosen." +
                                                            "\nIn other words: the distance 0 won't be shuffled.");
        comments.put("effects.restoreFoodLevel", "Whether to restore the food level and saturation after resurrection.");
        comments.put("effects.removeExistingEffects", "Whether to remove existing potion effects after resurrection." +
                                                      "\nThis is normal totem behaviour.");
        comments.put("effects.list", "Potion effects to apply after resurrection." +
                                     "\nA list of ids can be found here: https://minecraft.fandom.com/wiki/Effect#Effect_list" +
                                     "\n (Please only look at values that are present in the Java Edition)");
        comments.put("animation.teleportParticles", "Whether to display teleport particles after resurrection.");
        comments.put("animation.teleportSound", "Whether to play a teleport sound and delay totem effects for a short amount of time.");
        comments.put("animation.totemEffects", "Whether to display the totem effects (animation, particles and sound).");
        comments.put("item.hasToBeInHand", "If disabled, the totem does not has to be hold in the hand to work." +
                                           "\nIt then can by anywhere in the inventory." +
                                           "\nIf enabled, the totem has to be in the mainhand or the offhand, just like a normal totem.");
        comments.put("item.customRecipe", "Whether to use a custom item and recipe for the totem item." +
                                          "\nIf you made a mistake with the custom item you will be notified in the console and the item won't work." +
                                          "\nPlease note: if you've changed something for the recipe and reloaded the config you may have to rejoin for the changes to take effect." +
                                          "\nPlease also note: if you change the resulting item, earlier crafted totems will still work.");
        comments.put("item.result", "The item to use as a totem item and the result of the recipe.");
        comments.put("item.result.name", "The display name of the item." +
                                         "\nYou can use \"&\" and a color code to colorize the chat or use the adventure text syntax. A few examples:" +
                                         "\n - &e&lText = yellow and bold" +
                                         "\n - <yellow><bold>Text = yellow and bold" +
                                         "\n - <#ff0000>Text = red hex color" +
                                         "\n - <rainbow>Text</rainbow> = rainbow colors" +
                                         "\n - <gradient:yellow:gold>Text</gradient> = gradient from yellow to gold" +
                                         "\n - <gradient:#ff0000:#ff6f00:#ffff00>Text</gradient> = gradient from red over orange to yellow (with hex colors)" +
                                         "\nThe adventure syntax is described here: https://docs.adventure.kyori.net/minimessage#format" +
                                         "\nPlease note that hex colors, rainbows and gradients are NOT supported in 1.15 and lower." +
                                         "\nYou should only use them in 1.16 and above. They will look very weird in 1.15 and lower." +
                                         "\n\nIf given in the NBT string, the display name in the NBT string will override this." +
                                         "\nSet to \"null\" to disable.");
        comments.put("item.result.lore", "The lore of the item as an array. Each array item is a line in the lore." +
                                         "\nYou can use the same format as for \"name\"." +
                                         "\nIf given in the NBT string, the lore in the NBT string will override this." +
                                         "\nSet to \"[]\" to disable.");
        comments.put("item.result.nbt", "The NBT string to apply to the item." +
                                        "\nSet to \"{}\" to disable." +
                                        "\nIf you want to include ' in your nbt string, you can escape them using ''" +
                                        "\n\"HideFlags: 1\" which is given by default is used to hide the enchantments." +
                                        "\n\nIf you don't know how NBT works, see this tutorial: https://minecraft.fandom.com/wiki/Tutorials/Command_NBT_tags" +
                                        "\n or use a /give generator and copy everything from { to }. Give command generator examples:" +
                                        "\n - https://mcstacker.net (click on the \"/give\" button)" +
                                        "\n - https://www.gamergeeks.net/apps/minecraft/give-command-generator");
        comments.put("item.recipe.shaped", "Whether the recipe should be a shaped recipe.");
        comments.put("item.recipe.shapelessIngredients", "The ingredients in case \"shaped\" is disabled." +
                                                         "\nYou have to provide at least 1 and at most 9 ingredients.");
        comments.put("item.recipe.shapedIngredients", "The ingredients in case \"shaped\" is enabled." +
                                                      "\nThe shape may differ from the original 3x3. For example it can be 2x3, 3x2 or 2x2." +
                                                      "\nYou have to provide at least 1 and at most 3 rows and at least 1 and at most 3 ingredients per row.");
    }

    public static boolean load() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setSplitLines(false);
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
            int lastIndent = 0;
            String[] lines = yaml.dumpAsMap(config).split("\n");
            StringBuilder sb = new StringBuilder("# VoidTotem version " + VoidTotem.getInstance().getDescription().getVersion() +
                                                 "\n# Github: https://github.com/Rapha149/VoidTotem" +
                                                 "\n# Spigot: " + Updates.SPIGOT_URL + "\n");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    int indent = matcher.group(1).length();
                    parents.put(indent, matcher.group(3));

                    List<String> tree = new ArrayList<>();
                    for (int j = 0; j <= indent; j += options.getIndent())
                        tree.add(parents.get(j));
                    String key = String.join(".", tree);
                    if (comments.containsKey(key)) {
                        if (lastIndent == indent)
                            sb.append("\n");

                        String prefix = StringUtils.repeat(" ", indent) + "# ";
                        sb.append(prefix + String.join("\n" + prefix, comments.get(key).split("\n")) + "\n" + line + "\n");

                        lastIndent = indent;
                        continue;
                    } else if (matcher.group(4) == null)
                        sb.append("\n");
                    lastIndent = indent;
                }

                Matcher potionMatcher = potionPattern.matcher(line);
                if (potionMatcher.matches()) {
                    PotionEffectType type = PotionEffectType.getById(Integer.parseInt(potionMatcher.group(1)));
                    if (type != null) {
                        sb.append(line + "  # " + wrapper.getPotionEffectName(type) + "\n");
                        continue;
                    }
                }

                sb.append(line + "\n");
            }

            writer.write(sb.toString().replaceAll("\\[\\n\\s+\\]", "[]"));
        }

        AtomicBoolean mistakes = new AtomicBoolean(false);
        Logger logger = VoidTotem.getInstance().getLogger();
        config.effects.list.forEach(effect -> {
            if (PotionEffectType.getById(effect.id) == null) {
                logger.severe(getMessage("config.potion_effect_not_found").replace("%id%", String.valueOf(effect.id)));
                effect.valid = false;
                mistakes.set(true);
            }
        });

        ItemData item = config.item;
        ResultData result = config.item.result;
        if (item.customRecipe) {
            if (Material.getMaterial(result.item.toUpperCase()) == null) {
                logger.severe(getMessage("config.recipe.result_item.not_found").replace("%item%", result.item));
                result.valid = false;
                mistakes.set(true);
            }

            if (result.count <= 0 || result.count > 127) {
                logger.severe(getMessage("config.recipe.result_item.invalid_count").replace("%limit_down%", "1")
                        .replace("%limit_up%", "127"));
                result.valid = false;
                mistakes.set(true);
            }

            if (!wrapper.verifyNBT(result.nbt)) {
                logger.severe(getMessage("config.recipe.result_item.invalid_nbt"));
                result.valid = false;
                mistakes.set(true);
            }

            RecipeData recipe = item.recipe;
            if (!recipe.shaped) {
                if (recipe.shapelessIngredients.size() <= 0 || recipe.shapelessIngredients.size() > 9) {
                    logger.severe(getMessage("config.recipe.shapeless_ingredients_invalid_count"));
                    recipe.valid = false;
                    mistakes.set(true);
                }
            } else {
                if (recipe.shapedIngredients.size() <= 0 || recipe.shapedIngredients.size() > 3) {
                    logger.severe(getMessage("config.recipe.shaped.ingredient_rows_invalid_count"));
                    recipe.valid = false;
                    mistakes.set(true);
                }
                for (int i = 0; i < recipe.shapedIngredients.size(); i++) {
                    int length = recipe.shapedIngredients.get(i).split("\\|").length;
                    if (length <= 0 || length > 3) {
                        logger.severe(getMessage("config.recipe.shaped.ingredients_invalid_count")
                                .replace("%row%", String.valueOf(i + 1)));
                        recipe.valid = false;
                        mistakes.set(true);
                    }
                }
            }

            (recipe.shaped ? recipe.shapedIngredients.stream().flatMap(row -> Arrays.stream(row.split("\\|")))
                    .map(String::trim) : recipe.shapelessIngredients.stream()).distinct().forEach(ingredient -> {
                if (Material.getMaterial(ingredient.toUpperCase()) == null) {
                    logger.severe(getMessage("config.recipe.ingredient_item_invalid").replace("%item%", ingredient));
                    recipe.valid = false;
                    mistakes.set(true);
                }
            });
        }

        if (item.result.valid) {
            if (!item.customRecipe) {
                result.itemStack = wrapper.addIdentifier(new ItemStack(Material.TOTEM_OF_UNDYING));
            } else if (result.valid) {
                ItemStack itemStack = new ItemStack(Material.getMaterial(result.item.toUpperCase()), result.count);
                ItemMeta meta = itemStack.getItemMeta();
                if (result.name != null)
                    meta.setDisplayName(colorize(result.name));
                if (!result.lore.isEmpty())
                    meta.setLore(result.lore.stream().map(Config::colorize).collect(Collectors.toList()));
                itemStack.setItemMeta(meta);
                itemStack = wrapper.applyNBT(itemStack, result.nbt);
                result.itemStack = wrapper.addIdentifier(itemStack);
            } else
                result.itemStack = null;
        }

        return !mistakes.get();
    }

    public static Config get() {
        return config;
    }

    private static String colorize(String str) {
        return SERIALIZER.serialize(MINI_MESSAGE.parse(ChatColor.translateAlternateColorCodes('&', str)));
    }

    public boolean checkForUpdates = true;
    public double healthTrigger = 0;
    public int searchDistance = 100;
    public boolean patchKillCommand = true;
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
        public boolean totemEffects = true;
    }

    public static class ItemData {

        public boolean hasToBeInHand = true;
        public boolean customRecipe = false;
        public ResultData result = new ResultData();
        public RecipeData recipe = new RecipeData();

        public static class ResultData {

            public String item = "totem_of_undying";
            public int count = 1;
            public String name = "&6Void &eTotem";
            public List<String> lore = Arrays.asList("&7Save yourself from the void!");
            public String nbt = "{HideFlags: 1, Enchantments: [{id: \"minecraft:unbreaking\", lvl: 1}]}";

            public transient boolean valid = true;
            private transient ItemStack itemStack;

            public ItemStack getItemStack() {
                return itemStack != null ? itemStack.clone() : null;
            }
        }

        public static class RecipeData {

            public boolean shaped = true;
            public List<String> shapelessIngredients = Arrays.asList("totem_of_undying", "ender_pearl", "chorus_fruit");
            public List<String> shapedIngredients = Arrays.asList("chorus_fruit | diamond | chorus_fruit",
                    "ender_pearl | totem_of_undying | ender_pearl",
                    "chorus_fruit | diamond | chorus_fruit");

            public transient boolean valid = true;
        }
    }
}
