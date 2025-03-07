package de.mennomax.astikorcarts.entity;

import java.util.function.Supplier;

import de.mennomax.astikorcarts.common.entities.AstikorEntities;
import de.mennomax.astikorcarts.config.AstikorCartsConfig;

import net.dries007.tfc.util.registry.RegistryWood;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AnimalCartEntity extends AbstractDrawnEntity {
    public final RegistryWood wood;
    public final Supplier<? extends Item> drop;
    public EntityType<? extends Entity> entityTypeIn;

    public PostilionEntity postilionEntity = null;

    public AnimalCartEntity(final EntityType<? extends Entity> entityTypeIn, final Level worldIn, Supplier<? extends Item> drop, RegistryWood wood) {
        super(entityTypeIn, worldIn);
        this.drop = drop;
        this.wood = wood;
    }

    public PostilionEntity getPostilionEntity(RegistryWood wood)
    {
        if (postilionEntity != null)
            return postilionEntity;

        if (wood != null)
            return AstikorEntities.POSTILION_TFC.get(wood).get().create(this.level);

        return null;
    }

    @Override
    public AstikorCartsConfig.CartConfig getConfig() {
        return AstikorCartsConfig.COMMON.animalCart;
    }

    @Override
    public void tick() {
        super.tick();
        final Entity coachman = this.getControllingPassenger();
        final Entity pulling = this.getPulling();
        if (pulling != null && coachman != null && pulling.getControllingPassenger() == null) {
            final PostilionEntity postilion = getPostilionEntity(wood);
            if (postilion != null) {
                postilion.moveTo(pulling.getX(), pulling.getY(), pulling.getZ(), coachman.getYRot(), coachman.getXRot());
                if (postilion.startRiding(pulling)) {
                    this.level.addFreshEntity(postilion);
                } else {
                    postilion.discard();
                }
            }
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!this.level.isClientSide) {
                for (final Entity entity : this.getPassengers()) {
                    if (!(entity instanceof Player)) {
                        entity.stopRiding();
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        final InteractionResult bannerResult = this.useBanner(player, hand);
        if (bannerResult.consumesAction()) {
            return bannerResult;
        }
        if (this.getPulling() != player) {
            if (!this.canAddPassenger(player)) {
                return InteractionResult.PASS;
            }
            if (!this.level.isClientSide) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void push(final Entity entityIn)
    {
        if (!entityIn.hasPassenger(this))
        {
            if (!this.level.isClientSide && this.getPulling() != entityIn && this.getControllingPassenger() == null && this.getPassengers().size() <= AstikorCartsConfig.COMMON.config.maxAnimalSize.get() && !entityIn.isPassenger() && (entityIn.getBbWidth() < this.getBbWidth() || this.getPassengers().size() <= AstikorCartsConfig.COMMON.config.maxAnimalSize.get()) && entityIn instanceof LivingEntity
                && canCarryWaterEntities(entityIn) && canPushIntoPlayers(entityIn))
            {
                entityIn.startRiding(this);
            }
            else
            {
                super.push(entityIn);
            }
        }
    }

    public static boolean canPushIntoPlayers(Entity entityIn)
    {
        if (entityIn instanceof Player)
            return AstikorCartsConfig.COMMON.config.canPushIntoPlayers.get();
        else
            return true;
    }

    public static boolean canCarryWaterEntities(Entity entityIn)
    {
        if (entityIn instanceof WaterAnimal)
            return AstikorCartsConfig.COMMON.config.canCarryWaterEntities.get();
        else
            return true;
    }

    @Override
    public boolean canAddPassenger(final Entity passenger) {
        return this.getPassengers().size() <= AstikorCartsConfig.COMMON.config.maxAnimalSize.get();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 11.0D / 16.0D;
    }

    @Override
    public void positionRider(final Entity passenger) {
        if (this.hasPassenger(passenger)) {
            double f = -0.1D;

            if (this.getPassengers().size() > 1) {
                f = this.getPassengers().indexOf(passenger) == 0 ? 0.2D : -0.6D;

                if (passenger instanceof Animal) {
                    f += 0.2D;
                }
            }

            final Vec3 forward = this.getLookAngle();
            final Vec3 origin = new Vec3(0.0D, this.getPassengersRidingOffset(), 1.0D / 16.0D);
            final Vec3 pos = origin.add(forward.scale(f + Mth.sin((float) Math.toRadians(this.getXRot())) * 0.7D));
            passenger.setPos(this.getX() + pos.x, this.getY() + pos.y + passenger.getMyRidingOffset(), this.getZ() + pos.z);
            passenger.setYBodyRot(this.getYRot());
            final float f2 = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
            final float f1 = Mth.clamp(f2, -105.0F, 105.0F);
            passenger.yRotO += f1 - f2;
            passenger.setYRot(passenger.getYRot() + (f1 - f2));
            passenger.setYHeadRot(passenger.getYRot());
            if (passenger instanceof Animal && this.getPassengers().size() > 1) {
                final int j = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(((Animal) passenger).yBodyRot + j);
                passenger.setYHeadRot(passenger.getYHeadRot() + j);
            }
        }
    }

    @Override
    public Item getCartItem() {
        return drop.get();
    }
}
