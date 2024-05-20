package de.rapha149.voidtotem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Messages {

    private static File messageFile;
    private static FileConfiguration messageConfig;

    static {
        messageFile = new File(VoidTotem.getInstance().getDataFolder(), "messages.yml");
        messageConfig = new YamlConfiguration();
        messageConfig.options().copyDefaults(true);
        messageConfig.addDefault("prefix", "&8[&4VoidTotem&8] ");
        messageConfig.addDefault("plugin.enable", "Plugin successfully enabled.");
        messageConfig.addDefault("plugin.disable", "Plugin disabled.");
        messageConfig.addDefault("plugin.up_to_date", "Your version of this plugin is up to date!");
        messageConfig.addDefault("plugin.outdated", "There's a new version available for this plugin: %version%" +
                                                    "\nYou can download it from: %url%");
        messageConfig.addDefault("config.search.platform.block_not_found", "The block \"%block%\" does not exist (search platform). The fallback is \"minecraft:cobblestone\".");
        messageConfig.addDefault("config.search.platform.not_a_block", "The material \"%block%\" is not a solid block (search platform). The fallback is \"minecraft:cobblestone\".");
        messageConfig.addDefault("config.advancement.invalid_key", "Invalid advancement key \"%key%\"");
        messageConfig.addDefault("config.advancement.not_found", "There's no advancement with the key \"%key%\"");
        messageConfig.addDefault("config.potion_effect_not_found", "There's no potion effect with the id \"%id%\"");
        messageConfig.addDefault("config.recipe.result_item.not_found", "The result item \"%item%\" does not exist.");
        messageConfig.addDefault("config.recipe.result_item.invalid_count", "The count of the result item has to be between %limit_down% and %limit_up%.");
        messageConfig.addDefault("config.recipe.result_item.invalid_nbt", "Can't read result item nbt string.");
        messageConfig.addDefault("config.recipe.shapeless_ingredients_invalid_count", "You specified an invalid amount of ingredients.");
        messageConfig.addDefault("config.recipe.shaped.ingredient_rows_invalid_count", "You specified an invalid amount of ingredient rows.");
        messageConfig.addDefault("config.recipe.shaped.ingredients_invalid_count", "Row %row% has an invalid amount of ingredients.");
        messageConfig.addDefault("config.recipe.ingredient_item_invalid", "The ingredient item \"%item%\" does not exist.");
        messageConfig.addDefault("old_recipe_not_removed", "The old recipe could not be removed. " +
                                                           "If you changed the recipe, please restart the server for the changed to take affect.");
        messageConfig.addDefault("error", "%prefix%&cAn error occured. Check the console for details.");
        messageConfig.addDefault("syntax", "%prefix%&cSyntax error! Please use &7/%syntax%&c.");
        messageConfig.addDefault("no_permission", "%prefix%&cYou do not have enough permissions to perform this action.");
        messageConfig.addDefault("not_player", "%prefix%&cYou have to be a player to perform this action.");
        messageConfig.addDefault("player_not_found", "%prefix%&cThere's no player with the name &7%name%&c.");
        messageConfig.addDefault("reload.mistakes", "%prefix%&6You made some mistakes in the config, check the console for details.");
        messageConfig.addDefault("reload.success", "%prefix%&7Config was reloaded.");
        messageConfig.addDefault("giveitem.item_not_valid", "%prefix%&cThe result item in the config is not valid.");
        messageConfig.addDefault("giveitem.self.no_empty_slot", "%prefix%&cYou have no empty slot in your inventory.");
        messageConfig.addDefault("giveitem.self.success", "%prefix%&7A void totem item was given to you.");
        messageConfig.addDefault("giveitem.others.no_empty_slot", "%prefix%&7%player% &chas no empty slot in your inventory.");
        messageConfig.addDefault("giveitem.others.success", "%prefix%&7A void totem item was given to &6%player%&7.");
        messageConfig.addDefault("isvoidtotem.no_custom_item", "%prefix%&6You don't have §ecustomItem §6enabled in the config, therefore every totem is a void totem.");
        messageConfig.addDefault("isvoidtotem.no_totem_in_hand", "%prefix%&6You don't have a totem in your main hand.");
        messageConfig.addDefault("isvoidtotem.is_not_void_totem", "%prefix%&cThe totem in your main hand is §4not §ca void totem.");
        messageConfig.addDefault("isvoidtotem.is_void_totem", "%prefix%&aThe totem in your main hand is a void totem.");
        messageConfig.addDefault("makevoidtotem.no_custom_item", "%prefix%&6You don't have §ecustomItem §6enabled in the config, therefore every totem is a void totem.");
        messageConfig.addDefault("makevoidtotem.no_totem_in_hand", "%prefix%&6You don't have a totem in your main hand.");
        messageConfig.addDefault("makevoidtotem.already_void_totem", "%prefix%&6The totem in your main hand was already a void totem.");
        messageConfig.addDefault("makevoidtotem.success", "%prefix%&7The totem in your main hand is now a void totem.");
        messageConfig.addDefault("platform_hologram.wait_for_player", "&c! Attention !" +
                                                      "\n§6This platform is only temporary." +
                                                      "\n§6It will be destroyed once you leave it.");
        messageConfig.addDefault("platform_hologram.delay", "&c! Attention !" +
                                                      "\n§6This platform is only temporary." +
                                                      "\n§6It will be destroyed in &c%time% &6seconds.");
    }

    public static void loadMessages() {
        try {
            if (messageFile.exists())
                messageConfig.load(messageFile);
            else
                messageFile.getParentFile().mkdirs();

            messageConfig.getKeys(true).forEach(key -> {
                if (!messageConfig.getDefaults().isSet(key))
                    messageConfig.set(key, null);
            });

            messageConfig.save(messageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            VoidTotem.getInstance().getLogger().severe("Failed to load message config.");
        }
    }

    public static String getMessage(String key) {
        if (messageConfig.contains(key)) {
            return ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key)
                    .replace("\\n", "\n")
                    .replace("%prefix%", messageConfig.getString("prefix")));
        } else
            throw new IllegalArgumentException("Message key \"" + key + "\" does not exist.");
    }
}