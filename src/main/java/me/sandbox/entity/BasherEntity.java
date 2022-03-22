package me.sandbox.entity;

import com.google.common.collect.Maps;
import me.sandbox.item.ItemRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Predicate;

public class BasherEntity
        extends IllagerEntity {
    public int stunTick = 60;
    public boolean isStunned = false;
    private static final TrackedData<Boolean> STUNNED = DataTracker.registerData(BasherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public BasherEntity(EntityType<? extends BasherEntity> entityType, World world) {
        super((EntityType<? extends IllagerEntity>) entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new IllagerEntity.LongDoorInteractGoal(this));
        this.goalSelector.add(4, new BasherEntity.AttackGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge(new Class[0]));
        this.targetSelector.add(2, new ActiveTargetGoal<PlayerEntity>((MobEntity) this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<MerchantEntity>((MobEntity) this, MerchantEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<IronGolemEntity>((MobEntity) this, IronGolemEntity.class, true));
        this.goalSelector.add(8, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0f, 1.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
    }

    @Override
    protected void mobTick() {
        if (!this.isAiDisabled() && NavigationConditions.hasMobNavigation(this)) {
            boolean bl = ((ServerWorld) this.world).hasRaidAt(this.getBlockPos());
            ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(bl);
            super.mobTick();
        }
        if (!this.isAlive()) {
            return;
        }
    }
    @Override
    public boolean canSee(Entity entity) {
        if (this.getStunnedState()) {
            return false;
        }
        return super.canSee(entity);
    }
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("Stunned", this.isStunned);
        super.writeCustomDataToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setStunnedState(nbt.getBoolean("Stunned"));
    }
    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(STUNNED, false);
        super.initDataTracker();
    }
    public void setStunnedState(boolean isStunned) {
        this.dataTracker.set(STUNNED, isStunned);
    }
    public boolean getStunnedState() {
        return this.dataTracker.get(STUNNED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }
        if (this.getStunnedState()) {
            --stunTick;
            if (stunTick == 0) {
                this.setStunnedState(false);
            }
        }
    }
    private AttributeContainer attributeContainer;

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.getStunnedState();
    }

    @Override
    public AttributeContainer getAttributes() {
        if (attributeContainer == null) {
            attributeContainer = new AttributeContainer(HostileEntity.createHostileAttributes()
                    .add(EntityAttributes.GENERIC_MAX_HEALTH, 28.0D)
                    .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31D)
                    .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                    .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK,0.2D)
                    .build());
        }
        return attributeContainer;
    }


    @Override
    public IllagerEntity.State getState() {
        if (this.isCelebrating()) {
            return IllagerEntity.State.CELEBRATING;
        }
        if (this.isAttacking()) {
            return State.ATTACKING;
        }
        return IllagerEntity.State.CROSSED;
    }

    @Override
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_VINDICATOR_CELEBRATE;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        boolean hasShield = this.getMainHandStack().isOf(Items.SHIELD);
        if (isAttacking()) {
            Random random = new Random();
            int canBlockMelee = random.nextInt(5);
            if (attacker instanceof LivingEntity) {
                ItemStack item = ((LivingEntity) attacker).getMainHandStack();
                ItemStack basherItem = this.getMainHandStack();
                if ((item.isOf(Items.DIAMOND_AXE) || item.isOf(Items.IRON_AXE) || item.isOf(Items.GOLDEN_AXE) || item.isOf(Items.NETHERITE_AXE) || item.isOf(Items.STONE_AXE) || item.isOf(Items.WOODEN_AXE)) && basherItem.isOf(Items.SHIELD) || (attacker instanceof IronGolemEntity && basherItem.isOf(Items.SHIELD))) {
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                    this.setStunnedState(true);
                    if (world instanceof ServerWorld) {
                        ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, basherItem), this.getX(), this.getY()+1.5, this.getZ(), 30, 0.3D, 0.2D, 0.3D, 0.003);
                    }
                    this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));

                }
            }
            if ((source.getSource()) instanceof PersistentProjectileEntity && hasShield) {
                this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                return false;
            }
            if ((source.getSource()) instanceof LivingEntity && !(canBlockMelee == 0) && hasShield) {
                this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                return false;
            }
        }
        boolean bl2 = super.damage(source, amount);
        return bl2;
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData entityData2 = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.initEquipment(difficulty);
        this.updateEnchantments(difficulty);
        return entityData2;
    }

    @Override
    protected void initEquipment(LocalDifficulty difficulty) {
        if (this.getRaid() == null) {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SHIELD));
        }
    }

    @Override
    public boolean isTeammate(Entity other) {
        if (super.isTeammate(other)) {
            return true;
        }
        if (other instanceof LivingEntity && ((LivingEntity) other).getGroup() == EntityGroup.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        }
        return false;
    }


    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VINDICATOR_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VINDICATOR_HURT;
    }

    @Override
    public void addBonusForWave(int wave, boolean unused) {
        boolean bl;
        ItemStack itemStack = new ItemStack(Items.SHIELD);
        Raid raid = this.getRaid();
        int i = 1;
        if (wave > raid.getMaxWaves(Difficulty.NORMAL)) {
            i = 2;
        }
        boolean bl2 = bl = this.random.nextFloat() <= raid.getEnchantmentChance();
        if (bl) {
            HashMap<Enchantment, Integer> map = Maps.newHashMap();
            map.put(Enchantments.UNBREAKING, i);
            EnchantmentHelper.set(map, itemStack);
        }
        this.equipStack(EquipmentSlot.MAINHAND, itemStack);
    }

    class AttackGoal
            extends MeleeAttackGoal {
        public AttackGoal(BasherEntity vindicator) {
            super(vindicator, 1.0, false);

        }

        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity entity) {
            if (this.mob.getVehicle() instanceof RavagerEntity) {
                float f = this.mob.getVehicle().getWidth() - 0.1f;
                return f * 2.0f * (f * 2.0f) + entity.getWidth();
            }
            return super.getSquaredMaxAttackDistance(entity);
        }
    }
}
