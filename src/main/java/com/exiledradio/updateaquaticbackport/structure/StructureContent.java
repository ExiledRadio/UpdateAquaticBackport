package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.storage.MapDecoration;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.13's ocean structures: shipwrecks, ocean ruins and buried treasure, their loot, and the buried
 * treasure map with its red X.
 *
 * <ul>
 *   <li>Placement is today's (structure sets {@code shipwrecks}, {@code ocean_ruins},
 *       {@code buried_treasures}), driven by Backport Core's {@link ScatteredStructure} from the populate
 *       step and laid out on its terrain heights, so it works on any terrain Core can predict.</li>
 *   <li>Biomes are today's: sunken shipwrecks in every ocean, beached ones and buried treasure on beaches,
 *       warm ruins in warm and lukewarm oceans, cold ruins in the rest ({@link StructureBiomes}).</li>
 *   <li>Structure files are Mojang's own, in {@code assets/updateaquaticbackport/structures/}; loot tables
 *       are today's, under {@code updateaquaticbackport:chests/}, set by the files' data markers.</li>
 *   <li>Dolphins and treasure maps find them through Core's {@link Structures}.</li>
 * </ul>
 * Off ({@code structures.enabled}), nothing generates and no table or map icon is registered.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class StructureContent {

    public static final ResourceLocation SHIPWRECK_MAP = Content.id("chests/shipwreck_map");
    public static final ResourceLocation SHIPWRECK_SUPPLY = Content.id("chests/shipwreck_supply");
    public static final ResourceLocation SHIPWRECK_TREASURE = Content.id("chests/shipwreck_treasure");
    public static final ResourceLocation UNDERWATER_RUIN_BIG = Content.id("chests/underwater_ruin_big");
    public static final ResourceLocation UNDERWATER_RUIN_SMALL = Content.id("chests/underwater_ruin_small");
    public static final ResourceLocation BURIED_TREASURE = Content.id("chests/buried_treasure");

    /** Save names: permanent once worlds have them. */
    static final String SHIPWRECK_SAVE = "UAB_Shipwreck";
    static final String OCEAN_RUIN_SAVE = "UAB_OceanRuin";
    static final String BURIED_TREASURE_SAVE = "UAB_BuriedTreasure";

    /** The 1.12.2 renderer's slot for the eleventh decoration type, where our map_icons.png has the red X. */
    private static final int RED_X_SLOT = 10;

    private static MapDecoration.Type redX;

    private StructureContent() {
    }

    public static boolean enabled() {
        return ModConfig.structures.enabled;
    }

    /** Pre-init: the template palette source, the red X decoration and the map loot function. */
    public static void preInit() {
        // Structure files naming minecraft:kelp, minecraft:seagrass and the like get ours.
        ModernPalette.source(UpdateAquaticBackport.MOD_ID);
        if (!enabled()) {
            return;
        }
        redX = addRedX();
        LootFunctionManager.registerFunction(new LootExplorationMap.Serializer());
    }

    /** Init: loot tables, save names and the three structures. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        for (ResourceLocation table : new ResourceLocation[]{SHIPWRECK_MAP, SHIPWRECK_SUPPLY, SHIPWRECK_TREASURE,
                UNDERWATER_RUIN_BIG, UNDERWATER_RUIN_SMALL, BURIED_TREASURE}) {
            LootTableList.register(table);
        }
        MapGenStructureIO.registerStructure(MapGenShipwreck.Start.class, SHIPWRECK_SAVE);
        MapGenStructureIO.registerStructureComponent(ShipwreckPiece.class, "UAB_ShipwreckPiece");
        MapGenStructureIO.registerStructure(MapGenOceanRuin.Start.class, OCEAN_RUIN_SAVE);
        MapGenStructureIO.registerStructureComponent(OceanRuinPiece.class, "UAB_OceanRuinPiece");
        MapGenStructureIO.registerStructure(MapGenBuriedTreasure.Start.class, BURIED_TREASURE_SAVE);
        MapGenStructureIO.registerStructureComponent(BuriedTreasurePiece.class, "UAB_BuriedTreasurePiece");

        ScatteredStructure.register(Structures.SHIPWRECK, world -> generates(world) ? new MapGenShipwreck() : null);
        ScatteredStructure.register(Structures.OCEAN_RUIN, world -> generates(world) ? new MapGenOceanRuin() : null);
        ScatteredStructure.register(Structures.BURIED_TREASURE, world -> generates(world) ? new MapGenBuriedTreasure() : null);
    }

    /** Today's ocean structures are the overworld's, and only where the world generates structures. */
    static boolean generates(World world) {
        return world.provider.getDimension() == 0 && world.getWorldInfo().isMapFeaturesEnabled();
    }

    /** @return the red X map decoration (1.13's buried treasure marker) */
    public static MapDecoration.Type redX() {
        return redX != null ? redX : MapDecoration.Type.TARGET_X;
    }

    /**
     * 1.12.2 has no red X decoration: one more {@link MapDecoration.Type}, whose icon number is its ordinal.
     * Added on both sides in pre-init, so a server and its clients number it the same.
     */
    private static MapDecoration.Type addRedX() {
        for (MapDecoration.Type type : MapDecoration.Type.values()) {
            if ("RED_X".equals(type.name())) {
                return type;
            }
        }
        MapDecoration.Type type = EnumHelper.addEnum(MapDecoration.Type.class, "RED_X",
                new Class<?>[]{boolean.class, int.class}, true, -1);
        if (type == null) {
            UpdateAquaticBackport.LOGGER.warn("Could not add the red X map decoration; buried treasure maps use the white X.");
        } else if (type.getIcon() != RED_X_SLOT) {
            UpdateAquaticBackport.LOGGER.warn("Another mod added map decorations first: the red X is number {}, not {}, "
                    + "so buried treasure maps may show the wrong icon.", type.getIcon(), RED_X_SLOT);
        }
        return type;
    }

    /**
     * The heart of the sea, today's first pool of buried treasure, belongs to the conduit stream; a loot
     * file naming it would fail to load whenever that stream is off, so it joins here.
     */
    @SubscribeEvent
    public static void loot(LootTableLoadEvent event) {
        if (!enabled() || !BURIED_TREASURE.equals(event.getName())) {
            return;
        }
        Item heart = Owners.ours("heart_of_the_sea");
        if (heart == null) {
            return;
        }
        LootEntry entry = new LootEntryItem(heart, 1, 0, new LootFunction[0], new LootCondition[0],
                Content.id("heart_of_the_sea").toString());
        event.getTable().addPool(new LootPool(new LootEntry[]{entry}, new LootCondition[0], new RandomValueRange(1),
                new RandomValueRange(0), "heart_of_the_sea"));
    }
}
