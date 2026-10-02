package com.blossomsmp.combat.commands;

import com.blossomsmp.combat.BlossomCombat;
import com.blossomsmp.combat.Settings;
import com.blossomsmp.combat.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /combatlog reload | tag <player> | untag <player> | status <player> */
public final class CombatAdminCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("reload", "tag", "untag", "status");
    private final BlossomCombat plugin;

    public CombatAdminCommand(BlossomCombat plugin) {
        this.plugin = plugin;
    }

    private void say(CommandSender sender, String text) {
        if (text != null && !text.isEmpty()) {
            sender.sendMessage(Text.color(text));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Settings s = plugin.settings();
        if (!sender.hasPermission("blossomcombat.admin")) {
            say(sender, s.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            say(sender, s.msg("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            plugin.reloadSettings();
            say(sender, plugin.settings().msg("reloaded"));
            return true;
        }

        if (!SUBS.contains(sub) || args.length < 2) {
            say(sender, s.msg("usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            say(sender, s.msg("player-not-found"));
            return true;
        }

        switch (sub) {
            case "tag" -> {
                plugin.combat().tag(target, sender instanceof Player p && !p.equals(target) ? p : null);
                say(sender, s.msg("admin-tagged").replace("{player}", target.getName()));
            }
            case "untag" -> {
                plugin.combat().untag(target, true);
                say(sender, s.msg("admin-untagged").replace("{player}", target.getName()));
            }
            default -> {
                if (plugin.combat().isTagged(target)) {
                    say(sender, s.msg("status-other-in")
                            .replace("{player}", target.getName())
                            .replace("{time}", String.valueOf(plugin.combat().remainingSeconds(target))));
                } else {
                    say(sender, s.msg("status-other-out").replace("{player}", target.getName()));
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("blossomcombat.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String sub : SUBS) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
