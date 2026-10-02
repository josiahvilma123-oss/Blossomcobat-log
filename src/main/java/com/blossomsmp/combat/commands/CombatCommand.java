package com.blossomsmp.combat.commands;

import com.blossomsmp.combat.BlossomCombat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** /combat - shows your own combat timer. */
public final class CombatCommand implements TabExecutor {

    private final BlossomCombat plugin;

    public CombatCommand(BlossomCombat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /combat. Try /combatlog status <player>.");
            return true;
        }
        if (plugin.combat().isTagged(player)) {
            plugin.combat().send(player, plugin.settings().msg("status-in"));
        } else {
            plugin.combat().send(player, plugin.settings().msg("status-out"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
