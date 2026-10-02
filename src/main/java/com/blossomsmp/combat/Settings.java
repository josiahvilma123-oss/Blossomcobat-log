package com.blossomsmp.combat;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** All values from config.yml, read once on startup and on /combatlog reload. */
public final class Settings {

    public final int combatSeconds;

    public final boolean tagVictim;
    public final boolean tagAttacker;
    public final boolean tagOnMobDamage;
    public final boolean ignoreCreative;
    public final Set<String> disabledWorlds = new HashSet<>();

    public final boolean killOnLogout;
    public final boolean punishKicked;
    public final boolean broadcastLogout;
    public final String broadcastMessage;
    public final List<String> logoutCommands;

    public final boolean untagOnEnemyDeath;

    public final boolean blockCommands;
    public final boolean whitelistMode;
    public final Set<String> commandList = new HashSet<>();

    public final boolean blockElytra;
    public final boolean blockFlight;
    public final boolean blockPearls;
    public final int pearlCooldownSeconds;
    public final boolean blockTeleports;
    public final Set<String> blockedTeleportCauses = new HashSet<>();

    public final boolean bossBarEnabled;
    public final String bossBarText;
    public final BossBar.Color bossBarColor;
    public final BossBar.Overlay bossBarStyle;
    public final boolean actionBarEnabled;
    public final String actionBarText;

    public final String soundEnter;
    public final String soundLeave;
    public final String soundBlocked;

    private final FileConfiguration config;

    public Settings(FileConfiguration c) {
        this.config = c;

        combatSeconds = Math.max(1, c.getInt("combat-time", 20));

        tagVictim = c.getBoolean("tagging.tag-victim", true);
        tagAttacker = c.getBoolean("tagging.tag-attacker", true);
        tagOnMobDamage = c.getBoolean("tagging.tag-on-mob-damage", false);
        ignoreCreative = c.getBoolean("tagging.ignore-creative", true);
        for (String w : c.getStringList("tagging.disabled-worlds")) {
            disabledWorlds.add(w.toLowerCase(Locale.ROOT));
        }

        killOnLogout = c.getBoolean("combat-log.kill-player", true);
        punishKicked = c.getBoolean("combat-log.punish-kicked", false);
        broadcastLogout = c.getBoolean("combat-log.broadcast", true);
        broadcastMessage = c.getString("combat-log.broadcast-message", "");
        logoutCommands = new ArrayList<>(c.getStringList("combat-log.commands"));

        untagOnEnemyDeath = c.getBoolean("untag-on-enemy-death", true);

        blockCommands = c.getBoolean("commands.block", true);
        whitelistMode = "whitelist".equalsIgnoreCase(c.getString("commands.mode", "blacklist"));
        for (String cmd : c.getStringList("commands.list")) {
            String clean = cmd.trim().toLowerCase(Locale.ROOT);
            if (clean.startsWith("/")) {
                clean = clean.substring(1);
            }
            if (!clean.isEmpty()) {
                commandList.add(clean);
            }
        }
        if (whitelistMode) {
            // Players must always be able to check their own timer
            commandList.add("combat");
            commandList.add("ct");
            commandList.add("combattime");
        }

        blockElytra = c.getBoolean("restrictions.block-elytra", true);
        blockFlight = c.getBoolean("restrictions.block-flight", true);
        blockPearls = c.getBoolean("restrictions.block-ender-pearls", false);
        pearlCooldownSeconds = Math.max(0, c.getInt("restrictions.ender-pearl-cooldown", 15));
        blockTeleports = c.getBoolean("restrictions.block-teleports", true);
        for (String cause : c.getStringList("restrictions.blocked-teleport-causes")) {
            blockedTeleportCauses.add(cause.trim().toUpperCase(Locale.ROOT));
        }

        bossBarEnabled = c.getBoolean("display.boss-bar.enabled", true);
        bossBarText = c.getString("display.boss-bar.text", "&c⚔ Combat {time}s");
        bossBarColor = parseColor(c.getString("display.boss-bar.color", "PINK"));
        bossBarStyle = parseStyle(c.getString("display.boss-bar.style", "PROGRESS"));
        actionBarEnabled = c.getBoolean("display.action-bar.enabled", false);
        actionBarText = c.getString("display.action-bar.text", "&c⚔ Combat {time}s");

        soundEnter = c.getString("sounds.enter", "");
        soundLeave = c.getString("sounds.leave", "");
        soundBlocked = c.getString("sounds.blocked", "");
    }

    /** A message from the messages section, with the prefix added. */
    public String msg(String key) {
        String text = config.getString("messages." + key, "");
        if (text == null || text.isEmpty()) {
            return "";
        }
        return config.getString("messages.prefix", "") + text;
    }

    /** A message without the prefix. */
    public String raw(String key) {
        return config.getString("messages." + key, "");
    }

    private static BossBar.Color parseColor(String name) {
        try {
            return BossBar.Color.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return BossBar.Color.PINK;
        }
    }

    private static BossBar.Overlay parseStyle(String name) {
        try {
            return BossBar.Overlay.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
