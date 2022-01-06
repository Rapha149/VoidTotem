package de.rapha149.voidtotem;

import de.rapha149.voidtotem.Config.ItemData;
import de.rapha149.voidtotem.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.rapha149.voidtotem.Messages.getMessage;

public class VoidTotemCommand implements CommandExecutor, TabCompleter {

    public VoidTotemCommand(PluginCommand command) {
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("voidtotem")) {
            sender.sendMessage(getMessage("no_permission"));
            return true;
        }

        if (args.length < 1 || !args[0].toLowerCase().matches("reload|giveitem")) {
            sender.sendMessage(getMessage("syntax").replace("%syntax%", alias + " <reload|giveitem>"));
            return true;
        }

        boolean isPlayer = sender instanceof Player;
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("voidtotem.reload")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                Messages.loadMessages();
                try {
                    if (Config.load())
                        sender.sendMessage(getMessage("reload.success"));
                    else if (isPlayer)
                        sender.sendMessage(getMessage("reload.mistakes"));
                    VoidTotem.getInstance().loadRecipe();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (isPlayer)
                        sender.sendMessage(getMessage("error"));
                }
                break;
            case "giveitem":
                if (!sender.hasPermission("voidtotem.giveitem")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                boolean self = false;
                Player target;
                if (args.length >= 2 && sender.hasPermission("voidtotem.giveitem.others")) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(getMessage("player_not_found").replace("%name%", args[1]));
                        break;
                    }
                } else if (isPlayer) {
                    self = true;
                    target = (Player) sender;
                } else {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%", alias + " giveitem <Player>"));
                    break;
                }

                VersionWrapper wrapper = VoidTotem.getInstance().wrapper;
                ItemData itemData = Config.get().item;
                ItemStack item;
                if (!itemData.customRecipe)
                    item = new ItemStack(Material.TOTEM_OF_UNDYING);
                else if (itemData.result.valid) {
                    item = wrapper.applyNBT(new ItemStack(Material.getMaterial(itemData.result.item.toUpperCase()), itemData.result.count),
                            itemData.result.nbt);
                } else {
                    sender.sendMessage(getMessage("giveitem.item_not_valid"));
                    break;
                }

                String messagePrefix = "giveitem." + (self ? "self" : "others") + ".";
                PlayerInventory inventory = target.getInventory();
                if (inventory.firstEmpty() == -1) {
                    sender.sendMessage(getMessage(messagePrefix + "no_empty_slot").replace("%player%", target.getName()));
                    break;
                }

                inventory.addItem(wrapper.addIdentifier(item));
                sender.sendMessage(getMessage(messagePrefix + "success").replace("%player%", target.getName()));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("voidtotem"))
            return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("voidtotem.reload"))
                completions.add("reload");
            if (sender.hasPermission("voidtotem.giveitem"))
                completions.add("giveitem");
        } else if (args.length == 2 && sender.hasPermission("voidtotem.giveitem") &&
                   args[0].equalsIgnoreCase("giveitem")) {
            Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(completions::add);
        }

        String arg = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(arg)).collect(Collectors.toList());
    }
}
