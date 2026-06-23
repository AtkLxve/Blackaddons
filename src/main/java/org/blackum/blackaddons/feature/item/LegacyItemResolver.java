package org.blackum.blackaddons.feature.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.blackum.blackaddons.Blackaddons;

public class LegacyItemResolver {
    private static final int ID_TABLE_SIZE = 426;
    private static final Item[] ID_TABLE = new Item[ID_TABLE_SIZE];

//? if >=26.2 {
/*
    private static final Item[] WOOL = {
            (Item) Items.WOOL.white(), (Item) Items.WOOL.orange(), (Item) Items.WOOL.magenta(), (Item) Items.WOOL.lightBlue(),
            (Item) Items.WOOL.yellow(), (Item) Items.WOOL.lime(), (Item) Items.WOOL.pink(), (Item) Items.WOOL.gray(),
            (Item) Items.WOOL.lightGray(), (Item) Items.WOOL.cyan(), (Item) Items.WOOL.purple(), (Item) Items.WOOL.blue(),
            (Item) Items.WOOL.brown(), (Item) Items.WOOL.green(), (Item) Items.WOOL.red(), (Item) Items.WOOL.black()
    };
    private static final Item[] STAINED_GLASS = {
            (Item) Items.STAINED_GLASS.white(), (Item) Items.STAINED_GLASS.orange(), (Item) Items.STAINED_GLASS.magenta(), (Item) Items.STAINED_GLASS.lightBlue(),
            (Item) Items.STAINED_GLASS.yellow(), (Item) Items.STAINED_GLASS.lime(), (Item) Items.STAINED_GLASS.pink(), (Item) Items.STAINED_GLASS.gray(),
            (Item) Items.STAINED_GLASS.lightGray(), (Item) Items.STAINED_GLASS.cyan(), (Item) Items.STAINED_GLASS.purple(), (Item) Items.STAINED_GLASS.blue(),
            (Item) Items.STAINED_GLASS.brown(), (Item) Items.STAINED_GLASS.green(), (Item) Items.STAINED_GLASS.red(), (Item) Items.STAINED_GLASS.black()
    };
    private static final Item[] STAINED_GLASS_PANE = {
            (Item) Items.STAINED_GLASS_PANE.white(), (Item) Items.STAINED_GLASS_PANE.orange(), (Item) Items.STAINED_GLASS_PANE.magenta(), (Item) Items.STAINED_GLASS_PANE.lightBlue(),
            (Item) Items.STAINED_GLASS_PANE.yellow(), (Item) Items.STAINED_GLASS_PANE.lime(), (Item) Items.STAINED_GLASS_PANE.pink(), (Item) Items.STAINED_GLASS_PANE.gray(),
            (Item) Items.STAINED_GLASS_PANE.lightGray(), (Item) Items.STAINED_GLASS_PANE.cyan(), (Item) Items.STAINED_GLASS_PANE.purple(), (Item) Items.STAINED_GLASS_PANE.blue(),
            (Item) Items.STAINED_GLASS_PANE.brown(), (Item) Items.STAINED_GLASS_PANE.green(), (Item) Items.STAINED_GLASS_PANE.red(), (Item) Items.STAINED_GLASS_PANE.black()
    };
    private static final Item[] CARPET = {
            (Item) Items.CARPET.white(), (Item) Items.CARPET.orange(), (Item) Items.CARPET.magenta(), (Item) Items.CARPET.lightBlue(),
            (Item) Items.CARPET.yellow(), (Item) Items.CARPET.lime(), (Item) Items.CARPET.pink(), (Item) Items.CARPET.gray(),
            (Item) Items.CARPET.lightGray(), (Item) Items.CARPET.cyan(), (Item) Items.CARPET.purple(), (Item) Items.CARPET.blue(),
            (Item) Items.CARPET.brown(), (Item) Items.CARPET.green(), (Item) Items.CARPET.red(), (Item) Items.CARPET.black()
    };
    private static final Item[] BANNER = {
            (Item) Items.BANNER.white(), (Item) Items.BANNER.orange(), (Item) Items.BANNER.magenta(), (Item) Items.BANNER.lightBlue(),
            (Item) Items.BANNER.yellow(), (Item) Items.BANNER.lime(), (Item) Items.BANNER.pink(), (Item) Items.BANNER.gray(),
            (Item) Items.BANNER.lightGray(), (Item) Items.BANNER.cyan(), (Item) Items.BANNER.purple(), (Item) Items.BANNER.blue(),
            (Item) Items.BANNER.brown(), (Item) Items.BANNER.green(), (Item) Items.BANNER.red(), (Item) Items.BANNER.black()
    };
    private static final Item[] TERRACOTTA = {
            (Item) Items.DYED_TERRACOTTA.white(), (Item) Items.DYED_TERRACOTTA.orange(), (Item) Items.DYED_TERRACOTTA.magenta(), (Item) Items.DYED_TERRACOTTA.lightBlue(),
            (Item) Items.DYED_TERRACOTTA.yellow(), (Item) Items.DYED_TERRACOTTA.lime(), (Item) Items.DYED_TERRACOTTA.pink(), (Item) Items.DYED_TERRACOTTA.gray(),
            (Item) Items.DYED_TERRACOTTA.lightGray(), (Item) Items.DYED_TERRACOTTA.cyan(), (Item) Items.DYED_TERRACOTTA.purple(), (Item) Items.DYED_TERRACOTTA.blue(),
            (Item) Items.DYED_TERRACOTTA.brown(), (Item) Items.DYED_TERRACOTTA.green(), (Item) Items.DYED_TERRACOTTA.red(), (Item) Items.DYED_TERRACOTTA.black()
    };
*/
//?} else {
    private static final Item[] WOOL = {
            Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL,
            Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
            Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL
    };
    private static final Item[] STAINED_GLASS = {
            Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS,
            Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS, Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS,
            Items.LIGHT_GRAY_STAINED_GLASS, Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
            Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.RED_STAINED_GLASS, Items.BLACK_STAINED_GLASS
    };
    private static final Item[] TERRACOTTA = {
            Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA,
            Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA,
            Items.LIGHT_GRAY_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
            Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA
    };
    private static final Item[] STAINED_GLASS_PANE = {
            Items.WHITE_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE,
            Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE, Items.PINK_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE,
            Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.CYAN_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
            Items.BROWN_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE, Items.BLACK_STAINED_GLASS_PANE
    };
    private static final Item[] CARPET = {
            Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET, Items.LIGHT_BLUE_CARPET,
            Items.YELLOW_CARPET, Items.LIME_CARPET, Items.PINK_CARPET, Items.GRAY_CARPET,
            Items.LIGHT_GRAY_CARPET, Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
            Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET
    };
    private static final Item[] BANNER = {
            Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER, Items.LIGHT_BLUE_BANNER,
            Items.YELLOW_BANNER, Items.LIME_BANNER, Items.PINK_BANNER, Items.GRAY_BANNER,
            Items.LIGHT_GRAY_BANNER, Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER,
            Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER
    };
//?}

//? if >=26.2 {
/*
    private static final Item WHITE_BED = (Item) Items.BED.white();
    private static Item getDye(int damage) {
        return switch (damage) {
            case 1 -> (Item) Items.DYE.red();
            case 2 -> (Item) Items.DYE.green();
            case 3 -> Items.COCOA_BEANS;
            case 4 -> Items.LAPIS_LAZULI;
            case 5 -> (Item) Items.DYE.purple();
            case 6 -> (Item) Items.DYE.cyan();
            case 7 -> (Item) Items.DYE.lightGray();
            case 8 -> (Item) Items.DYE.gray();
            case 9 -> (Item) Items.DYE.pink();
            case 10 -> (Item) Items.DYE.lime();
            case 11 -> (Item) Items.DYE.yellow();
            case 12 -> (Item) Items.DYE.lightBlue();
            case 13 -> (Item) Items.DYE.magenta();
            case 14 -> (Item) Items.DYE.orange();
            case 15 -> Items.BONE_MEAL;
            default -> Items.INK_SAC;
        };
    }
*/
//?} else {
    private static final Item WHITE_BED = Items.WHITE_BED;
    private static Item getDye(int damage) {
        return switch (damage) {
            case 1 -> Items.RED_DYE;
            case 2 -> Items.GREEN_DYE;
            case 3 -> Items.COCOA_BEANS;
            case 4 -> Items.LAPIS_LAZULI;
            case 5 -> Items.PURPLE_DYE;
            case 6 -> Items.CYAN_DYE;
            case 7 -> Items.LIGHT_GRAY_DYE;
            case 8 -> Items.GRAY_DYE;
            case 9 -> Items.PINK_DYE;
            case 10 -> Items.LIME_DYE;
            case 11 -> Items.YELLOW_DYE;
            case 12 -> Items.LIGHT_BLUE_DYE;
            case 13 -> Items.MAGENTA_DYE;
            case 14 -> Items.ORANGE_DYE;
            case 15 -> Items.BONE_MEAL;
            default -> Items.INK_SAC;
        };
    }
//?}

