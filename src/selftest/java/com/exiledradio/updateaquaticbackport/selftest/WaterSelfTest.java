package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.water.FloatingItems;
import com.exiledradio.updateaquaticbackport.water.GradualAir;
import com.exiledradio.updateaquaticbackport.water.ShallowWater;
import com.exiledradio.updateaquaticbackport.water.WaterColors;
import com.exiledradio.updateaquaticbackport.water.WaterContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Water colors, light through water, gradual air, floating items and orbs, and the shallow-water jump. */
public final class WaterSelfTest implements SelfTest {

    @Override
    public String name() {
        return "water";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(WaterContent.enabled(), "water stream on");
        c.ok(WaterContent.config().biomeColors && WaterContent.config().underwaterFog && WaterContent.config().ambience
                && WaterContent.config().lightThroughWater && WaterContent.config().gradualAir
                && WaterContent.config().floatingItems && WaterContent.config().shallowJump, "every part on by default");

        sounds(c);
        colors(c);
        light(c, world);
        air(c, world);
        floating(c, world);
        shallow(c, world);
    }

    // 1. sounds -------------------------------------------------------------------------------------
    private static void sounds(Check c) {
        String[] names = {"ambient.underwater.enter", "ambient.underwater.exit", "ambient.underwater.loop",
                "ambient.underwater.loop.additions", "ambient.underwater.loop.additions.rare",
                "ambient.underwater.loop.additions.ultra_rare"};
        SoundEvent[] ours = {WaterContent.UNDERWATER_ENTER, WaterContent.UNDERWATER_EXIT, WaterContent.UNDERWATER_LOOP,
                WaterContent.UNDERWATER_ADDITIONS, WaterContent.UNDERWATER_ADDITIONS_RARE, WaterContent.UNDERWATER_ADDITIONS_ULTRA_RARE};
        for (int i = 0; i < names.length; i++) {
            c.eq(ours[i], ForgeRegistries.SOUND_EVENTS.getValue(Content.id(names[i])), "sound " + names[i]);
        }
    }

    // 2. colors -------------------------------------------------------------------------------------
    private static void colors(Check c) {
        c.ok(WaterColors.active(), "biome colors active on the server");
        int def = 0x3F76E4, fog = 0x050533;
        Object[][] want = {
                {Biomes.PLAINS, def, fog}, {Biomes.OCEAN, def, fog}, {Biomes.DEEP_OCEAN, def, fog}, {Biomes.RIVER, def, fog},
                {Biomes.DESERT, def, fog}, {Biomes.HELL, def, fog}, {Biomes.SKY, def, fog}, {Biomes.ICE_PLAINS, def, fog},
                {Biomes.SWAMPLAND, 0x617B64, 0x232317}, {Biomes.MUTATED_SWAMPLAND, 0x617B64, 0x232317},
                {Biomes.FROZEN_OCEAN, 0x3938C9, fog}, {Biomes.FROZEN_RIVER, 0x3938C9, fog},
                {Biomes.COLD_TAIGA, 0x3D57D6, fog}, {Biomes.COLD_TAIGA_HILLS, 0x3D57D6, fog},
                {Biomes.MUTATED_TAIGA_COLD, 0x3D57D6, fog}, {Biomes.COLD_BEACH, 0x3D57D6, fog},
        };
        for (Object[] w : want) {
            Biome b = (Biome) w[0];
            // Through the event, as every water tint asks.
            c.eq(w[1], b.getWaterColorMultiplier(), "water color of " + b.getRegistryName());
            c.eq(w[2], WaterColors.fog(b), "fog color of " + b.getRegistryName());
        }
        int vanilla = 0;
        for (Biome b : ForgeRegistries.BIOMES) {
            if ("minecraft".equals(b.getRegistryName().getNamespace())) {
                vanilla++;
                c.ok(b.getWaterColorMultiplier() != 0xFFFFFF, "no vanilla biome keeps 1.12.2's white tint: " + b.getRegistryName());
            }
        }
        c.ok(vanilla >= 60, "all of vanilla's biomes checked: " + vanilla);

        // Our ocean biomes, by name, whether or not the oceans stream is in this build.
        Object[][] oceans = {
                {"warm_ocean", 0x43D5EE, 0x041F33}, {"lukewarm_ocean", 0x45ADF2, 0x041633}, {"deep_lukewarm_ocean", 0x45ADF2, 0x041633},
                {"cold_ocean", 0x3D57D6, fog}, {"deep_cold_ocean", 0x3D57D6, fog},
                {"frozen_ocean", 0x3938C9, fog}, {"deep_frozen_ocean", 0x3938C9, fog},
        };
        for (Object[] o : oceans) {
            Biome real = ForgeRegistries.BIOMES.getValue(Content.id((String) o[0]));
            Biome b = real != null && Content.id((String) o[0]).equals(real.getRegistryName()) ? real : fake(Content.id((String) o[0]), 0xFFFFFF);
            c.eq(o[1], b.getWaterColorMultiplier(), "water color of our " + o[0] + (b == real ? "" : " (stand-in)"));
            c.eq(o[2], WaterColors.fog(b), "fog color of our " + o[0]);
        }
        // Another mod's biomes: white gets the default, a tint stays a filter over it.
        c.eq(def, fake(new ResourceLocation("somemod", "plain"), 0xFFFFFF).getWaterColorMultiplier(), "a mod biome with no tint");
        int tint = 0x62AF84;
        int expect = (0x62 * 0x3F / 255) << 16 | (0xAF * 0x76 / 255) << 8 | (0x84 * 0xE4 / 255);
        c.eq(expect, fake(new ResourceLocation("somemod", "bayou"), tint).getWaterColorMultiplier(), "a mod biome's tint over 1.13's blue");
        c.eq(fog, WaterColors.fog(fake(new ResourceLocation("somemod", "bayou"), tint)), "a mod biome's fog");
    }

