package de.rapha149.voidtotem.version;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public interface VersionWrapper {

    String IDENTIFIER = "voidtotem";

    boolean verifyNBT(String nbt);

    ItemStack applyNBT(ItemStack item, String nbt) throws IllegalArgumentException;

    ItemStack addIdentifier(ItemStack item);

    boolean hasIdentifier(ItemStack item);

    void removeRecipe(NamespacedKey key);

    String getPotionEffectName(PotionEffectType type);

    int getDownwardHeightLimit(World world);

    Block getHighestEmptyBlockAt(World world, int x, int z);

    boolean isPassable(Block block);

    void playBreakSound(Block block);

    double getAbsorptionHearts(LivingEntity entity);
}
