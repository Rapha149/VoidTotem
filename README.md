# VoidTotem

A Minecraft plugin that enables you to use a totem to save you from the void.  
You can even set a custom item and recipe for the totem!
If you are resurrected by the totem you will be teleported onto a nearby block within a customizable search distance.

## URLS

- [Spigot](https://www.spigotmc.org/resources/void-totem.99003/)
- [bStats](https://bstats.org/plugin/bukkit/Void%20Totem/13802)

## Config

The default `config.yml` looks like this:
```yml
# VoidTotem version 1.7.1
# Github: https://github.com/Rapha149/VoidTotem
# Spigot: https://www.spigotmc.org/resources/void-totem.99003/

# Whether to check for updates on enabling.
checkForUpdates: true

# If the health of the player is be below or equal to this, the totem will try to resurrect the player.
# It's expressed in half hearts, that means if it's 0 the player will be resurrected when he would have 0 hearts left,
# if it's 10 the player will be resurrected when he would have 5 hearts left and if it's 20 the player will be resurrected on first void damage.
healthTrigger: 0.0

search:
  # Specifies the distance to search for suitable blocks. It's measured in blocks in every direction from the player.
  distance: 100

  # Customize the platform that will be created when the player is saved from the void but the plugin can't find a suitable block in the range of the search distance.
  # This does not work for mobs, even if "onlySavePlayers" is "false".
  platform:
    # Whether or not the platform should be created. If disabled and the plugin can't find any suitable blocks, the player won't be saved.
    enabled: true

    # The size of the platform. It's measured in blocks in every direction from the center of the platform.
    # For example: "0" will create a single block platform, "1" will create a 3x3 platform and "2" will create a 5x5 platform.
    size: 2

    # The y coordinate the platform will be created at.
    height: 70

    # The block the platform will be created from.
    block: minecraft:cobblestone

    # Whether or not the platform should be breakable by the player.
    # This should not be enabled when disappearing is disabled.
    # Please note: a platform that was unbreakable will be breakable after a restart/reload of the server.
    breakable: false

    # Customize the options for disappearing.
    disappear:
      # Whether or not the platform should disappear.
      # Please note: if the server restarts/reloads while the platform is still there, it won't disappear after the restart/reload.
      enabled: true

      # Whether or not the platform should only disappear after the player has left the platform.
      waitForPlayer: true

      # The time in seconds before the platform disappears.
      # If "waitForPlayer" is "true", the countdown will be started once the player leaves the platform.
      # If "waitForPlayer" is "false", the countdown will start directly after the creation of the platform.
      delay: 10

      # Whether or not to create a hologram above the platform that shows the remaining time before the platform disappears.
      hologram: true

      # Whether or not to play the block breaking sound.
      sound: true

# If disabled, the totem will save players from the /kill command.
# This is due to the fact that the damage cause in the Spigot API is the same for the void and /kill.
# If enabled the totem will only resurrect people if they are below the downward height limit.
patchKillCommand: true

# If disabled, mobs who can hold the totem will be saved from the void, too.
# That is the same behavior as for normal totem resurrections.
# Please note: the platform (see above) will not be created for mobs.
onlySavePlayers: false

# If enabled the plugin makes sure that the player gets teleported even if that is cancelled by other plugins.
# This might not work 100% of the time.
forceTeleport: false

playerData:
  # If enabled, the used totem statistic will be increased for the player if saved from the void.
  totemStatistic: true

  advancement:
    # If enabled, the player will receive the totem advancement upon resurrection if they did not have it before.
    enabled: true

    # The advancement to grant the player. The advancement has to exist on the server.
    advancement: minecraft:adventure/totem_of_undying

    # The criteria to set completed. Set to "[]" to complete the whole advancement.
    criteria: []

randomization:
  # Whether to randomize search for suitable blocks.
  enabled: true

  # How far to spread distance randomization.
  # For example: if it's 10, 10 distances will be shuffled. The distances 0-9 will be shuffled,
  # the distances 10-19 will be shuffled and so on.
  # Set to 0 to disable distance shuffling.
  # Set to 1 to shuffle all distances (that might teleport the player far away).
  distanceStack: 10

  # If disabled and there is a block directly above you, that block will be chosen.
  # In other words: the distance 0 won't be shuffled.
  randomizeZeroDistance: true

effects:
  # Whether to restore the food level and saturation after resurrection.
  restoreFoodLevel: false

  # Whether to remove existing potion effects after resurrection.
  # This is normal totem behaviour.
  removeExistingEffects: true

  # Potion effects to apply after resurrection.
  list:
  - name: REGENERATION
    duration: 45
    amplifier: 1
  - name: FIRE_RESISTANCE
    duration: 40
    amplifier: 0
  - name: ABSORPTION
    duration: 5
    amplifier: 1

animation:
  # Whether to display teleport particles after resurrection.
  teleportParticles: true

  # Whether to play a teleport sound and delay totem effects for a short amount of time.
  teleportSound: false

  # Whether to display the totem effects (animation, particles and sound).
  totemEffects: true

item:
  # If disabled, the totem does not has to be hold in the hand to work.
  # It then can by anywhere in the inventory.
  # If enabled, the totem has to be in the mainhand or the offhand, just like a normal totem.
  hasToBeInHand: true

  # Whether to use a custom item and recipe for the totem item.
  # If you made a mistake with the custom item you will be notified in the console and the item won't work.
  # Please note: if you've changed something for the recipe and reloaded the config you may have to rejoin for the changes to take effect.
  # Please also note: if you change the resulting item, earlier crafted totems will still work.
  customRecipe: false

  # If enabled, and "customRecipe" is enabled, you won't be able to use the custom totems for normal totem resurrections.
  # This also applies for totems that were given to players using the command
  #  included in the plugin - even if the command was executed before "customRecipe" was enabled.
  # Please note: even if "onlySavePlayers" is enabled, the custom totem still won't work for mobs.
  noNormalResurrection: false

  # The item to use as a totem item and the result of the recipe.
  result:
    item: totem_of_undying
    count: 1

    # The display name of the item.
    # You can use "&" and a color code to colorize the chat or use the adventure text syntax. A few examples:
    #  - &e&lText = yellow and bold
    #  - <yellow><bold>Text = yellow and bold
    #  - <#ff0000>Text = red hex color
    #  - <rainbow>Text</rainbow> = rainbow colors
    #  - <gradient:yellow:gold>Text</gradient> = gradient from yellow to gold
    #  - <gradient:#ff0000:#ff6f00:#ffff00>Text</gradient> = gradient from red over orange to yellow (with hex colors)
    # The adventure syntax is described here: https://docs.adventure.kyori.net/minimessage#format
    # Please note that hex colors, rainbows and gradients are NOT supported in 1.15 and lower.
    # You should only use them in 1.16 and above. They will look very weird in 1.15 and lower.
    # 
    # If given in the NBT string, the display name in the NBT string will override this.
    # Set to "null" to disable.
    name: '&6Void &eTotem'

    # The lore of the item as an array. Each array item is a line in the lore.
    # You can use the same format as for "name".
    # If given in the NBT string, the lore in the NBT string will override this.
    # Set to "[]" to disable.
    lore:
    - '&7Save yourself from the void!'

    # The NBT string to apply to the item.
    # Set to "{}" to disable.
    # If you want to include ' in your nbt string, you can escape them using ''
    # "HideFlags: 1" which is given by default is used to hide the enchantments.
    # 
    # If you don't know how NBT works, see this tutorial: https://minecraft.fandom.com/wiki/Tutorials/Command_NBT_tags
    #  or use a /give generator and copy everything from { to }. Give command generator examples:
    #  - https://mcstacker.net (click on the "/give" button)
    #  - https://www.gamergeeks.net/apps/minecraft/give-command-generator
    nbt: '{HideFlags: 1, Enchantments: [{id: "minecraft:unbreaking", lvl: 1}]}'

  recipe:
    # Whether the recipe should be a shaped recipe.
    shaped: true

    # The ingredients in case "shaped" is disabled.
    # You have to provide at least 1 and at most 9 ingredients.
    shapelessIngredients:
    - totem_of_undying
    - ender_pearl
    - chorus_fruit

    # The ingredients in case "shaped" is enabled.
    # The shape may differ from the original 3x3. For example it can be 2x3, 3x2 or 2x2.
    # You have to provide at least 1 and at most 3 rows and at least 1 and at most 3 ingredients per row.
    shapedIngredients:
    - chorus_fruit | diamond | chorus_fruit
    - ender_pearl | totem_of_undying | ender_pearl
    - chorus_fruit | diamond | chorus_fruit
```

If you need any help regarding the config, don't hesitate to ask, I tried to explain it as clearly as possible.

### Messages

You can change all messages in the `messages.yml` file!

## Commands

The plugin has one basic command: `/voidtotem`  
The alias is `/vt`  
Sub commands are:
- `/voidtotem reload` - Reloads the config.
- `/voidtotem giveitem [Player]` - Gives you or another player a void totem item. When using via the console the player has to be specified.  
  If `customRecipe` was disabled on command execution, the item that was given to the player will also work when `customRecipe` is enabled.

## Permissions

- `voidtotem.reload` - Permission for `/voidtotem reload`
- `voidtotem.giveitem` - Permission for `/voidtotem giveitem`
- `voidtotem.giveitem.others` - Permission for `/voidtotem giveitem <Player>`

## Additional information

This plugin collects anonymous server stats with [bStats](https://bstats.org), an open-source statistics service for Minecraft software. If you don't want this, you can deactivate it in `plugins/bStats/config.yml`.

## Credits

Credits go to [laGameTV](https://lagametv.de/) for the idea of the plugin.
