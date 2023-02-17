package de.rapha149.voidtotem.version;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.Recipes;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.Map;

public class Wrapper1_18_R2 implements VersionWrapper {

    @Override
    public boolean verifyNBT(String nbt) {
        try {
            MojangsonParser.a(nbt);
            return true;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack applyNBT(org.bukkit.inventory.ItemStack item, String nbt) {
        try {
            ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            nmsItem.u().a(MojangsonParser.a(nbt));
            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Can't read nbt string", e);
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack addIdentifier(org.bukkit.inventory.ItemStack item) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        nmsItem.u().a(IDENTIFIER, true);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public boolean hasIdentifier(org.bukkit.inventory.ItemStack item) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if (!nmsItem.s())
            return false;
        NBTTagCompound nbt = nmsItem.u();
        if (!nbt.e(IDENTIFIER))
            return false;
        return nbt.q(IDENTIFIER);
    }

    @Override
    public void removeRecipe(NamespacedKey key) {
        CraftingManager manager = ((CraftServer) Bukkit.getServer()).getServer().aC();
        MinecraftKey minecraftKey = new MinecraftKey(key.getNamespace(), key.getKey());
        manager.c.get(Recipes.a).remove(minecraftKey);

        try {
            Field field = manager.getClass().getDeclaredField("d");
            field.setAccessible(true);
            Map<MinecraftKey, IRecipe<?>> map = (Map<MinecraftKey, IRecipe<?>>) field.get(manager);
            if (!(map instanceof ImmutableMap<MinecraftKey, IRecipe<?>>))
                map.remove(minecraftKey);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPotionEffectName(PotionEffectType type) {
        return type.getKey().getKey();
    }

    @Override
    public int getDownwardHeightLimit(World world) {
        return world.getMinHeight();
    }

    @Override
    public Block getHighestEmptyBlockAt(World world, int x, int z) {
        return world.getHighestBlockAt(x, z).getRelative(BlockFace.UP);
    }

    @Override
    public boolean isPassable(Block block) {
        return block.isPassable();
    }

    @Override
    public void playBreakSound(Block block) {
        SoundGroup group = block.getBlockData().getSoundGroup();
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), group.getBreakSound(), group.getVolume(), group.getPitch());
    }

    @Override
    public double getAbsorptionHearts(LivingEntity entity) {
        return entity.getAbsorptionAmount();
    }
}
