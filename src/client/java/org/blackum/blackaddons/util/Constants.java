package org.blackum.blackaddons.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Constants {

    private Constants() {
    }

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Cloudflare bypass
    public static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; x64; rv:136.0) Gecko/20100101 Firefox/136.0";
    public static final String BOT_USER_AGENT = "BlackAddons/1.0";
    public static final int HTTP_TIMEOUT_SECONDS = 15;

    // APIs
    public static final String DEFAULT_BOT_URL = "http://hypixel-skyblock-socket.pegle.com:8080";
    public static final String HYPIXEL_BAZAAR_API = "https://api.hypixel.net/skyblock/bazaar";
    public static final String MOULBERRY_AH_API = "https://moulberry.codes/auction_averages_lbin/3day.json";
    public static final String PLAYER_DB_API = "https://playerdb.co/api/player/minecraft/";
    public static final String ADJECTILS_PROFILE_API = "https://adjectilsbackend.adjectivenoun3215.workers.dev/v2/skyblock/profiles?uuid=";
    public static final String COFL_SHINY_NECRON_HANDLE = "https://sky.coflnet.com/api/item/price/NECRON_HANDLE?IsShiny=true";
    public static final String COFL_SKELETON_MASTER_CHESTPLATE_MAX = "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?ItemTier=10-10&NoOtherValuableEnchants=true&BaseStatBoost=50";
    public static final String COFL_SKELETON_MASTER_CHESTPLATE_BASE = "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?BaseStatBoost=50";

    // Caching
    public static final long DAY_IN_MS = 24 * 60 * 60 * 1000L;
    public static final long PRICE_CACHE_DURATION_MS = DAY_IN_MS;
}
