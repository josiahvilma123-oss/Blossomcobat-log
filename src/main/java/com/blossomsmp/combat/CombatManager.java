package com.blossomsmp.combat;

import com.blossomsmp.combat.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Keeps track of who is in combat and for how long. */
public final class CombatManager {

    private final BlossomCombat plugin;
    private final Map<UUID, Long> expiresAt = new HashMap<>();
    private final Map<UUID, UUID> lastEnemy = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public CombatManager(BlossomCombat plugin) {
        this.plugin = plugin;
    }

    private Settings s() {
        return plugin.settings();
    }

    // ------------------------------------------------------------ checks

    public boolean isTagged(Player player) {
        Long end = expiresAt.get(player.getUniqueId());
        return end != null && end > System.currentTimeMillis();
    }

    public long remainingMillis(Player player) {
        Long end = expiresAt.get(player.getUniqueId());
        if (end == null) {
            return 0;
        }
        return Math.max(0, end - System.currentTimeMillis());
    }

    public int remainingSeconds(Player player) {
        return (int) Math.ceil(remainingMillis(player) / 1000.0);
    }

    public UUID getEnemy(Player player) {
        return lastEnemy.get(player.getUniqueId());
    }

    public boolean canBeTagged(Player player) {
        if (player.hasPermission("blossomcombat.bypass")) {
            return false;
        }
        if (player.hasMetadata("NPC")) {
            return false;
        }
        GameMode mode = player.getGameMode();
        if (s().ignoreCreative && (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR)) {
            return false;
        }
        return !s().disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------ tag / untag

    /** Put a player in combat (or restart their timer). enemy can be null. */
    public void tag(Player player, Player enemy) {
        if (!canBeTagged(player)) {
            return;
        }
        boolean wasTagged = isTagged(player);
        expiresAt.put(player.getUniqueId(), System.currentTimeMillis() + s().combatSeconds * 1000L);
        if (enemy != null) {
            lastEnemy.put(player.getUniqueId(), enemy.getUniqueId());
        }

        if (!wasTagged) {
            if (enemy != null) {
                send(player, s().msg("enter").replace("{enemy}", enemy.getName()));
            } else {
                send(player, s().msg("enter-no-enemy"));
            }
            playSound(player, s().soundEnter);

            if (s().blockElytra && player.isGliding()) {
                player.setGliding(false);
            }
            if (s().blockFlight && player.isFlying() && player.getGameMode() != GameMode.CREATIVE
                    && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
            }
        }
        updateDisplay(player);
    }

    /** Take a player out of combat. notify = tell them about it. */
    public void untag(Player player, boolean notify) {
        UUID id = player.getUniqueId();
        boolean was = expiresAt.remove(id) != null;
        lastEnemy.remove(id);
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            player.hideBossBar(bar);
        }
        if (was && s().actionBarEnabled) {
            player.sendActionBar(Text.color(""));
        }
        if (was && notify) {
            send(player, s().msg("leave"));
            playSound(player, s().soundLeave);
        }
    }

    /** Untag everyone who was fighting this player. */
    public void untagEnemiesOf(UUID deadPlayer) {
        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(lastEnemy.entrySet())) {
            if (deadPlayer.equals(entry.getValue())) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) {
                    untag(p, true);
                }
            }
        }
    }

    // ------------------------------------------------------------ timer

    /** Runs every few ticks: counts down timers and updates the bars. */
    public void tick() {
        for (UUID id : new ArrayList<>(expiresAt.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                expiresAt.remove(id);
                lastEnemy.remove(id);
                bossBars.remove(id);
                continue;
            }
            if (!isTagged(player)) {
                untag(player, true);
            } else {
                updateDisplay(player);
            }
        }
    }

    private void updateDisplay(Player player) {
        String time = String.valueOf(remainingSeconds(player));

        if (s().bossBarEnabled) {
            float progress = (float) (remainingMillis(player) / (s().combatSeconds * 1000.0));
            progress = Math.max(0f, Math.min(1f, progress));
            BossBar bar = bossBars.get(player.getUniqueId());
            if (bar == null) {
                bar = BossBar.bossBar(Text.color(s().bossBarText.replace("{time}", time)),
                        progress, s().bossBarColor, s().bossBarStyle);
                bossBars.put(player.getUniqueId(), bar);
                player.showBossBar(bar);
            } else {
                bar.name(Text.color(s().bossBarText.replace("{time}", time)));
                bar.progress(progress);
            }
        }

        if (s().actionBarEnabled) {
            player.sendActionBar(Text.color(s().actionBarText.replace("{time}", time)));
        }
    }

    /** Used on reload and shutdown: hide every boss bar. */
    public void hideAllBars() {
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
    }

    public void clearAll() {
        hideAllBars();
        expiresAt.clear();
        lastEnemy.clear();
    }

    // ------------------------------------------------------------ helpers

    public void send(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(Text.color(message.replace("{time}", String.valueOf(remainingSeconds(player)))));
    }

    public void playSound(Player player, String sound) {
        if (sound == null || sound.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound.trim().toLowerCase(Locale.ROOT), 1f, 1f);
        } catch (Exception ignored) {
            // Bad sound name in config - just stay quiet
        }
    }
}
