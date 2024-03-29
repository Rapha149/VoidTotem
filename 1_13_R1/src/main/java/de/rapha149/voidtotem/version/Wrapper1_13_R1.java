package de.rapha149.voidtotem.version;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.v1_13_R1.ItemStack;
import net.minecraft.server.v1_13_R1.MinecraftKey;
import net.minecraft.server.v1_13_R1.MojangsonParser;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R1.CraftServer;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class Wrapper1_13_R1 implements VersionWrapper {

    @Override
    public boolean verifyNBT(String nbt) {
        try {
            MojangsonParser.parse(nbt);
            return true;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack applyNBT(org.bukkit.inventory.ItemStack item, String nbt) {
        try {
            ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            nmsItem.getOrCreateTag().a(MojangsonParser.parse(nbt));
            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Can't read nbt string", e);
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack addIdentifier(org.bukkit.inventory.ItemStack item) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        nmsItem.getOrCreateTag().setBoolean(IDENTIFIER, true);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public boolean hasIdentifier(org.bukkit.inventory.ItemStack item) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (!nmsItem.hasTag())
            return false;
        NBTTagCompound nbt = nmsItem.getTag();
        if (!nbt.hasKey(IDENTIFIER))
            return false;
        return nbt.getBoolean(IDENTIFIER);
    }

    @Override
    public void removeRecipe(NamespacedKey key) {
        ((CraftServer) Bukkit.getServer()).getServer().getCraftingManager().recipes.remove(new MinecraftKey(key.getNamespace(), key.getKey()));
    }

    @Override
    public String getPotionEffectName(PotionEffectType type) {
        return type.getName();
    }

    @Override
    public int getDownwardHeightLimit(World world) {
        return 0;
    }

    @Override
    public Block getHighestEmptyBlockAt(World world, int x, int z) {
        return world.getHighestBlockAt(x, z);
    }

    @Override
    public boolean isPassable(Block block) {
        return block.getType() == Material.AIR;
    }

    @Override
    public void playBreakSound(Block block) {
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_STONE_BREAK, 1, 1);
    }

    @Override
    public double getAbsorptionHearts(LivingEntity entity) {
        return ((CraftLivingEntity) entity).getHandle().getAbsorptionHearts();
    }
}