    private static Biome fake(ResourceLocation id, int waterColor) {
        Biome b = new Biome(new Biome.BiomeProperties("selftest " + id).setWaterColor(waterColor)) {
        };
        b.setRegistryName(id);
        return b;
    }

    // 3. light --------------------------------------------------------------------------------------
    private static void light(Check c, WorldServer world) {
        c.eq(1, Blocks.WATER.getDefaultState().getLightOpacity(), "still water opacity 1");
        c.eq(1, Blocks.FLOWING_WATER.getDefaultState().getLightOpacity(), "flowing water opacity 1");
        c.eq(3, Blocks.ICE.getDefaultState().getLightOpacity(), "ice unchanged");
        int depth = 12;
        BlockPos floor = Pools.pool(c, depth);
        BlockPos column = floor.add(6, 0, 6);
        StringBuilder seen = new StringBuilder();
        boolean ok = true;
        for (int k = 1; k <= depth; k++) {
            BlockPos p = column.up(depth + 1 - k);
            int sky = world.getLightFor(EnumSkyBlock.SKY, p);
            seen.append(sky).append(' ');
            ok &= sky == 15 - k;
        }
        c.ok(ok, "sky light drops by 1 per block of water, 14 at the top (seen " + seen.toString().trim() + ")");
        c.ok(world.getLightFor(EnumSkyBlock.SKY, column.up()) == 3, "12 blocks down: light 3, where 1.12.2 had 0");
    }

