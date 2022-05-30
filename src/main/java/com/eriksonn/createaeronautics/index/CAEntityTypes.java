package com.eriksonn.createaeronautics.index;

import com.eriksonn.createaeronautics.CreateAeronautics;
import com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing.GyroscopicControlledContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipContraption;
import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntityRenderer;
import com.eriksonn.createaeronautics.contraptions.AirshipContraptionRenderer;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionEntityRenderer;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionType;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.repack.registrate.util.entry.EntityEntry;
import com.simibubi.create.repack.registrate.util.nullness.NonNullConsumer;
import com.simibubi.create.repack.registrate.util.nullness.NonNullSupplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class CAEntityTypes {
    public static final EntityEntry<AirshipContraptionEntity> AIRSHIP_CONTRAPTION = contraption("airship_contraption", AirshipContraptionEntity::new, () -> ContraptionEntityRenderer::new, 5, 1, false);
    public static ContraptionType AIRSHIP = ContraptionType.register("airship", AirshipContraption::new);
    public static final EntityEntry<GyroscopicControlledContraptionEntity> GYROSCOPIC_CONTROLLED_CONTRAPTION = contraption("gyroscopic_bearing_contraption", GyroscopicControlledContraptionEntity::new, () -> {
        return ContraptionEntityRenderer::new;
    }, 20, 40, false);

    public CAEntityTypes() {
    }

    private static <T extends Entity> EntityEntry<T> contraption(String name, EntityType.IFactory<T> factory, NonNullSupplier<IRenderFactory<? super T>> renderer, int range, int updateFrequency, boolean sendVelocity) {
        return register(name, factory, renderer, EntityClassification.MISC, range, updateFrequency, sendVelocity, true, AbstractContraptionEntity::build).register();
    }

    private static <T extends Entity> CreateEntityBuilder<T, ?> register(String name, EntityType.IFactory<T> factory, NonNullSupplier<IRenderFactory<? super T>> renderer, EntityClassification group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire, NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
        String id = Lang.asId(name);
        return (CreateEntityBuilder) CreateAeronautics.registrate().entity(id, factory, group).properties((b) -> {
            b.setTrackingRange(range).setUpdateInterval(updateFrequency).setShouldReceiveVelocityUpdates(sendVelocity);
        }).properties(propertyBuilder).properties((b) -> {
            if (immuneToFire) {
                b.fireImmune();
            }

        }).renderer(renderer);
    }

    public static void register() {
    }
}
