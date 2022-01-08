# VoidTotem

A Minecraft plugin that enables you to use a totem to save you from the void.  
You can even set a custom item and recipe for the totem!
If you are resurrected by the totem you will be teleported onto a nearby block within a customizable search distance.

## URLS

- [Spigot](https://www.spigotmc.org/resources/void-totem.99003/)
- [bStats](https://bstats.org/plugin/bukkit/Void%20Totem/13802)

## Config

The default `config.yml` looks like this (without the comments):
```yml
# Whether to check for updates on enabling.
checkForUpdates: true

# If the health of the player is be below or equal to this, the totem will try to resurrect the player.
# It's expressed in half hearts, that means if it's 0 the player will be resurrected when he would have 0 hearts left,
# if it's 10 the player will be resurrected when he would have 5 hearts left and if it's 20 the player will be resurrected on first void damage.
healthTrigger: 0.0

# Specifies the distance to search for suitable blocks. It's measured in blocks in every direction from the player.
searchDistance: 100

# If disabled, the totem will save players from the /kill command.
# This is due to the fact that the damage cause in the Spigot API is the same for the void and /kill.
# If enabled the totem will only resurrect people if they are below the downward height limit.
patchKillCommand: true

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
  # A list of ids can be found here: https://minecraft.fandom.com/wiki/Effect#Effect_list
  #  (Please only look at values that are present in the Java Edition)
  list:
  - id: 10  # regeneration
    duration: 45
    amplifier: 1
  - id: 12  # fire_resistance
    duration: 40
    amplifier: 0
  - id: 22  # absorption
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

  # The item to use as a totem item and the result of the recipe.
  result:
    item: totem_of_undying
    count: 1

    # If you want to include ' in your nbt string, you can escape them using ''
    # "HideFlags: 1" which is given by default is used to hide the enchantments.
    # If you don't know how NBT works, see this tutorial: https://minecraft.fandom.com/wiki/Tutorials/Command_NBT_tags
    #  or use a /give generator and copy everything from { to }. Give command generator examples:
    #  - https://mcstacker.net (click on the "/give" button)
    #  - https://www.gamergeeks.net/apps/minecraft/give-command-generator
    nbt: '{display: {Name: "{\"text\": \"§6Void §eTotem\"}"}, HideFlags: 1, Enchantments:
      [{id: "minecraft:unbreaking", lvl: 1}]}'

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

If you need any help regarding the config, don't hesitate to ask, I tried to explaining it as clearly as possible.

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

Credits go to [laGameTV](https://github.com/laGameTV) for the idea of the plugin.