    // 4. gradual air --------------------------------------------------------------------------------
    private static void air(Check c, WorldServer world) {
        c.eq(104, GradualAir.corrected(100, 300), "a refill to 300 becomes +4");
        c.eq(300, GradualAir.corrected(297, 300), "the last few fill up");
        c.eq(99, GradualAir.corrected(100, 99), "losing air under water is untouched");
        c.eq(300, GradualAir.corrected(300, 300), "full stays full");

        BlockPos dry = c.area();
        BlockPos floor = Pools.pool(c, 6);

        // A real player: the same tick, pre to post.
        EntityPlayerMP player = Players.real(world, "water");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        // Entering water fires an advancement whose recipe packet needs a real connection.
        player.getAdvancements().dispose();
        player.setPosition(dry.getX() + 4.5, dry.getY(), dry.getZ() + 4.5);
        world.spawnEntity(player);
        try {
            player.setAir(100);
            player.onUpdateEntity();
            c.eq(104, player.getAir(), "a player out of water: 4 air back in one tick");
            for (int i = 0; i < 5; i++) {
                player.onUpdateEntity();
            }
            c.eq(124, player.getAir(), "and 4 more each tick");
            player.setPosition(floor.getX() + 3.5, floor.getY() + 1, floor.getZ() + 3.5);
            player.onUpdateEntity();
            c.ok(player.isInsideOfMaterial(Material.WATER), "the player is under water");
            c.ok(player.getAir() <= 124 && player.getAir() >= 123, "under water again: carries on from 124 (" + player.getAir() + ")");
            player.setPosition(dry.getX() + 4.5, dry.getY(), dry.getZ() + 4.5);
            player.setAir(290);
            for (int i = 0; i < 5; i++) {
                player.onUpdateEntity();
            }
            c.eq(300, player.getAir(), "fills to exactly 300");
        } finally {
            world.removeEntity(player);
        }

        // A mob: corrected at the start of each tick.
        EntityPig pig = new EntityPig(world);
        pig.setNoAI(true);
        pig.setPosition(dry.getX() + 10.5, dry.getY(), dry.getZ() + 10.5);
        world.spawnEntity(pig);
        pig.setAir(100);
        for (int i = 0; i < 10; i++) {
            pig.onUpdate();
        }
        pig.setPosition(floor.getX() + 6.5, floor.getY() + 1, floor.getZ() + 6.5);
        pig.onUpdate();
        c.ok(pig.isInsideOfMaterial(Material.WATER), "the pig is under water");
        c.eq(139, pig.getAir(), "a pig: 10 ticks of +4 above water, then -1 under");
        pig.setDead();

        // Fish and squid keep their instant refill.
        EntitySquid squid = new EntitySquid(world);
        squid.setNoAI(true);
        squid.setPosition(floor.getX() + 9.5, floor.getY() + 2, floor.getZ() + 9.5);
        world.spawnEntity(squid);
        squid.onUpdate();
        squid.setAir(100);
        squid.onUpdate();
        squid.onUpdate();
        c.eq(300, squid.getAir(), "a squid back in water is full at once");
        squid.setDead();

        Block column = Owners.ourBlock("bubble_column");
        if (column != null) {
            EntityPig inColumn = new EntityPig(world);
            inColumn.setNoAI(true);
            BlockPos at = floor.add(2, 1, 2);
            inColumn.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            world.spawnEntity(inColumn);
            BlockPos eyes = new BlockPos(inColumn.posX, inColumn.posY + inColumn.getEyeHeight(), inColumn.posZ);
            if (world.setBlockState(eyes, column.getDefaultState(), 2 | 16) && world.getBlockState(eyes).getBlock() == column) {
                inColumn.setAir(100);
                inColumn.onUpdate();
                int first = inColumn.getAir();
                inColumn.onUpdate();
                c.ok(inColumn.getAir() >= first, "in a bubble column the column's own refill is left alone");
            }
            inColumn.setDead();
        } else {
            UpdateAquaticBackport.LOGGER.info("SELFTEST water: no bubble column in this build; its exemption is not exercised");
        }
    }

