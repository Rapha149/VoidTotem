package de.rapha149.voidtotem;

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

        if (args.length < 1 || !args[0].toLowerCase().matches("reload|giveitem|isvoidtotem|makevoidtotem")) {
            sender.sendMessage(getMessage("syntax").replace("%syntax%", alias + " <reload|giveitem|isvoidtotem|makevoidtotem>"));
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
                    Util.loadRecipe();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (isPlayer)
                        sender.sendMessage(getMessage("error"));
                }
                break;
            case "giveitem": {
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

                ItemStack item = Config.get().item.result.getItemStack();
                if (item == null) {
                    sender.sendMessage(getMessage("giveitem.item_not_valid"));
                    break;
                }

                String messagePrefix = "giveitem." + (self ? "self" : "others") + ".";
                PlayerInventory inventory = target.getInventory();
                if (inventory.firstEmpty() == -1) {
                    sender.sendMessage(getMessage(messagePrefix + "no_empty_slot").replace("%player%", target.getName()));
                    break;
                }

                inventory.addItem(item);
                sender.sendMessage(getMessage(messagePrefix + "success").replace("%player%", target.getName()));
                break;
            }
            case "isvoidtotem": {
                if (!sender.hasPermission("voidtotem.isvoidtotem")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                if (!isPlayer) {
                    sender.sendMessage(getMessage("not_player"));
                    break;
                }

                if (!Config.get().item.customItem) {
                    sender.sendMessage(getMessage("isvoidtotem.no_custom_item"));
                    break;
                }

                ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
                    sender.sendMessage(getMessage("isvoidtotem.no_totem_in_hand"));
                    break;
                }

                sender.sendMessage(getMessage("isvoidtotem." + (Util.hasIdentifier(item) ? "is_void_totem" : "is_not_void_totem")));
                break;
            }
            case "makevoidtotem":{
                if (!sender.hasPermission("voidtotem.makevoidtotem")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                if (!isPlayer) {
                    sender.sendMessage(getMessage("not_player"));
                    break;
                }

                if (!Config.get().item.customItem) {
                    sender.sendMessage(getMessage("makevoidtotem.no_custom_item"));
                    break;
                }

                ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
                    sender.sendMessage(getMessage("makevoidtotem.no_totem_in_hand"));
                    break;
                }

                if (Util.hasIdentifier(item)) {
                    sender.sendMessage(getMessage("makevoidtotem.already_void_totem"));
                    break;
                }

                Util.addIdentifier(item);
                sender.sendMessage(getMessage("makevoidtotem.success"));
                break;
            }
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
            if (sender.hasPermission("voidtotem.isvoidtotem"))
                completions.add("isvoidtotem");
            if (sender.hasPermission("voidtotem.makevoidtotem"))
                completions.add("makevoidtotem");
        } else if (args.length == 2 && sender.hasPermission("voidtotem.giveitem") &&
                   args[0].equalsIgnoreCase("giveitem")) {
            Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(completions::add);
        }

        String arg = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(arg)).collect(Collectors.toList());
    }
}
