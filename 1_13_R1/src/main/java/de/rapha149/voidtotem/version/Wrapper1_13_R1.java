package de.rapha149.voidtotem.version;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.v1_13_R1.MojangsonParser;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class Wrapper1_13_R1 implements VersionWrapper {

    @Override
    public ItemStack applyNBT(ItemStack item, String nbt) {
        try {
            net.minecraft.server.v1_13_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            nmsItem.getOrCreateTag().a(MojangsonParser.parse(nbt));
            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Can't read nbt string", e);
        }
    }

    @Override
    public ItemStack addIdentifier(ItemStack item) {
        net.minecraft.server.v1_13_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        nmsItem.getOrCreateTag().setBoolean(IDENTIFIER, true);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public boolean hasIdentifier(ItemStack item) {
        net.minecraft.server.v1_13_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        if(!nmsItem.hasTag())
            return false;
        NBTTagCompound nbt = nmsItem.getTag();
        if(!nbt.hasKey(IDENTIFIER))
            return false;
        return nbt.getBoolean(IDENTIFIER);
    }

    @Override
    public boolean isPassable(Block block) {
        return block.getType() == Material.AIR;
    }
}
