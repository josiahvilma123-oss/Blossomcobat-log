# ⚔ BlossomCombat

Combat log system for Blossom SMP (Paper 1.21.11). No other plugins needed.

## What it does
- Hitting a player (sword, bow, trident, TNT, crystals) puts BOTH players in combat for 20 seconds
- The timer restarts on every hit and shows in a pink boss bar
- Logging out in combat = you die, drop your items, and your enemy gets the kill
- Blocks commands in combat (/spawn, /home, /tpa, /rtp, /ec, /ah ...)
- Blocks elytra, /fly and teleports in combat, no ender pearl cooldown by default
- Everything is editable in `config.yml` - reload with `/combatlog reload`

## Commands
- `/combat` (`/ct`) - see your combat timer
- `/combatlog reload` - reload the config
- `/combatlog tag <player>` / `untag <player>` / `status <player>`

## Permissions
- `blossomcombat.admin` - admin commands (ops have it)
- `blossomcombat.bypass` - never get tagged
- `blossomcombat.bypass.commands` - use blocked commands in combat

## Building
Upload to GitHub, the Actions tab builds `BlossomCombat.jar` automatically.
