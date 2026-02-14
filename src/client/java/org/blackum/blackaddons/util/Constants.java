package org.blackum.blackaddons.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Constants {

    private Constants() {
    }

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Bot link
    public static final String DISCORD_AUTH_URL = "https://discord.com/oauth2/authorize?client_id=1134507219220713472";

    // Cloudflare bypass
    public static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; x64; rv:136.0) Gecko/20100101 Firefox/136.0";
    public static final String BOT_USER_AGENT = "BlackAddons/1.0";
    public static final int HTTP_TIMEOUT_SECONDS = 15;

    // Mod Metadata
    public static final String MOD_ID = "blackaddons";
    public static final String CONFIG_DIR_NAME = MOD_ID; // bruh
    public static final String PRICES_FILE_NAME = "prices.json";
    public static final String TEAMMATES_FILE_NAME = "teammates.json";
    public static final String RNG_DATA_FILE_NAME = "rng_data.json";
    public static final String BLOCKED_PACKETS_LOG_NAME = "blocked_packets.log";

    // APIs
    public static final String DEFAULT_BOT_URL = "http://hypixel-skyblock-socket.pegle.com:8080";
    public static final String HYPIXEL_BAZAAR_API = "https://api.hypixel.net/skyblock/bazaar";
    public static final String MOULBERRY_AH_API = "https://moulberry.codes/auction_averages_lbin/3day.json";
    public static final String PLAYER_DB_API = "https://playerdb.co/api/player/minecraft/";
    public static final String ADJECTILS_PROFILE_API = "https://adjectilsbackend.adjectivenoun3215.workers.dev/v2/skyblock/profiles?uuid=";
    public static final String COFL_SHINY_NECRON_HANDLE = "https://sky.coflnet.com/api/item/price/NECRON_HANDLE?IsShiny=true";
    public static final String COFL_SKELETON_MASTER_CHESTPLATE_MAX = "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?ItemTier=10-10&NoOtherValuableEnchants=true&BaseStatBoost=50";
    public static final String COFL_SKELETON_MASTER_CHESTPLATE_BASE = "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?BaseStatBoost=50";

    // Bot Endpoints
    public static final String BOT_API_RNG = "/v1/rng";
    public static final String BOT_API_DAILY = "/v1/daily";
    public static final String BOT_API_PROFILE = "/v1/profile";
    public static final String BOT_API_RTCA = "/v1/rtca";
    public static final String BOT_API_LEADERBOARD = "/v1/leaderboard";
    public static final String BOT_API_KEY = "/v1/key";

    // API Headers
    public static final String HEADER_ENCRYPTED_IDENTITY = "X-Encrypted-Identity";
    public static final String HEADER_DEVELOPER_KEY = "X-Developer-Key";

    // Catacombs Data
    public static final double CATA_50_XP = 569809640.0;
    public static final java.util.List<Double> DUNGEON_XP = java.util.List.of(
            0.0, 50.0, 75.0, 110.0, 160.0, 230.0, 330.0, 470.0, 670.0, 950.0, 1340.0,
            1890.0, 2665.0, 3760.0, 5260.0, 7380.0, 10300.0, 14400.0, 20000.0, 27600.0,
            38000.0, 52500.0, 71500.0, 97000.0, 132000.0, 180000.0, 243000.0, 328000.0,
            445000.0, 600000.0, 800000.0, 1065000.0, 1410000.0, 1900000.0, 2500000.0,
            3300000.0, 4300000.0, 5600000.0, 7200000.0, 9200000.0, 12000000.0, 15000000.0,
            19000000.0, 24000000.0, 30000000.0, 38000000.0, 48000000.0, 60000000.0, 75000000.0,
            93000000.0, 116250000.0, 200000000.0);

    public static final java.util.Map<String, Integer> FLOOR_XP_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("M7", 300000), java.util.Map.entry("M6", 100000), java.util.Map.entry("M5", 70000),
            java.util.Map.entry("M4", 55000), java.util.Map.entry("M3", 35000), java.util.Map.entry("M2", 20000),
            java.util.Map.entry("M1", 15000),
            java.util.Map.entry("F7", 28000), java.util.Map.entry("F6", 4880), java.util.Map.entry("F5", 2400),
            java.util.Map.entry("F4", 1420), java.util.Map.entry("F3", 560), java.util.Map.entry("F2", 220),
            java.util.Map.entry("F1", 110), java.util.Map.entry("Entrance", 55));

    // RNG Display Keywords
    public static final String RARE_DROP_KEYWORD = "RARE DROP!";
    public static final String PRAY_DROP_KEYWORD = "PRAY TO RNGESUS DROP!";
    public static final String MAGIC_FIND_LABEL = "✯ Magic Find";

    // Command Constants
    public static final String CMD_ARG_TYPE = "type";
    public static final String CMD_ARG_MAGIC_FIND = "magic_find";
    public static final String CMD_ARG_ITEM = "item";
    public static final String CMD_ARG_IGN = "ign";

    public static final String DROP_TYPE_RARE = "rare";
    public static final String DROP_TYPE_CRAZY = "crazy";
    public static final String DROP_TYPE_PRAY = "pray";

    // Caching
    public static final long DAY_IN_MS = 24 * 60 * 60 * 1000L;
    public static final long PRICE_CACHE_DURATION_MS = DAY_IN_MS;
}
