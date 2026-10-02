package com.blossomsmp.combat;

import com.blossomsmp.combat.commands.CombatAdminCommand;
import com.blossomsmp.combat.commands.CombatCommand;
import com.blossomsmp.combat.listeners.CombatListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlossomCombat extends JavaPlugin {

    private Settings settings;
    private CombatManager combat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(getConfig());
        combat = new CombatManager(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        register("combat", new CombatCommand(this));
        register("combatlog", new CombatAdminCommand(this));

        // Count down timers 4 times a second
        getServer().getScheduler().runTaskTimer(this, combat::tick, 5L, 5L);

        if (getServer().getPluginManager().getPlugin("CombatLog") != null
                || getServer().getPluginManager().getPlugin("CombatLogX") != null) {
            getLogger().warning("Another combat log plugin is installed! Remove it, or players may be punished twice.");
        }
        getLogger().info("BlossomCombat enabled - combat time is " + settings.combatSeconds + "s");
    }

    @Override
    public void onDisable() {
        if (combat != null) {
            // Nobody gets punished for a server restart
            combat.clearAll();
        }
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    public void reloadSettings() {
        reloadConfig();
        settings = new Settings(getConfig());
        combat.hideAllBars(); // they get re-made with the new look on the next tick
    }

    public Settings settings() {
        return settings;
    }

    public CombatManager combat() {
        return combat;
    }
}
