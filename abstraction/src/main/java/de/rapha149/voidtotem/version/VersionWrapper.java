package de.rapha149.voidtotem.version;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public interface VersionWrapper {

    String IDENTIFIER = "voidtotem";

    ItemStack applyNBT(ItemStack item, String nbt) throws IllegalArgumentException;

    ItemStack addIdentifier(ItemStack item);

    boolean hasIdentifier(ItemStack item);

    String getPotionEffectName(PotionEffectType type);

    Block getHighestEmptyBlockAt(World world, int x, int z);

    boolean isPassable(Block block);
}
