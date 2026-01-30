package net.ano.modcontrol.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ModEntityUtils {
    
    private static Set<String> modsWithEntities = null;
    private static Set<String> modsWithBlockBreakers = null;
    private static Set<ResourceLocation> blockBreakingEntities = null;
    private static Set<String> modsWithHostileOrNeutral = null;
    private static Set<ResourceLocation> hostileOrNeutralEntities = null;

    public static Set<String> getModsWithEntities() {
        if (modsWithEntities == null) {
            modsWithEntities = new HashSet<>();
            ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
                ResourceLocation key = entry.getKey().location();
                EntityType<?> entityType = entry.getValue();
                if (isLivingEntityType(entityType)) {
                    modsWithEntities.add(key.getNamespace());
                }
            });
        }
        return new HashSet<>(modsWithEntities);
    }

    public static Set<ResourceLocation> getEntitiesForMod(String modId) {
        Set<ResourceLocation> entities = new HashSet<>();
        ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
            ResourceLocation key = entry.getKey().location();
            if (key.getNamespace().equals(modId) && isLivingEntityType(entry.getValue())) {
                entities.add(key);
            }
        });
        return entities;
    }

    public static Set<ResourceLocation> getAllLivingEntities() {
        Set<ResourceLocation> entities = new HashSet<>();
        ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
            if (isLivingEntityType(entry.getValue())) {
                entities.add(entry.getKey().location());
            }
        });
        return entities;
    }

    private static boolean isLivingEntityType(EntityType<?> entityType) {
        try {
            return entityType.getCategory() != MobCategory.MISC || isKnownLivingEntity(entityType);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isKnownLivingEntity(EntityType<?> entityType) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (id == null) return false;
        Set<String> miscLivingEntities = Set.of(
            "minecraft:iron_golem", "minecraft:snow_golem", "minecraft:villager", "minecraft:wandering_trader"
        );
        return miscLivingEntities.contains(id.toString());
    }

    // ============ Block Breaking Entities ============

    public static Set<String> getModsWithBlockBreakers() {
        if (modsWithBlockBreakers == null) {
            modsWithBlockBreakers = new HashSet<>();
            getBlockBreakingEntities().forEach(rl -> modsWithBlockBreakers.add(rl.getNamespace()));
        }
        return new HashSet<>(modsWithBlockBreakers);
    }

    public static Set<ResourceLocation> getBlockBreakingEntities() {
        if (blockBreakingEntities == null) {
            blockBreakingEntities = new HashSet<>();
            addVanillaBlockBreakers();
            scanForModBlockBreakers();
        }
        return new HashSet<>(blockBreakingEntities);
    }

    private static void addVanillaBlockBreakers() {
        blockBreakingEntities.add(new ResourceLocation("minecraft", "creeper"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "wither"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "ender_dragon"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "enderman"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "zombie"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "zombie_villager"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "husk"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "drowned"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "silverfish"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "ravager"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "wither_skeleton"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "ghast"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "blaze"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "rabbit"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "snow_golem"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "fox"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "villager"));
        blockBreakingEntities.add(new ResourceLocation("minecraft", "piglin"));
    }

    private static void scanForModBlockBreakers() {
        ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
            ResourceLocation key = entry.getKey().location();
            if (key.getNamespace().equals("minecraft")) return;
            String path = key.getPath().toLowerCase();
            if (containsBlockBreakingKeyword(path)) {
                blockBreakingEntities.add(key);
            }
        });
    }

    private static boolean containsBlockBreakingKeyword(String name) {
        Set<String> keywords = Set.of(
            "creeper", "wither", "dragon", "enderman", "zombie", "ravager", "ghast", "blaze", 
            "golem", "giant", "destroyer", "demolisher", "breaker", "crusher", "exploder", "bomber", "titan", "colossus"
        );
        for (String keyword : keywords) {
            if (name.contains(keyword)) return true;
        }
        return false;
    }

    public static boolean canEntityBreakBlocks(Entity entity) {
        if (entity == null) return false;
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return false;
        return getBlockBreakingEntities().contains(entityId);
    }

    // ============ Hostile and Neutral Entities ============

    public static Set<String> getModsWithHostileOrNeutral() {
        if (modsWithHostileOrNeutral == null) {
            modsWithHostileOrNeutral = new HashSet<>();
            getHostileOrNeutralEntities().forEach(rl -> modsWithHostileOrNeutral.add(rl.getNamespace()));
        }
        return new HashSet<>(modsWithHostileOrNeutral);
    }

    public static Set<ResourceLocation> getHostileOrNeutralEntities() {
        if (hostileOrNeutralEntities == null) {
            hostileOrNeutralEntities = new HashSet<>();
            addVanillaHostileAndNeutral();
            scanForModHostileAndNeutral();
        }
        return new HashSet<>(hostileOrNeutralEntities);
    }

    private static void addVanillaHostileAndNeutral() {
        // Hostile mobs
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "zombie"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "skeleton"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "creeper"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "spider"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "cave_spider"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "enderman"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "slime"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "magma_cube"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "ghast"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "blaze"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "wither_skeleton"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "witch"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "guardian"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "elder_guardian"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "shulker"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "husk"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "stray"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "phantom"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "drowned"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "pillager"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "vindicator"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "evoker"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "vex"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "ravager"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "illusioner"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "wither"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "ender_dragon"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "silverfish"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "endermite"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "hoglin"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "zoglin"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "piglin_brute"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "warden"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "zombie_villager"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "breeze"));
        
        // Neutral mobs
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "wolf"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "iron_golem"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "snow_golem"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "bee"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "llama"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "trader_llama"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "panda"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "polar_bear"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "dolphin"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "piglin"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "zombified_piglin"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "goat"));
        hostileOrNeutralEntities.add(new ResourceLocation("minecraft", "fox"));
    }

    private static void scanForModHostileAndNeutral() {
        ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
            ResourceLocation key = entry.getKey().location();
            EntityType<?> entityType = entry.getValue();
            
            if (key.getNamespace().equals("minecraft")) return;
            
            // Check mob category
            MobCategory category = entityType.getCategory();
            if (category == MobCategory.MONSTER) {
                hostileOrNeutralEntities.add(key);
                return;
            }
            
            // Check for keywords suggesting hostile/neutral
            String path = key.getPath().toLowerCase();
            if (containsHostileOrNeutralKeyword(path)) {
                hostileOrNeutralEntities.add(key);
            }
        });
    }

    private static boolean containsHostileOrNeutralKeyword(String name) {
        Set<String> keywords = Set.of(
            "zombie", "skeleton", "creeper", "spider", "enderman", "slime", "ghast", "blaze",
            "witch", "guardian", "shulker", "phantom", "drowned", "pillager", "vindicator",
            "evoker", "vex", "ravager", "wither", "dragon", "silverfish", "endermite",
            "hoglin", "zoglin", "piglin", "warden", "wolf", "golem", "bee", "bear",
            "hostile", "monster", "demon", "beast", "predator", "hunter", "warrior",
            "brute", "giant", "titan", "boss", "minion", "guard", "sentinel"
        );
        for (String keyword : keywords) {
            if (name.contains(keyword)) return true;
        }
        return false;
    }

    public static boolean isHostileOrNeutral(Entity entity) {
        if (entity == null) return false;
        
        // Check if it's an Enemy or NeutralMob
        if (entity instanceof Enemy || entity instanceof NeutralMob) {
            return true;
        }
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return false;
        
        return getHostileOrNeutralEntities().contains(entityId);
    }

    // ============ Utility Methods ============

    public static void refreshCaches() {
        modsWithEntities = null;
        modsWithBlockBreakers = null;
        blockBreakingEntities = null;
        modsWithHostileOrNeutral = null;
        hostileOrNeutralEntities = null;
    }

    public static Optional<EntityType<?>> getEntityType(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getValue(id));
    }

    public static boolean isValidEntityMod(String modId) {
        return getModsWithEntities().contains(modId);
    }

    public static boolean isValidBlockBreakMod(String modId) {
        return getModsWithBlockBreakers().contains(modId);
    }

    public static boolean isValidHostileOrNeutralMod(String modId) {
        return getModsWithHostileOrNeutral().contains(modId);
    }
}