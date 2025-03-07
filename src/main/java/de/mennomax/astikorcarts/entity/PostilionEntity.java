package de.mennomax.astikorcarts.entity;

import de.mennomax.astikorcarts.world.AstikorWorld;

import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public class PostilionEntity extends DummyLivingEntity {
    public final Supplier<? extends Item> drop;

    public PostilionEntity(final EntityType<? extends PostilionEntity> type, final Level world, Supplier<? extends Item> drop) {
        super(type, world);
        this.drop = drop;
    }

    public Item getDropItem()
    {
        return drop.get();
    }

    @Override
    public double getMyRidingOffset() {
        return 0.125D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            final LivingEntity coachman = this.getCoachman();
            if (coachman != null) {
                this.setYRot(coachman.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(coachman.getXRot() * 0.5F);
                this.zza = coachman.zza;
                this.xxa = 0.0F;
            } else {
                this.discard();
            }
        }
    }

    @Nullable
    public LivingEntity getCoachman() {
        final Entity mount = this.getVehicle();
        if (mount != null) {
            return AstikorWorld.get(this.level).map(m -> m.getDrawn(mount)).orElse(Optional.empty())
                .map(AbstractDrawnEntity::getControllingPassenger).orElse(null);
        }
        return null;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
