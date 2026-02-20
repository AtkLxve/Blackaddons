# BlackAddons

BlackAddons is a utility and quality-of-life mod for Minecraft, designed to enhance the player experience with advanced personalization, privacy tools, and dungeon utilities. I totally didn't make readme with ai. Totally.

## Features

### Utility

- **Profile Viewer**: Inspect player stats directly in-game without using external sites. View dungeon stats, skills, slayers, and more with visualized graphs for class levels and floor completions.
- **Dungeon Party Finder**: A dedicated menu for finding and creating dungeon groups. It integrates with external services to provide a more reliable experience than the standard party finder.
- **IRC Chat**: A global chat system that lets you communicate with other mod users across different servers. It features an emoji selector and handles media previews directly in the interface.
- **RNG Tracker**: Automatically tracks rare drops and displays them on screen. Includes options to simulate drops for testing purposes.
- **Bot Integration**: Seamlessly syncs daily stats and integrates with external bots for leaderboard tracking.
- https://github.com/BLACKUM/rtca-bot-hypixel (bot)

### Privacy & Mod Hider

- **Spoof Mode**: Diverse control over how your client identifies itself to servers.
    - **Vanilla**: Pretend to be a completely vanilla client.
    - **Modded**: Identify as modded but hide specific mods from the list.
    - **Custom**: Set a custom client brand name, hide mods, and disable payloads.
- **Hide Mods**: Prevent servers from querying your detailed mod list.
- **Payload Control**: Block or whitelist custom payload channels to prevent server-side mod detection hacks and improve security. Yes, even modanouncer from Firmament.

### Cheats (AtkLxve is maintaining that, i don't take any accountability if something goes wrong)

- **AutoTNT**: Automatically interacts with specific blocks (Cracked Stone Bricks, Smooth Stone Slab) using TNT.
    - **Smart Delay**: Configurable tick delays for interactions.
    - **Safety**: Built-in anti-spam to prevent accidental multiple clicks and auto-unequip functionality.

### Quality of Life

- **Fullbright**: Toggle permanent Night Vision for better visibility in dark areas.
- **Custom GUI**: A clean, modern card-based interface for settings with movable and resizable elements.
- **Notifications**: In-game toast notifications for important events and updates.

## Commands

- `/ba`, `/black`, or `/blackaddons`: Open the main settings menu.
- `/ba pf`: Open the Dungeon Party Finder.
- `/ba irc`: Open the IRC chat interface.
- `/ba pv [player] [force]`: Open the Profile Viewer for a specific player. Use the force argument to refresh cached data.
- `/ba preview [url]`: Open a full-screen preview for a direct image or Discord media link.
- `/ba daily`: Sync your daily stats for leaderboards.
- `/ba test GiveTNT`: Get specific items for testing AutoTNT functionality.
- `/ba test rng [type] [magic_find] [item]`: Simulate an RNG drop event to test the tracker.
- `/ba commandaliases add [alias] [original command]`: Create a custom command alias. You may need to swap lobbies to see changes.
- `/ba commandaliases del [alias]`: Remove a command alias.
- `/ba commandaliases list`: List all currently configured aliases.

## Credits

- **Blackum**: Lead developer
- **AtkLxve**: Cheats implementation
- **Autismo**: Assistance with architecture, Java development, and general mod improvements

