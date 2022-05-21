package com.eriksonn.createaeronautics.index;

import com.eriksonn.createaeronautics.CreateAeronautics;
import com.eriksonn.createaeronautics.particle.LevititeSparkleParticle;
import com.eriksonn.createaeronautics.particle.LevititeSparkleParticleData;
import com.eriksonn.createaeronautics.particle.PropellerAirParticleData;
import com.simibubi.create.content.contraptions.particle.ICustomParticleData;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Supplier;

public enum CAParticleTypes {

    PROPELLER_AIR_FLOW(PropellerAirParticleData::new),
    LEVITITE_SPARKLE(LevititeSparkleParticleData::new);
    private CAParticleTypes.ParticleEntry<?> entry;

    <D extends IParticleData> CAParticleTypes(Supplier<? extends ICustomParticleData<D>> typeFactory) {
        String asId = Lang.asId(this.name());
        entry = new CAParticleTypes.ParticleEntry<>(new ResourceLocation(CreateAeronautics.MODID, asId), typeFactory);
    }

    public static void register(RegistryEvent.Register<ParticleType<?>> event) {
        for (CAParticleTypes particle : values())
            particle.entry.register(event.getRegistry());
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerFactories(ParticleFactoryRegisterEvent event) {
        ParticleManager particles = Minecraft.getInstance().particleEngine;
        for (CAParticleTypes particle : values())
            particle.entry.registerFactory(particles);
    }

    public ParticleType<?> get() {
        return entry.getOrCreateType();
    }

    public String parameter() {
        return Lang.asId(name());
    }

    private class ParticleEntry<D extends IParticleData> {
        Supplier<? extends ICustomParticleData<D>> typeFactory;
        ParticleType<D> type;
        ResourceLocation id;

        public ParticleEntry(ResourceLocation id, Supplier<? extends ICustomParticleData<D>> typeFactory) {
            this.id = id;
            this.typeFactory = typeFactory;
        }

        void register(IForgeRegistry<ParticleType<?>> registry) {
            registry.register(getOrCreateType());
        }

        ParticleType<D> getOrCreateType() {
            if (type != null)
                return type;
            type = typeFactory.get()
                    .createType();
            type.setRegistryName(id);
            return type;
        }

        @OnlyIn(Dist.CLIENT)
        void registerFactory(ParticleManager particles) {
            typeFactory.get()
                    .register(getOrCreateType(), particles);
        }
    }
}
