package me.sandbox.client.particle;

import me.sandbox.IllagerExpansion;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ParticleRegistry {
    public static final DefaultParticleType MAGIC_FLAME = FabricParticleTypes.simple();
    public static final DefaultParticleType NECROMANCER_BUFF = FabricParticleTypes.simple();

    public static void registerParticles() {
        Registry.register(Registry.PARTICLE_TYPE, new Identifier(IllagerExpansion.MOD_ID, "magic_flame"), MAGIC_FLAME);
        Registry.register(Registry.PARTICLE_TYPE, new Identifier(IllagerExpansion.MOD_ID, "necromancer_buff"), NECROMANCER_BUFF);
    }
}