    // 5. floating -----------------------------------------------------------------------------------
    private static void floating(Check c, WorldServer world) {
        BlockPos floor = Pools.pool(c, 10);
        double start = floor.getY() + 2.0;

        EntityItem item = new EntityItem(world, floor.getX() + 2.5, start, floor.getZ() + 2.5, new ItemStack(Items.STICK));
        warmUp(item, start);
        c.ok(item.isInWater() && item.isInsideOfMaterial(Material.WATER), "the item is in the water");
        c.ok(FloatingItems.apply(item), "float applies under water");
        item.onUpdate();
        c.ok(Math.abs(item.posY - start - 5.0E-4) < 1.0E-6, "one tick: rises 0.0005, 1.14.4's float (" + (item.posY - start) + ")");
        c.ok(Math.abs(item.motionY - 5.0E-4 * 0.98) < 1.0E-6, "and keeps 0.98 of it (" + item.motionY + ")");
        for (int i = 0; i < 800; i++) {
            FloatingItems.apply(item);
            item.onUpdate();
        }
        c.ok(item.posY > floor.getY() + 9.5, "after 40 s an item has floated up near the surface (" + (item.posY - floor.getY()) + ")");
        c.ok(item.posY < floor.getY() + 11.5, "and stays at it (" + (item.posY - floor.getY()) + ")");

        EntityItem sinking = new EntityItem(world, floor.getX() + 6.5, start + 4, floor.getZ() + 6.5, new ItemStack(Items.BONE));
        warmUp(sinking, start + 4);
        for (int i = 0; i < 400; i++) {
            sinking.onUpdate();
        }
        c.ok(sinking.posY < floor.getY() + 1.5, "without the float, 1.12.2's item sinks to the floor (" + (sinking.posY - floor.getY()) + ")");

        EntityXPOrb orb = new EntityXPOrb(world, floor.getX() + 10.5, start, floor.getZ() + 10.5, 5);
        warmUp(orb, start);
        c.ok(FloatingItems.apply(orb), "float applies to an orb under water");
        orb.onUpdate();
        c.ok(Math.abs(orb.posY - start - 5.0E-4) < 1.0E-6, "an orb rises 0.0005 in a tick (" + (orb.posY - start) + ")");
        orb.motionY = 0.5;
        FloatingItems.apply(orb);
        double before = orb.posY;
        orb.onUpdate();
        c.ok(Math.abs(orb.posY - before - 0.06) < 1.0E-6, "an orb never rises faster than 0.06 a tick (" + (orb.posY - before) + ")");

        EntityItem dry = new EntityItem(world, floor.getX() + 6.5, floor.getY() + 14, floor.getZ() + 6.5, new ItemStack(Items.STICK));
        warmUp(dry, floor.getY() + 14);
        c.ok(!FloatingItems.apply(dry), "no float out of water");

        EntityItem weightless = new EntityItem(world, floor.getX() + 10.5, start + 5, floor.getZ() + 2.5, new ItemStack(Items.STICK));
        weightless.setNoGravity(true);
        warmUp(weightless, start + 5);
        FloatingItems.apply(weightless);
        c.ok(Math.abs(weightless.motionY - 5.0E-4) < 1.0E-9, "no gravity: only the lift, nothing given back");

        // From the world tick: a spawned item in the water.
        EntityItem spawned = new EntityItem(world, floor.getX() + 4.5, start + 3, floor.getZ() + 8.5, new ItemStack(Items.FEATHER));
        world.spawnEntity(spawned);
        warmUp(spawned, start + 3);
        FloatingItems.applyAll(world);
        c.ok(Math.abs(spawned.motionY - (5.0E-4 + 0.03999999910593033D)) < 1.0E-9, "the world-tick pass reaches loaded items");
        spawned.setDead();
    }

    /** One update so the entity knows it is in water, then still at a height. */
    private static void warmUp(Entity e, double y) {
        e.setPosition(e.posX, y, e.posZ);
        e.motionX = e.motionY = e.motionZ = 0.0D;
        e.onUpdate();
        e.setPosition(e.posX, y, e.posZ);
        e.motionX = e.motionY = e.motionZ = 0.0D;
    }

    // 6. shallow water ------------------------------------------------------------------------------
    private static void shallow(Check c, WorldServer world) {
        BlockPos at = c.area().add(5, 0, 5);
        FakePlayer player = c.player("water", at);
        for (int level = 0; level < 8; level++) {
            world.setBlockState(at, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, level), 2 | 16);
            player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            player.onGround = true;
            player.handleWaterMovement();
            double depth = ShallowWater.submergedHeight(player);
            c.ok(Math.abs(depth - (8 - level) / 9.0) < 0.002, "level " + level + ": submerged " + (8 - level) + "/9 (" + depth + ")");
            c.ok(player.isInWater(), "level " + level + ": 1.12.2 calls it in water");
            c.eq(level >= 5, ShallowWater.canJumpNormally(player), "level " + level + ": jumps normally");
        }
        world.setBlockState(at.up(), Blocks.WATER.getDefaultState(), 2 | 16);
        world.setBlockState(at, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 6), 2 | 16);
        player.handleWaterMovement();
        c.ok(ShallowWater.submergedHeight(player) > 1.5, "water above: the lower block counts as full");
        c.ok(!ShallowWater.canJumpNormally(player), "deep water: swims up");
        world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), 2 | 16);
        world.setBlockState(at, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 6), 2 | 16);
        player.handleWaterMovement();
        player.onGround = false;
        c.ok(!ShallowWater.canJumpNormally(player), "off the ground: no jump");
        player.onGround = true;
        player.capabilities.isFlying = true;
        c.ok(!ShallowWater.canJumpNormally(player), "flying: no jump");
        player.capabilities.isFlying = false;
        world.setBlockState(at, Blocks.AIR.getDefaultState(), 2 | 16);
        player.handleWaterMovement();
        c.eq(0.0D, ShallowWater.submergedHeight(player), "dry: not submerged");
        c.ok(!ShallowWater.canJumpNormally(player), "dry: vanilla's own jump");
    }
}