    static {
        ID_TABLE[1] = Items.STONE;
        ID_TABLE[2] = Items.GRASS_BLOCK;
        ID_TABLE[3] = Items.DIRT;
        ID_TABLE[4] = Items.COBBLESTONE;
        ID_TABLE[5] = Items.OAK_PLANKS;
        ID_TABLE[6] = Items.OAK_SAPLING;
        ID_TABLE[7] = Items.BEDROCK;
        ID_TABLE[8] = Items.WATER_BUCKET;
        ID_TABLE[9] = Items.WATER_BUCKET;
        ID_TABLE[10] = Items.LAVA_BUCKET;
        ID_TABLE[11] = Items.LAVA_BUCKET;
        ID_TABLE[12] = Items.SAND;
        ID_TABLE[13] = Items.GRAVEL;
        ID_TABLE[14] = Items.GOLD_ORE;
        ID_TABLE[15] = Items.IRON_ORE;
        ID_TABLE[16] = Items.COAL_ORE;
        ID_TABLE[17] = Items.OAK_LOG;
        ID_TABLE[18] = Items.OAK_LEAVES;
        ID_TABLE[19] = Items.SPONGE;
        ID_TABLE[20] = Items.GLASS;
        ID_TABLE[21] = Items.LAPIS_ORE;
        ID_TABLE[22] = Items.LAPIS_BLOCK;
        ID_TABLE[23] = Items.DISPENSER;
        ID_TABLE[24] = Items.SANDSTONE;
        ID_TABLE[25] = Items.NOTE_BLOCK;
        ID_TABLE[26] = WHITE_BED;
        ID_TABLE[27] = Items.POWERED_RAIL;
        ID_TABLE[28] = Items.DETECTOR_RAIL;
        ID_TABLE[29] = Items.STICKY_PISTON;
        ID_TABLE[30] = Items.COBWEB;
        ID_TABLE[31] = Items.SHORT_GRASS;
        ID_TABLE[32] = Items.DEAD_BUSH;
        ID_TABLE[33] = Items.PISTON;
        ID_TABLE[35] = WOOL[0];
        ID_TABLE[37] = Items.DANDELION;
        ID_TABLE[38] = Items.POPPY;
        ID_TABLE[39] = Items.BROWN_MUSHROOM;
        ID_TABLE[40] = Items.RED_MUSHROOM;
        ID_TABLE[41] = Items.GOLD_BLOCK;
        ID_TABLE[42] = Items.IRON_BLOCK;
        ID_TABLE[43] = Items.STONE_SLAB;
        ID_TABLE[44] = Items.STONE_SLAB;
        ID_TABLE[45] = Items.BRICKS;
        ID_TABLE[46] = Items.TNT;
        ID_TABLE[47] = Items.BOOKSHELF;
        ID_TABLE[48] = Items.MOSSY_COBBLESTONE;
        ID_TABLE[49] = Items.OBSIDIAN;
        ID_TABLE[50] = Items.TORCH;
        ID_TABLE[51] = Items.FIRE_CHARGE;
        ID_TABLE[52] = Items.SPAWNER;
        ID_TABLE[53] = Items.OAK_STAIRS;
        ID_TABLE[54] = Items.CHEST;
        ID_TABLE[56] = Items.DIAMOND_ORE;
        ID_TABLE[57] = Items.DIAMOND_BLOCK;
        ID_TABLE[58] = Items.CRAFTING_TABLE;
        ID_TABLE[59] = Items.WHEAT;
        ID_TABLE[60] = Items.FARMLAND;
        ID_TABLE[61] = Items.FURNACE;
        ID_TABLE[63] = Items.OAK_SIGN;
        ID_TABLE[64] = Items.OAK_DOOR;
        ID_TABLE[65] = Items.LADDER;
        ID_TABLE[66] = Items.RAIL;
        ID_TABLE[67] = Items.STONE_STAIRS;
        ID_TABLE[68] = Items.OAK_SIGN;
        ID_TABLE[69] = Items.LEVER;
        ID_TABLE[70] = Items.STONE_PRESSURE_PLATE;
        ID_TABLE[71] = Items.IRON_DOOR;
        ID_TABLE[72] = Items.OAK_PRESSURE_PLATE;
        ID_TABLE[73] = Items.REDSTONE_ORE;
        ID_TABLE[76] = Items.REDSTONE_TORCH;
        ID_TABLE[77] = Items.STONE_BUTTON;
        ID_TABLE[78] = Items.SNOW;
        ID_TABLE[79] = Items.ICE;
        ID_TABLE[80] = Items.SNOW_BLOCK;
        ID_TABLE[81] = Items.CACTUS;
        ID_TABLE[82] = Items.CLAY;
        ID_TABLE[83] = Items.SUGAR_CANE;
        ID_TABLE[84] = Items.JUKEBOX;
        ID_TABLE[85] = Items.OAK_FENCE;
        ID_TABLE[86] = Items.PUMPKIN;
        ID_TABLE[87] = Items.NETHERRACK;
        ID_TABLE[88] = Items.SOUL_SAND;
        ID_TABLE[89] = Items.GLOWSTONE;
        ID_TABLE[91] = Items.CARVED_PUMPKIN;
        ID_TABLE[95] = STAINED_GLASS[0];
        ID_TABLE[96] = Items.OAK_TRAPDOOR;
        ID_TABLE[98] = Items.STONE_BRICKS;
        ID_TABLE[101] = Items.IRON_BARS;
        ID_TABLE[102] = Items.GLASS_PANE;
        ID_TABLE[103] = Items.MELON;
        ID_TABLE[106] = Items.VINE;
        ID_TABLE[107] = Items.OAK_FENCE_GATE;
        ID_TABLE[108] = Items.BRICK_STAIRS;
        ID_TABLE[109] = Items.STONE_BRICK_STAIRS;
        ID_TABLE[110] = Items.MYCELIUM;
        ID_TABLE[111] = Items.LILY_PAD;
        ID_TABLE[112] = Items.NETHER_BRICKS;
        ID_TABLE[113] = Items.NETHER_BRICK_FENCE;
        ID_TABLE[114] = Items.NETHER_BRICK_STAIRS;
        ID_TABLE[115] = Items.NETHER_WART;
        ID_TABLE[116] = Items.ENCHANTING_TABLE;
        ID_TABLE[117] = Items.BREWING_STAND;
        ID_TABLE[118] = Items.CAULDRON;
        ID_TABLE[120] = Items.END_PORTAL_FRAME;
        ID_TABLE[121] = Items.END_STONE;
        ID_TABLE[122] = Items.DRAGON_EGG;
        ID_TABLE[123] = Items.REDSTONE_LAMP;
        ID_TABLE[126] = Items.OAK_SLAB;
        ID_TABLE[128] = Items.SANDSTONE_STAIRS;
        ID_TABLE[129] = Items.EMERALD_ORE;
        ID_TABLE[130] = Items.ENDER_CHEST;
        ID_TABLE[131] = Items.TRIPWIRE_HOOK;
        ID_TABLE[133] = Items.EMERALD_BLOCK;
        ID_TABLE[134] = Items.SPRUCE_STAIRS;
        ID_TABLE[135] = Items.BIRCH_STAIRS;
        ID_TABLE[136] = Items.JUNGLE_STAIRS;
        ID_TABLE[138] = Items.BEACON;
        ID_TABLE[139] = Items.COBBLESTONE_WALL;
        ID_TABLE[140] = Items.FLOWER_POT;
        ID_TABLE[143] = Items.OAK_BUTTON;
        ID_TABLE[145] = Items.ANVIL;
        ID_TABLE[146] = Items.TRAPPED_CHEST;
        ID_TABLE[147] = Items.LIGHT_WEIGHTED_PRESSURE_PLATE;
        ID_TABLE[148] = Items.HEAVY_WEIGHTED_PRESSURE_PLATE;
        ID_TABLE[151] = Items.DAYLIGHT_DETECTOR;
        ID_TABLE[152] = Items.REDSTONE_BLOCK;
        ID_TABLE[153] = Items.NETHER_QUARTZ_ORE;
        ID_TABLE[154] = Items.HOPPER;
        ID_TABLE[155] = Items.QUARTZ_BLOCK;
        ID_TABLE[156] = Items.QUARTZ_STAIRS;
        ID_TABLE[157] = Items.ACTIVATOR_RAIL;
        ID_TABLE[158] = Items.DROPPER;
        ID_TABLE[159] = TERRACOTTA[0];
        ID_TABLE[160] = STAINED_GLASS_PANE[0];
        ID_TABLE[161] = Items.ACACIA_LEAVES;
        ID_TABLE[162] = Items.ACACIA_LOG;
        ID_TABLE[163] = Items.ACACIA_STAIRS;
        ID_TABLE[164] = Items.DARK_OAK_STAIRS;
        ID_TABLE[165] = Items.SLIME_BLOCK;
        ID_TABLE[166] = Items.BARRIER;
        ID_TABLE[167] = Items.IRON_TRAPDOOR;
        ID_TABLE[168] = Items.PRISMARINE;
        ID_TABLE[169] = Items.SEA_LANTERN;
        ID_TABLE[170] = Items.HAY_BLOCK;
        ID_TABLE[171] = CARPET[0];
        ID_TABLE[172] = Items.TERRACOTTA;
        ID_TABLE[173] = Items.COAL_BLOCK;
        ID_TABLE[174] = Items.PACKED_ICE;
        ID_TABLE[175] = Items.SUNFLOWER;

        ID_TABLE[256] = Items.IRON_SHOVEL;
        ID_TABLE[257] = Items.IRON_PICKAXE;
        ID_TABLE[258] = Items.IRON_AXE;
        ID_TABLE[259] = Items.FLINT_AND_STEEL;
        ID_TABLE[260] = Items.APPLE;
        ID_TABLE[261] = Items.BOW;
        ID_TABLE[262] = Items.ARROW;
        ID_TABLE[263] = Items.COAL;
        ID_TABLE[264] = Items.DIAMOND;
        ID_TABLE[265] = Items.IRON_INGOT;
        ID_TABLE[266] = Items.GOLD_INGOT;
        ID_TABLE[267] = Items.IRON_SWORD;
        ID_TABLE[268] = Items.WOODEN_SWORD;
        ID_TABLE[269] = Items.WOODEN_SHOVEL;
        ID_TABLE[270] = Items.WOODEN_PICKAXE;
        ID_TABLE[271] = Items.WOODEN_AXE;
        ID_TABLE[272] = Items.STONE_SWORD;
        ID_TABLE[273] = Items.STONE_SHOVEL;
        ID_TABLE[274] = Items.STONE_PICKAXE;
        ID_TABLE[275] = Items.STONE_AXE;
        ID_TABLE[276] = Items.DIAMOND_SWORD;
        ID_TABLE[277] = Items.DIAMOND_SHOVEL;
        ID_TABLE[278] = Items.DIAMOND_PICKAXE;
        ID_TABLE[279] = Items.DIAMOND_AXE;
        ID_TABLE[280] = Items.STICK;
        ID_TABLE[281] = Items.BOWL;
        ID_TABLE[282] = Items.MUSHROOM_STEW;
        ID_TABLE[283] = Items.GOLDEN_SWORD;
        ID_TABLE[284] = Items.GOLDEN_SHOVEL;
        ID_TABLE[285] = Items.GOLDEN_PICKAXE;
        ID_TABLE[286] = Items.GOLDEN_AXE;
        ID_TABLE[287] = Items.STRING;
        ID_TABLE[288] = Items.FEATHER;
        ID_TABLE[289] = Items.GUNPOWDER;
        ID_TABLE[290] = Items.WOODEN_HOE;
        ID_TABLE[291] = Items.STONE_HOE;
        ID_TABLE[292] = Items.IRON_HOE;
        ID_TABLE[293] = Items.DIAMOND_HOE;
        ID_TABLE[294] = Items.GOLDEN_HOE;
        ID_TABLE[295] = Items.WHEAT_SEEDS;
        ID_TABLE[296] = Items.WHEAT;
        ID_TABLE[297] = Items.BREAD;
        ID_TABLE[298] = Items.LEATHER_HELMET;
        ID_TABLE[299] = Items.LEATHER_CHESTPLATE;
        ID_TABLE[300] = Items.LEATHER_LEGGINGS;
        ID_TABLE[301] = Items.LEATHER_BOOTS;
        ID_TABLE[302] = Items.CHAINMAIL_HELMET;
        ID_TABLE[303] = Items.CHAINMAIL_CHESTPLATE;
        ID_TABLE[304] = Items.CHAINMAIL_LEGGINGS;
        ID_TABLE[305] = Items.CHAINMAIL_BOOTS;
        ID_TABLE[306] = Items.IRON_HELMET;
        ID_TABLE[307] = Items.IRON_CHESTPLATE;
        ID_TABLE[308] = Items.IRON_LEGGINGS;
        ID_TABLE[309] = Items.IRON_BOOTS;
        ID_TABLE[310] = Items.DIAMOND_HELMET;
        ID_TABLE[311] = Items.DIAMOND_CHESTPLATE;
        ID_TABLE[312] = Items.DIAMOND_LEGGINGS;
        ID_TABLE[313] = Items.DIAMOND_BOOTS;
        ID_TABLE[314] = Items.GOLDEN_HELMET;
        ID_TABLE[315] = Items.GOLDEN_CHESTPLATE;
        ID_TABLE[316] = Items.GOLDEN_LEGGINGS;
        ID_TABLE[317] = Items.GOLDEN_BOOTS;
        ID_TABLE[318] = Items.FLINT;
        ID_TABLE[319] = Items.PORKCHOP;
        ID_TABLE[320] = Items.COOKED_PORKCHOP;
        ID_TABLE[321] = Items.PAINTING;
        ID_TABLE[322] = Items.GOLDEN_APPLE;
        ID_TABLE[323] = Items.OAK_SIGN;
        ID_TABLE[324] = Items.OAK_DOOR;
        ID_TABLE[325] = Items.BUCKET;
        ID_TABLE[326] = Items.WATER_BUCKET;
        ID_TABLE[327] = Items.LAVA_BUCKET;
        ID_TABLE[328] = Items.MINECART;
        ID_TABLE[329] = Items.SADDLE;
        ID_TABLE[330] = Items.IRON_DOOR;
        ID_TABLE[331] = Items.REDSTONE;
        ID_TABLE[332] = Items.SNOWBALL;
        ID_TABLE[333] = Items.OAK_BOAT;
        ID_TABLE[334] = Items.LEATHER;
        ID_TABLE[335] = Items.MILK_BUCKET;
        ID_TABLE[336] = Items.BRICK;
        ID_TABLE[337] = Items.CLAY_BALL;
        ID_TABLE[338] = Items.SUGAR_CANE;
        ID_TABLE[339] = Items.PAPER;
        ID_TABLE[340] = Items.BOOK;
        ID_TABLE[341] = Items.SLIME_BALL;
        ID_TABLE[342] = Items.CHEST_MINECART;
        ID_TABLE[343] = Items.FURNACE_MINECART;
        ID_TABLE[344] = Items.EGG;
        ID_TABLE[345] = Items.COMPASS;
        ID_TABLE[346] = Items.FISHING_ROD;
        ID_TABLE[347] = Items.CLOCK;
        ID_TABLE[348] = Items.GLOWSTONE_DUST;
        ID_TABLE[349] = Items.COD;
        ID_TABLE[350] = Items.COOKED_COD;
        ID_TABLE[351] = Items.INK_SAC;
        ID_TABLE[352] = Items.BONE;
        ID_TABLE[353] = Items.SUGAR;
        ID_TABLE[354] = Items.CAKE;
        ID_TABLE[355] = WHITE_BED;
        ID_TABLE[356] = Items.REPEATER;
        ID_TABLE[357] = Items.COOKIE;
        ID_TABLE[358] = Items.FILLED_MAP;
        ID_TABLE[359] = Items.SHEARS;
        ID_TABLE[360] = Items.MELON_SLICE;
        ID_TABLE[361] = Items.PUMPKIN_SEEDS;
        ID_TABLE[362] = Items.MELON_SEEDS;
        ID_TABLE[363] = Items.BEEF;
        ID_TABLE[364] = Items.COOKED_BEEF;
        ID_TABLE[365] = Items.CHICKEN;
        ID_TABLE[366] = Items.COOKED_CHICKEN;
        ID_TABLE[367] = Items.ROTTEN_FLESH;
        ID_TABLE[368] = Items.ENDER_PEARL;
        ID_TABLE[369] = Items.BLAZE_ROD;
        ID_TABLE[370] = Items.GHAST_TEAR;
        ID_TABLE[371] = Items.GOLD_NUGGET;
        ID_TABLE[372] = Items.NETHER_WART;
        ID_TABLE[373] = Items.POTION;
        ID_TABLE[374] = Items.GLASS_BOTTLE;
        ID_TABLE[375] = Items.SPIDER_EYE;
        ID_TABLE[376] = Items.FERMENTED_SPIDER_EYE;
        ID_TABLE[377] = Items.BLAZE_POWDER;
        ID_TABLE[378] = Items.MAGMA_CREAM;
        ID_TABLE[379] = Items.BREWING_STAND;
        ID_TABLE[380] = Items.CAULDRON;
        ID_TABLE[381] = Items.ENDER_EYE;
        ID_TABLE[382] = Items.GLISTERING_MELON_SLICE;
        ID_TABLE[383] = Items.PIG_SPAWN_EGG;
        ID_TABLE[384] = Items.EXPERIENCE_BOTTLE;
        ID_TABLE[385] = Items.FIRE_CHARGE;
        ID_TABLE[386] = Items.WRITABLE_BOOK;
        ID_TABLE[387] = Items.WRITTEN_BOOK;
        ID_TABLE[388] = Items.EMERALD;
        ID_TABLE[389] = Items.ITEM_FRAME;
        ID_TABLE[390] = Items.FLOWER_POT;
        ID_TABLE[391] = Items.CARROT;
        ID_TABLE[392] = Items.POTATO;
        ID_TABLE[393] = Items.BAKED_POTATO;
        ID_TABLE[394] = Items.POISONOUS_POTATO;
        ID_TABLE[395] = Items.MAP;
        ID_TABLE[396] = Items.GOLDEN_CARROT;
        ID_TABLE[397] = Items.PLAYER_HEAD;
        ID_TABLE[398] = Items.CARROT_ON_A_STICK;
        ID_TABLE[399] = Items.NETHER_STAR;
        ID_TABLE[400] = Items.PUMPKIN_PIE;
        ID_TABLE[401] = Items.FIREWORK_STAR;
        ID_TABLE[402] = Items.FIREWORK_ROCKET;
        ID_TABLE[403] = Items.ENCHANTED_BOOK;
        ID_TABLE[404] = Items.COMPARATOR;
        ID_TABLE[405] = Items.NETHER_BRICK;
        ID_TABLE[406] = Items.QUARTZ;
        ID_TABLE[407] = Items.TNT_MINECART;
        ID_TABLE[408] = Items.HOPPER_MINECART;
        ID_TABLE[409] = Items.PRISMARINE_SHARD;
        ID_TABLE[410] = Items.PRISMARINE_CRYSTALS;
        ID_TABLE[411] = Items.RABBIT;
        ID_TABLE[412] = Items.COOKED_RABBIT;
        ID_TABLE[413] = Items.RABBIT_STEW;
        ID_TABLE[414] = Items.RABBIT_FOOT;
        ID_TABLE[415] = Items.RABBIT_HIDE;
        ID_TABLE[416] = Items.ARMOR_STAND;
        ID_TABLE[417] = Items.IRON_HORSE_ARMOR;
        ID_TABLE[418] = Items.GOLDEN_HORSE_ARMOR;
        ID_TABLE[419] = Items.DIAMOND_HORSE_ARMOR;
        ID_TABLE[420] = Items.LEAD;
        ID_TABLE[421] = Items.NAME_TAG;
        ID_TABLE[425] = BANNER[0];
    }

