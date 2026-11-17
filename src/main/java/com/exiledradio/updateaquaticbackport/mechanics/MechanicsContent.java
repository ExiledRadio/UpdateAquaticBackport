package com.exiledradio.updateaquaticbackport.mechanics;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import javax.annotation.Nullable;

/**
 * 1.13's changes to how existing things behave ({@code mechanics} settings). Most of them are patches
 * ({@link MechanicsHooks}); this class has the ones that are events:
 *
 * <ul>
 *   <li>item frames on floors and ceilings ({@link EntityFloorItemFrame}, always registered so a world keeps
 *       them whatever the setting);</li>
 *   <li>signs, banners, heads and redstone dust make their place sound (1.13, 17w47a). 1.12.2's items for them are
 *       not {@code ItemBlock}s and play nothing; Forge's place event (server side) gives the block placed, and its
 *       sound type's place sound plays for everyone around, at {@code ItemBlock}'s volume and pitch.</li>
 * </ul>
 * Squid spawning is {@link SquidSpawning}; the block outline is the client's {@code ShapedOutlines}.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class MechanicsContent {

    /** This stream's entity ids (block 30-34). */
    public static final int ITEM_FRAME_ID = 30;

    private MechanicsContent() {
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        // Tracked as vanilla's frame is: 160 blocks, no position updates, no velocity.
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityFloorItemFrame.class)
                .id(Content.id("item_frame"), ITEM_FRAME_ID)
                .name(UpdateAquaticBackport.MOD_ID + ".item_frame")
                .tracker(160, Integer.MAX_VALUE, false)
                .build());
    }

    // ------------------------------------------------------------------------------- floor item frames

    /**
     * An item frame used on the top or bottom of a block. 1.12.2's item fails there (it takes only the four sides);
     * that click then goes on as a plain item use, after the clicked block had its chance to do something (a chest
     * opens, a lever flips, as with any item). This places a frame facing up or down as 1.13's
     * {@code ItemItemFrame} does - where the player may edit, on a solid block, where no other hanging thing is -
     * plays its place sound and uses up one frame. The block and face are the player's own reach and look, as the
     * client's were.
     */
    @SubscribeEvent
    public static void placeFrame(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        if (!ModConfig.mechanics.floorItemFrames || stack.getItem() != Items.ITEM_FRAME || player.isSpectator()) {
            return;
        }
        World world = event.getWorld();
        RayTraceResult hit = lookedAt(player);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.sideHit.getAxis() != EnumFacing.Axis.Y) {
            return;
        }
        EnumFacing face = hit.sideHit;
        BlockPos at = hit.getBlockPos().offset(face);
        if (world.isOutsideBuildHeight(at) || !player.canPlayerEdit(at, face, stack)) {
            return;
        }
        EntityFloorItemFrame frame = new EntityFloorItemFrame(world, at, face);
        if (!frame.onValidSurface()) {
            return;
        }
        if (!world.isRemote) {
            frame.playPlaceSound();
            world.spawnEntity(frame);
        } else {
            player.swingArm(event.getHand());
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** The block the player looks at within reach, as the client's {@code objectMouseOver} finds it. */
    @Nullable
    static RayTraceResult lookedAt(EntityPlayer player) {
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        return player.world.rayTraceBlocks(eyes, eyes.add(look.x * reach, look.y * reach, look.z * reach), false, false, true);
    }

    // ---------------------------------------------------------------------------------- place sounds

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placeSound(BlockEvent.PlaceEvent event) {
        if (!ModConfig.mechanics.placementSounds || event.isCanceled() || event.getWorld().isRemote) {
            return;
        }
        IBlockState state = event.getPlacedBlock();
        Block block = state.getBlock();
        if (!(block instanceof BlockSign || block instanceof BlockBanner || block instanceof BlockSkull || block instanceof BlockRedstoneWire)) {
            return;
        }
        // An ItemBlock (another mod's) plays its own sound already; 1.12.2's banner item is an ItemBlock that
        // places on its own and plays nothing.
        Item item = event.getPlayer().getHeldItem(event.getHand()).getItem();
        if (item instanceof ItemBlock && !(item instanceof ItemBanner)) {
            return;
        }
        playPlaceSound(event.getWorld(), event.getPos(), state, event.getPlayer());
    }

    /** {@code ItemBlock}'s place sound, heard by everyone around. */
    public static void playPlaceSound(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        SoundType sound = state.getBlock().getSoundType(state, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }
}
