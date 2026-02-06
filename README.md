# BlackAddons

BlackAddons is a utility and quality-of-life mod for Minecraft, designed to enhance the player experience with advanced personalization, privacy tools, and dungeon utilities.

## Features

### Utility

- **Profile Viewer**: Inspect player stats directly in-game without using external sites. View Dungeon stats, skills, slayers, and more with visualized graphs for class levels and floor completions.
- **RNG Tracker**: Automatically tracks rare drops and displays them on screen. Includes options to simulate drops for testing purposes.
- **Bot Integration**: Seamlessly syncs daily stats and integrates with external bots for leaderboard tracking.
https://github.com/BLACKUM/rtca-bot-hypixel (bot)

### Privacy & Mod Hider

- **Spoof Mode**: diverse control over how your client identifies itself to servers.
    - **Vanilla**: Pretend to be a completely vanilla client.
    - **Modded**: Identify as modded but hide specific mods from the list.
    - **Custom**: Set a custom client brand name, hide mods, and disable payloads.
- **Hide Mods**: Prevent servers from querying your detailed mod list.
- **Payload Control**: Block or whitelist custom payload channels to prevent server-side mod detection hacks and improve security. Hides Firmament payload with modanouncer.

### Cheats (AtkLxve is maintaining that, i don't take any accountability if something goes wrong)

- **AutoTNT**: Automatically interacts with specific blocks (Cracked Stone Bricks, Smooth Stone Slab) using TNT.
    - **Smart Delay**: Configurable tick delays for interactions.
    - **Safety**: Built-in anti-spam to prevent accidental multiple clicks and auto-unequip functionality.

### Quality of Life

- **Fullbright**: Toggle permanent Night Vision for better visibility in dark areas.
- **Custom GUI**: A clean, modern card-based interface for settings with movable and resizable elements.
- **Notifications**: In-game toast notifications for important events and updates.

## Commands

- `/ba`: Open the main settings GUI.
- `/ba pv [player]`: Open the Profile Viewer for a specific player.
- `/ba daily`: Sync daily stats for leaderboards.
- `/ba test GiveTNT`: Give specific testing items for verifying AutoTNT functionality.
- `/ba test rng [type] [magic_find] [item]`: Simulate an RNG drop event for testing the tracker.