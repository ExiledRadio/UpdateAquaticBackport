package com.exiledradio.updateaquaticbackport.trident;

import com.google.common.collect.Multimap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The trident: 9 attack damage at 1.1 attack speed, 250 durability, thrown by holding and releasing use.
 *
 * <p>Ported from 1.14.4's {@code TridentItem}. Held for at least 10 ticks and released it is thrown (an
 * {@link EntityTrident}, which deals 8), or, with Riptide, launches its holder while they are in water or
 * rain. As current Java, a trident one use from breaking cannot be used at all.
 *
 * <p>The use animation is {@link EnumAction#NONE} to vanilla code, so nothing that switches on the
 * action ever meets an unknown value; the raised-spear first-person view is drawn by the client hook.
 */
public final class ItemTrident extends Item {

    /** 1.14.4: a throw needs the use key held this long. */
    public static final int MIN_USE_TICKS = 10;

    public ItemTrident() {
        this.setMaxDamage(250);
        this.setMaxStackSize(1);
        this.addPropertyOverride(new ResourceLocation("throwing"), new IItemPropertyGetter() {
            @Override
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                return entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack ? 1.0F : 0.0F;
            }
        });
    }

    /** @return true when one more use would break it (current Java refuses to use it then) */
    public static boolean tooDamagedToUse(ItemStack stack) {
        return stack.getItemDamage() >= stack.getMaxDamage() - 1;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (tooDamagedToUse(stack)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        if (TridentContent.riptideLevel(stack) > 0 && !player.isWet()) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        player.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (this.getMaxItemUseDuration(stack) - timeLeft < MIN_USE_TICKS || tooDamagedToUse(stack)) {
            return;
        }
        int riptide = TridentContent.riptideLevel(stack);
        if (riptide > 0 && !player.isWet()) {
            return;
        }
        if (!world.isRemote) {
            stack.damageItem(1, player);
            if (riptide == 0) {
                EntityTrident trident = new EntityTrident(world, player, stack);
                trident.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, 2.5F, 1.0F);
                if (player.capabilities.isCreativeMode) {
                    trident.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
                }
                world.spawnEntity(trident);
                world.playSound(null, trident.posX, trident.posY, trident.posZ, TridentSounds.TRIDENT_THROW,
                        SoundCategory.PLAYERS, 1.0F, 1.0F);
                if (!player.capabilities.isCreativeMode) {
                    player.inventory.deleteStack(stack);
                }
            }
        }
        StatBase used = StatList.getObjectUseStats(this);
        if (used != null) {
            player.addStat(used);
        }
        if (riptide > 0) {
            launch(player, riptide);
        }
    }

    /** Riptide: 1.14.4's push along the look direction, a 20-tick spin attack and its sound. */
    static void launch(EntityPlayer player, int riptide) {
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        float x = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float y = -MathHelper.sin(pitch * 0.017453292F);
        float z = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float length = MathHelper.sqrt(x * x + y * y + z * z);
        float speed = 3.0F * ((1.0F + riptide) / 4.0F);
        player.addVelocity(x * (speed / length), y * (speed / length), z * (speed / length));
        Riptide.start(player, Riptide.SPIN_TICKS);
        if (player.onGround) {
            player.move(MoverType.SELF, 0.0D, 1.1999999F, 0.0D);
        }
        SoundEvent sound = riptide >= 3 ? TridentSounds.TRIDENT_RIPTIDE_3
                : riptide == 2 ? TridentSounds.TRIDENT_RIPTIDE_2 : TridentSounds.TRIDENT_RIPTIDE_1;
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if (state.getBlockHardness(world, pos) != 0.0F) {
            stack.damageItem(2, entity);
        }
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers(slot);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", 8.0D, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", (double) -2.9F, 0));
        }
        return map;
    }
}
