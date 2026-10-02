package com.blossomsmp.combat.listeners;

import com.blossomsmp.combat.BlossomCombat;
import com.blossomsmp.combat.CombatManager;
import com.blossomsmp.combat.Settings;
import com.blossomsmp.combat.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class CombatListener implements Listener {

    private final BlossomCombat plugin;
    private final Set<UUID> kicked = new HashSet<>();
    /** True while we are killing a combat logger, so that hit doesn't tag anyone again. */
    private boolean punishing = false;

    public CombatListener(BlossomCombat plugin) {
        this.plugin = plugin;
    }

    private Settings s() {
        return plugin.settings();
    }

    private CombatManager combat() {
        return plugin.combat();
    }

    // ------------------------------------------------------------ getting tagged

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (punishing || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = findAttacker(event);

        if (attacker == null) {
            if (s().tagOnMobDamage && isMonster(event.getDamager())) {
                combat().tag(victim, null);
            }
            return;
        }
        if (attacker.equals(victim) || attacker.hasMetadata("NPC") || victim.hasMetadata("NPC")) {
            return;
        }
        if (s().tagVictim) {
            combat().tag(victim, attacker);
        }
        if (s().tagAttacker) {
            combat().tag(attacker, victim);
        }
    }

    /** Works out which player caused the damage (sword, bow, trident, TNT, crystals...). */
    private Player findAttacker(EntityDamageByEntityEvent event) {
        try {
            Entity causing = event.getDamageSource().getCausingEntity();
            if (causing instanceof Player p) {
                return p;
            }
        } catch (Throwable ignored) {
            // Fall back to the older checks below
        }
        Entity damager = event.getDamager();
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p;
        }
        return null;
    }

    private boolean isMonster(Entity damager) {
        if (damager instanceof Monster) {
            return true;
        }
        return damager instanceof Projectile projectile && projectile.getShooter() instanceof Monster;
    }

    // ------------------------------------------------------------ logging out

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        kicked.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean wasKicked = kicked.remove(player.getUniqueId());

        if (!combat().isTagged(player)) {
            combat().untag(player, false);
            return;
        }
        if (wasKicked && !s().punishKicked) {
            combat().untag(player, false);
            return;
        }

        UUID enemyId = combat().getEnemy(player);
        Player enemy = enemyId == null ? null : Bukkit.getPlayer(enemyId);
        combat().untag(player, false);

        if (s().killOnLogout) {
            killLogger(player, enemy);
        }

        if (s().broadcastLogout && s().broadcastMessage != null && !s().broadcastMessage.isEmpty()) {
            Bukkit.broadcast(Text.color(s().broadcastMessage
                    .replace("{player}", player.getName())
                    .replace("{enemy}", enemy == null ? "?" : enemy.getName())));
        }

        for (String command : s().logoutCommands) {
            String cmd = command.replace("{player}", player.getName());
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    /** Kills the player who logged out. Their items drop and the enemy gets the kill. */
    private void killLogger(Player player, Player enemy) {
        punishing = true;
        try {
            player.setNoDamageTicks(0);
            if (enemy != null && enemy.isOnline()) {
                DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                        .withCausingEntity(enemy)
                        .withDirectEntity(enemy)
                        .build();
                player.damage(1_000_000.0, source);
            }
            // If a totem, armour or another plugin saved them, finish the job
            if (!player.isDead() && player.getHealth() > 0) {
                player.setHealth(0.0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not kill combat logger " + player.getName() + ": " + e.getMessage());
            try {
                player.setHealth(0.0);
            } catch (Exception ignored) {
            }
        } finally {
            punishing = false;
        }
    }

    // ------------------------------------------------------------ dying

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        combat().untag(dead, false);
        if (s().untagOnEnemyDeath) {
            combat().untagEnemiesOf(dead.getUniqueId());
        }
    }

    // ------------------------------------------------------------ blocked commands

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!s().blockCommands || !combat().isTagged(player)
                || player.hasPermission("blossomcombat.bypass.commands")) {
            return;
        }
        String message = event.getMessage();
        if (message.length() < 2) {
            return;
        }
        String label = message.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1); // "/essentials:home" -> "home"
        }

        boolean inList = s().commandList.contains(label);
        boolean block = s().whitelistMode ? !inList : inList;
        if (block) {
            event.setCancelled(true);
            blocked(player, "blocked-command");
        }
    }

    // ------------------------------------------------------------ elytra / flying

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.isGliding()) {
            return;
        }
        if (s().blockElytra && combat().isTagged(player)) {
            event.setCancelled(true);
            blocked(player, "blocked-elytra");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFly(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!event.isFlying() || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (s().blockFlight && combat().isTagged(player)) {
            event.setCancelled(true);
            blocked(player, "blocked-flight");
        }
    }

    // ------------------------------------------------------------ ender pearls

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl) || !(pearl.getShooter() instanceof Player player)) {
            return;
        }
        if (!combat().isTagged(player)) {
            return;
        }
        if (s().blockPearls) {
            event.setCancelled(true);
            blocked(player, "blocked-pearl");
            return;
        }
        int seconds = s().pearlCooldownSeconds;
        if (seconds > 0) {
            // Next tick, so it replaces the normal 1 second cooldown
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setCooldown(Material.ENDER_PEARL, seconds * 20);
                }
            });
        }
    }

    // ------------------------------------------------------------ teleports

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!s().blockTeleports || !combat().isTagged(player)) {
            return;
        }
        if (s().blockedTeleportCauses.contains(event.getCause().name())) {
            event.setCancelled(true);
            blocked(player, "blocked-teleport");
        }
    }

    // ------------------------------------------------------------ helper

    private void blocked(Player player, String messageKey) {
        combat().send(player, s().msg(messageKey));
        combat().playSound(player, s().soundBlocked);
    }
}
