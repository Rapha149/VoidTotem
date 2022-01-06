package de.rapha149.voidtotem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("voidtotem.reload")) {
            try {
                if (Config.load())
                    sender.sendMessage("§aConfig reloaded!");
                else if (sender instanceof Player)
                    sender.sendMessage("§6You made some mistakes in the config, check the console for details.");
                VoidTotem.getInstance().loadRecipe();
            } catch (IOException e) {
                e.printStackTrace();
                if (sender instanceof Player)
                    sender.sendMessage("§cAn error occured. Check the console for details.");
            }
        } else
            sender.sendMessage("§cNo permission.");
        return false;
    }
}
