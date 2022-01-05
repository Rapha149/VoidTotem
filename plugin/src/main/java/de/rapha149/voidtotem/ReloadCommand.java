package de.rapha149.voidtotem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if(sender.hasPermission("voidtotem.reload")) {
            try {
                Config.load();
                VoidTotem.getInstance().loadRecipe();
                sender.sendMessage("§aPlugin reloaded!");
            } catch (IOException e) {
                e.printStackTrace();
                sender.sendMessage("§cAn error occured. Check the console for details.");
            }
        } else
            sender.sendMessage("§cNo permission.");
        return false;
    }
}