    public static Item resolve(int id, int damage) {
        switch (id) {
            case 1:
                return switch (damage) {
                    case 1 -> Items.GRANITE;
                    case 2 -> Items.POLISHED_GRANITE;
                    case 3 -> Items.DIORITE;
                    case 4 -> Items.POLISHED_DIORITE;
                    case 5 -> Items.ANDESITE;
                    case 6 -> Items.POLISHED_ANDESITE;
                    default -> Items.STONE;
                };
            case 3:
                return switch (damage) {
                    case 1 -> Items.COARSE_DIRT;
                    case 2 -> Items.PODZOL;
                    default -> Items.DIRT;
                };
            case 5:
                return switch (damage) {
                    case 1 -> Items.SPRUCE_PLANKS;
                    case 2 -> Items.BIRCH_PLANKS;
                    case 3 -> Items.JUNGLE_PLANKS;
                    case 4 -> Items.ACACIA_PLANKS;
                    case 5 -> Items.DARK_OAK_PLANKS;
                    default -> Items.OAK_PLANKS;
                };
            case 6:
                return switch (damage) {
                    case 1 -> Items.SPRUCE_SAPLING;
                    case 2 -> Items.BIRCH_SAPLING;
                    case 3 -> Items.JUNGLE_SAPLING;
                    case 4 -> Items.ACACIA_SAPLING;
                    case 5 -> Items.DARK_OAK_SAPLING;
                    default -> Items.OAK_SAPLING;
                };
            case 12:
                return damage == 1 ? Items.RED_SAND : Items.SAND;
            case 17:
                return switch (damage & 3) {
                    case 1 -> Items.SPRUCE_LOG;
                    case 2 -> Items.BIRCH_LOG;
                    case 3 -> Items.JUNGLE_LOG;
                    default -> Items.OAK_LOG;
                };
            case 18:
                return switch (damage & 3) {
                    case 1 -> Items.SPRUCE_LEAVES;
                    case 2 -> Items.BIRCH_LEAVES;
                    case 3 -> Items.JUNGLE_LEAVES;
                    default -> Items.OAK_LEAVES;
                };
            case 19:
                return damage == 1 ? Items.WET_SPONGE : Items.SPONGE;
            case 24:
                return switch (damage) {
                    case 1 -> Items.CHISELED_SANDSTONE;
                    case 2 -> Items.SMOOTH_SANDSTONE;
                    default -> Items.SANDSTONE;
                };
            case 31:
                return damage == 2 ? Items.FERN : Items.SHORT_GRASS;
            case 35:
                return WOOL[damage & 15];
            case 38:
                return switch (damage) {
                    case 1 -> Items.BLUE_ORCHID;
                    case 2 -> Items.ALLIUM;
                    case 3 -> Items.AZURE_BLUET;
                    case 4 -> Items.RED_TULIP;
                    case 5 -> Items.ORANGE_TULIP;
                    case 6 -> Items.WHITE_TULIP;
                    case 7 -> Items.PINK_TULIP;
                    case 8 -> Items.OXEYE_DAISY;
                    default -> Items.POPPY;
                };
            case 44:
                return switch (damage) {
                    case 1 -> Items.SANDSTONE_SLAB;
                    case 2 -> Items.OAK_SLAB;
                    case 3 -> Items.COBBLESTONE_SLAB;
                    case 4 -> Items.BRICK_SLAB;
                    case 5 -> Items.STONE_BRICK_SLAB;
                    case 6 -> Items.NETHER_BRICK_SLAB;
                    case 7 -> Items.QUARTZ_SLAB;
                    default -> Items.STONE_SLAB;
                };
            case 95:
                return STAINED_GLASS[damage & 15];
            case 98:
                return switch (damage) {
                    case 1 -> Items.MOSSY_STONE_BRICKS;
                    case 2 -> Items.CRACKED_STONE_BRICKS;
                    case 3 -> Items.CHISELED_STONE_BRICKS;
                    default -> Items.STONE_BRICKS;
                };
            case 126:
                return switch (damage) {
                    case 1 -> Items.SPRUCE_SLAB;
                    case 2 -> Items.BIRCH_SLAB;
                    case 3 -> Items.JUNGLE_SLAB;
                    case 4 -> Items.ACACIA_SLAB;
                    case 5 -> Items.DARK_OAK_SLAB;
                    default -> Items.OAK_SLAB;
                };
            case 139:
                return damage == 1 ? Items.MOSSY_COBBLESTONE_WALL : Items.COBBLESTONE_WALL;
            case 155:
                return switch (damage) {
                    case 1 -> Items.CHISELED_QUARTZ_BLOCK;
                    case 2 -> Items.QUARTZ_PILLAR;
                    default -> Items.QUARTZ_BLOCK;
                };
            case 159:
                return TERRACOTTA[damage & 15];
            case 160:
                return STAINED_GLASS_PANE[damage & 15];
            case 161:
                return (damage & 3) == 1 ? Items.DARK_OAK_LEAVES : Items.ACACIA_LEAVES;
            case 162:
                return (damage & 3) == 1 ? Items.DARK_OAK_LOG : Items.ACACIA_LOG;
            case 168:
                return switch (damage) {
                    case 1 -> Items.PRISMARINE_BRICKS;
                    case 2 -> Items.DARK_PRISMARINE;
                    default -> Items.PRISMARINE;
                };
            case 171:
                return CARPET[damage & 15];
            case 175:
                return switch (damage) {
                    case 1 -> Items.LILAC;
                    case 2 -> Items.TALL_GRASS;
                    case 3 -> Items.LARGE_FERN;
                    case 4 -> Items.ROSE_BUSH;
                    case 5 -> Items.PEONY;
                    default -> Items.SUNFLOWER;
                };
            case 263:
                return damage == 1 ? Items.CHARCOAL : Items.COAL;
            case 322:
                return damage == 1 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
            case 349:
                return switch (damage) {
                    case 1 -> Items.SALMON;
                    case 2 -> Items.TROPICAL_FISH;
                    case 3 -> Items.PUFFERFISH;
                    default -> Items.COD;
                };
            case 350:
                return damage == 1 ? Items.COOKED_SALMON : Items.COOKED_COD;
            case 351:
                return getDye(damage);
            case 425:
                return BANNER[damage & 15];
            case 2256:
                return Items.MUSIC_DISC_13;
            case 2258:
                return Items.MUSIC_DISC_CAT;
            case 2262:
                return Items.MUSIC_DISC_BLOCKS;
            default:
                break;
        }

        if (id >= 0 && id < ID_TABLE_SIZE) {
            Item item = ID_TABLE[id];
            if (item != null) return item;
            if (id == 0) return Items.AIR;
        }
        Blackaddons.LOGGER.warn("Unknown Legacy ID: {}", id);
        return Items.BARRIER;
    }
}
