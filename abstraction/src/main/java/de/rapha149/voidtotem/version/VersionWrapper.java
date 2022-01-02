package de.rapha149.voidtotem.version;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public interface VersionWrapper {

    String IDENTIFIER = "voidtotem";

    ItemStack applyNBT(ItemStack item, String nbt) throws IllegalArgumentException;

    ItemStack addIdentifier(ItemStack item);

    boolean hasIdentifier(ItemStack item);

    boolean isPassable(Block block);
}
