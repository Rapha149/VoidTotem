package de.rapha149.voidtotem.version;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class Wrapper1_21_R1 implements VersionWrapper {

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
            NBTTagCompound itemNBT = ((NBTTagCompound) nmsItem.b(VanillaRegistries.a()));
            itemNBT.p("components").a(MojangsonParser.a(nbt));
            return CraftItemStack.asBukkitCopy(ItemStack.a(VanillaRegistries.a(), itemNBT));
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Can't read nbt string", e);
        }
    }

    @Override
    public void removeRecipe(NamespacedKey key) {
        CraftingManager manager = ((CraftServer) Bukkit.getServer()).getServer().aJ();
        MinecraftKey minecraftKey = MinecraftKey.a(key.getNamespace(), key.getKey());
        manager.removeRecipe(minecraftKey);
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
