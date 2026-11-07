package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.common.BlockPseudoWater;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A bucket of cod, salmon, pufferfish or tropical fish. Emptying it places water as a water bucket does
 * and lets the fish out with what the bucket kept (its name, health, a tropical fish's look); the fish
 * never despawns. Dispensers empty it too. A bucket of tropical fish names its fish in the tooltip.
 */
public class ItemFishBucket extends Item {

    private final Function<World, ? extends EntityAbstractFish> fish;

    public ItemFishBucket(Function<World, ? extends EntityAbstractFish> fish) {
        this.fish = fish;
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        RayTraceResult hit = this.rayTrace(world, player, false);
        ActionResult<ItemStack> event = ForgeEventFactory.onBucketUse(player, world, stack, hit);
        if (event != null) {
            return event;
        }
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        }
        BlockPos pos = hit.getBlockPos();
        if (!world.isBlockModifiable(player, pos)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        boolean replaceable = world.getBlockState(pos).getBlock().isReplaceable(world, pos);
        BlockPos target = replaceable && hit.sideHit == EnumFacing.UP ? pos : pos.offset(hit.sideHit);
        if (!player.canPlayerEdit(target, hit.sideHit, stack) || !this.empty(player, world, target, stack)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, target, stack);
        }
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.capabilities.isCreativeMode ? stack : new ItemStack(Items.BUCKET));
    }

    /**
     * Places water at a position, as a water bucket would, and lets the fish out there.
     *
     * <p>Kelp, seagrass and wet coral already are water: the fish just goes in. A dry coral is filled.
     * In the Nether the water boils away and the fish is left to flop, as in 1.13.
     *
     * @return false if water cannot go there; then nothing happened
     */
    public boolean empty(@Nullable EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockPseudoWater) {
            BlockPseudoWater plant = (BlockPseudoWater) block;
            if (!plant.isWater(state)) {
                IBlockState filled = plant.filled(state);
                if (filled == null) {
                    return false;
                }
                if (!world.isRemote) {
                    world.setBlockState(pos, filled, 11);
                }
            }
        } else {
            Material material = state.getMaterial();
            boolean nonSolid = !material.isSolid();
            boolean replaceable = block.isReplaceable(world, pos);
            if (!world.isAirBlock(pos) && !nonSolid && !replaceable) {
                return false;
            }
            if (world.provider.doesWaterVaporize()) {
                world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                        2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
                for (int k = 0; k < 8; ++k) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + Math.random(), pos.getY() + Math.random(),
                            pos.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
                }
            } else {
                if (!world.isRemote && (nonSolid || replaceable) && !material.isLiquid()) {
                    world.destroyBlock(pos, true);
                }
                world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState(), 11);
            }
        }
        world.playSound(player, pos, FishContent.BUCKET_EMPTY_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        if (!world.isRemote) {
            this.release(world, pos, stack, player);
        }
        return true;
    }

    /** @return the fish let out, already in the world, or null */
    @Nullable
    public EntityAbstractFish release(World world, BlockPos pos, ItemStack stack, @Nullable EntityPlayer player) {
        EntityAbstractFish entity = this.fish.apply(world);
        entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
        entity.rotationYawHead = entity.rotationYaw;
        entity.renderYawOffset = entity.rotationYaw;
        entity.releasedFrom(stack);
        ItemMonsterPlacer.applyItemEntityDataToEntity(world, player, stack, entity);
        return world.spawnEntity(entity) ? entity : null;
    }

    /** 1.13's tooltip: a predefined fish's name, or its type and colors. */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        if (this != FishContent.tropicalFishBucket || tag == null || !tag.hasKey("BucketVariantTag", 3)) {
            return;
        }
        String style = TextFormatting.GRAY.toString() + TextFormatting.ITALIC;
        int variant = tag.getInteger("BucketVariantTag");
        int predefined = TropicalFishVariant.predefinedIndex(variant);
        if (predefined >= 0) {
            tooltip.add(style + I18n.format(TropicalFishVariant.predefinedKey(predefined)));
            return;
        }
        tooltip.add(style + I18n.format(TropicalFishVariant.typeKey(variant)));
        int body = TropicalFishVariant.bodyColor(variant);
        int pattern = TropicalFishVariant.patternColor(variant);
        String colors = I18n.format(TropicalFishVariant.colorKey(body));
        if (body != pattern) {
            colors = I18n.format("item." + com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.MOD_ID + ".tropical_fish_bucket.colors",
                    colors, I18n.format(TropicalFishVariant.colorKey(pattern)));
        }
        tooltip.add(style + colors);
    }

    /** Dispensers empty a fish bucket in front of them, or drop it if water cannot go there. */
    static final class Dispense extends BehaviorDefaultDispenseItem {
        private final BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();

        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            ItemFishBucket bucket = (ItemFishBucket) stack.getItem();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            if (bucket.empty(null, source.getWorld(), pos, stack)) {
                return new ItemStack(Items.BUCKET);
            }
            return this.drop.dispense(source, stack);
        }
    }
}
