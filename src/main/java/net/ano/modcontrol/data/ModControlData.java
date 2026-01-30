package net.ano.modcontrol.data;

import net.ano.modcontrol.AnosModControl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModControlData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Spawn control sets
    private static final Set<ResourceLocation> disabledEntities = new HashSet<>();
    private static final Set<String> disabledMods = new HashSet<>();
    
    // Block break control sets
    private static final Set<ResourceLocation> blockBreakDisabledEntities = new HashSet<>();
    private static final Set<String> blockBreakDisabledMods = new HashSet<>();
    
    // Attack control - disabled (off)
    private static final Map<ResourceLocation, Set<String>> attackDisabledEntities = new HashMap<>();
    private static final Map<String, Set<String>> attackDisabledMods = new HashMap<>();
    
    // Attack control - forced (on with targets)
    private static final Map<ResourceLocation, Set<String>> attackForcedEntities = new HashMap<>();
    private static final Map<String, Set<String>> attackForcedMods = new HashMap<>();
    
    // Anger control - disabled (off)
    private static final Map<ResourceLocation, Set<String>> angerDisabledEntities = new HashMap<>();
    private static final Map<String, Set<String>> angerDisabledMods = new HashMap<>();
    
    // Anger control - forced (on with targets)
    private static final Map<ResourceLocation, Set<String>> angerForcedEntities = new HashMap<>();
    private static final Map<String, Set<String>> angerForcedMods = new HashMap<>();

    // ============ Spawn Control Methods ============
    
    public static boolean isEntitySpawnDisabled(ResourceLocation entityId) {
        if (disabledEntities.contains(entityId)) return true;
        return disabledMods.contains(entityId.getNamespace());
    }

    public static void setEntitySpawnEnabled(ResourceLocation entityId, boolean enabled) {
        if (enabled) disabledEntities.remove(entityId);
        else disabledEntities.add(entityId);
    }

    public static void setModSpawnEnabled(String modId, boolean enabled) {
        if (enabled) disabledMods.remove(modId);
        else disabledMods.add(modId);
    }

    public static boolean isModSpawnDisabled(String modId) {
        return disabledMods.contains(modId);
    }

    public static Set<ResourceLocation> getDisabledEntities() {
        return new HashSet<>(disabledEntities);
    }

    public static Set<String> getDisabledMods() {
        return new HashSet<>(disabledMods);
    }

    // ============ Block Break Control Methods ============
    
    public static boolean isEntityBlockBreakDisabled(ResourceLocation entityId) {
        if (blockBreakDisabledEntities.contains(entityId)) return true;
        return blockBreakDisabledMods.contains(entityId.getNamespace());
    }

    public static void setEntityBlockBreakEnabled(ResourceLocation entityId, boolean enabled) {
        if (enabled) blockBreakDisabledEntities.remove(entityId);
        else blockBreakDisabledEntities.add(entityId);
    }

    public static void setModBlockBreakEnabled(String modId, boolean enabled) {
        if (enabled) blockBreakDisabledMods.remove(modId);
        else blockBreakDisabledMods.add(modId);
    }

    public static boolean isModBlockBreakDisabled(String modId) {
        return blockBreakDisabledMods.contains(modId);
    }

    public static Set<ResourceLocation> getBlockBreakDisabledEntities() {
        return new HashSet<>(blockBreakDisabledEntities);
    }

    public static Set<String> getBlockBreakDisabledMods() {
        return new HashSet<>(blockBreakDisabledMods);
    }

    // ============ Attack Control Methods ============
    
    public static boolean isAttackDisabled(ResourceLocation attackerId, ResourceLocation targetId, String targetPlayerName) {
        Set<String> entityTargets = attackDisabledEntities.get(attackerId);
        if (entityTargets != null && matchesTarget(entityTargets, targetId, targetPlayerName)) {
            return true;
        }
        Set<String> modTargets = attackDisabledMods.get(attackerId.getNamespace());
        return modTargets != null && matchesTarget(modTargets, targetId, targetPlayerName);
    }
    
    public static boolean isAttackForced(ResourceLocation attackerId, ResourceLocation targetId, String targetPlayerName) {
        Set<String> entityTargets = attackForcedEntities.get(attackerId);
        if (entityTargets != null && matchesTarget(entityTargets, targetId, targetPlayerName)) {
            return true;
        }
        Set<String> modTargets = attackForcedMods.get(attackerId.getNamespace());
        return modTargets != null && matchesTarget(modTargets, targetId, targetPlayerName);
    }
    
    public static Set<String> getForcedAttackTargets(ResourceLocation entityId) {
        Set<String> targets = new HashSet<>();
        Set<String> entityTargets = attackForcedEntities.get(entityId);
        if (entityTargets != null) targets.addAll(entityTargets);
        Set<String> modTargets = attackForcedMods.get(entityId.getNamespace());
        if (modTargets != null) targets.addAll(modTargets);
        return targets;
    }

    public static void setEntityAttackEnabled(ResourceLocation entityId, boolean enabled, Set<String> targets) {
        if (enabled) {
            // "on" = force attacks toward targets OR remove restrictions if no specific targets
            if (targets.contains("*")) {
                // Remove all restrictions
                attackDisabledEntities.remove(entityId);
                attackForcedEntities.remove(entityId);
            } else {
                // Force attacks toward specific targets
                attackForcedEntities.computeIfAbsent(entityId, k -> new HashSet<>()).addAll(targets);
                // Also remove any disabled restrictions for these targets
                Set<String> disabled = attackDisabledEntities.get(entityId);
                if (disabled != null) {
                    disabled.removeAll(targets);
                    if (disabled.isEmpty()) attackDisabledEntities.remove(entityId);
                }
            }
        } else {
            // "off" = disable attacks toward targets
            attackDisabledEntities.computeIfAbsent(entityId, k -> new HashSet<>()).addAll(targets);
            // Remove any forced attacks for these targets
            Set<String> forced = attackForcedEntities.get(entityId);
            if (forced != null) {
                forced.removeAll(targets);
                if (forced.isEmpty()) attackForcedEntities.remove(entityId);
            }
        }
    }

    public static void setModAttackEnabled(String modId, boolean enabled, Set<String> targets) {
        if (enabled) {
            if (targets.contains("*")) {
                attackDisabledMods.remove(modId);
                attackForcedMods.remove(modId);
            } else {
                attackForcedMods.computeIfAbsent(modId, k -> new HashSet<>()).addAll(targets);
                Set<String> disabled = attackDisabledMods.get(modId);
                if (disabled != null) {
                    disabled.removeAll(targets);
                    if (disabled.isEmpty()) attackDisabledMods.remove(modId);
                }
            }
        } else {
            attackDisabledMods.computeIfAbsent(modId, k -> new HashSet<>()).addAll(targets);
            Set<String> forced = attackForcedMods.get(modId);
            if (forced != null) {
                forced.removeAll(targets);
                if (forced.isEmpty()) attackForcedMods.remove(modId);
            }
        }
    }

    public static Map<ResourceLocation, Set<String>> getAttackDisabledEntities() {
        Map<ResourceLocation, Set<String>> copy = new HashMap<>();
        attackDisabledEntities.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    public static Map<String, Set<String>> getAttackDisabledMods() {
        Map<String, Set<String>> copy = new HashMap<>();
        attackDisabledMods.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    public static Map<ResourceLocation, Set<String>> getAttackForcedEntities() {
        Map<ResourceLocation, Set<String>> copy = new HashMap<>();
        attackForcedEntities.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    public static Map<String, Set<String>> getAttackForcedMods() {
        Map<String, Set<String>> copy = new HashMap<>();
        attackForcedMods.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    // ============ Anger Control Methods ============
    
    public static boolean isAngerDisabled(ResourceLocation entityId, ResourceLocation targetId, String targetPlayerName) {
        Set<String> entityTargets = angerDisabledEntities.get(entityId);
        if (entityTargets != null && matchesTarget(entityTargets, targetId, targetPlayerName)) {
            return true;
        }
        Set<String> modTargets = angerDisabledMods.get(entityId.getNamespace());
        return modTargets != null && matchesTarget(modTargets, targetId, targetPlayerName);
    }
    
    public static boolean isAngerForced(ResourceLocation entityId, ResourceLocation targetId, String targetPlayerName) {
        Set<String> entityTargets = angerForcedEntities.get(entityId);
        if (entityTargets != null && matchesTarget(entityTargets, targetId, targetPlayerName)) {
            return true;
        }
        Set<String> modTargets = angerForcedMods.get(entityId.getNamespace());
        return modTargets != null && matchesTarget(modTargets, targetId, targetPlayerName);
    }
    
    public static Set<String> getForcedAngerTargets(ResourceLocation entityId) {
        Set<String> targets = new HashSet<>();
        Set<String> entityTargets = angerForcedEntities.get(entityId);
        if (entityTargets != null) targets.addAll(entityTargets);
        Set<String> modTargets = angerForcedMods.get(entityId.getNamespace());
        if (modTargets != null) targets.addAll(modTargets);
        return targets;
    }

    public static void setEntityAngerEnabled(ResourceLocation entityId, boolean enabled, Set<String> targets) {
        if (enabled) {
            if (targets.contains("*")) {
                angerDisabledEntities.remove(entityId);
                angerForcedEntities.remove(entityId);
            } else {
                angerForcedEntities.computeIfAbsent(entityId, k -> new HashSet<>()).addAll(targets);
                Set<String> disabled = angerDisabledEntities.get(entityId);
                if (disabled != null) {
                    disabled.removeAll(targets);
                    if (disabled.isEmpty()) angerDisabledEntities.remove(entityId);
                }
            }
        } else {
            angerDisabledEntities.computeIfAbsent(entityId, k -> new HashSet<>()).addAll(targets);
            Set<String> forced = angerForcedEntities.get(entityId);
            if (forced != null) {
                forced.removeAll(targets);
                if (forced.isEmpty()) angerForcedEntities.remove(entityId);
            }
        }
    }

    public static void setModAngerEnabled(String modId, boolean enabled, Set<String> targets) {
        if (enabled) {
            if (targets.contains("*")) {
                angerDisabledMods.remove(modId);
                angerForcedMods.remove(modId);
            } else {
                angerForcedMods.computeIfAbsent(modId, k -> new HashSet<>()).addAll(targets);
                Set<String> disabled = angerDisabledMods.get(modId);
                if (disabled != null) {
                    disabled.removeAll(targets);
                    if (disabled.isEmpty()) angerDisabledMods.remove(modId);
                }
            }
        } else {
            angerDisabledMods.computeIfAbsent(modId, k -> new HashSet<>()).addAll(targets);
            Set<String> forced = angerForcedMods.get(modId);
            if (forced != null) {
                forced.removeAll(targets);
                if (forced.isEmpty()) angerForcedMods.remove(modId);
            }
        }
    }

    public static Map<ResourceLocation, Set<String>> getAngerDisabledEntities() {
        Map<ResourceLocation, Set<String>> copy = new HashMap<>();
        angerDisabledEntities.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    public static Map<String, Set<String>> getAngerDisabledMods() {
        Map<String, Set<String>> copy = new HashMap<>();
        angerDisabledMods.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    public static Map<ResourceLocation, Set<String>> getAngerForcedEntities() {
        Map<ResourceLocation, Set<String>> copy = new HashMap<>();
        angerForcedEntities.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    public static Map<String, Set<String>> getAngerForcedMods() {
        Map<String, Set<String>> copy = new HashMap<>();
        angerForcedMods.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }
    
    // ============ Target Matching ============
    
    private static boolean matchesTarget(Set<String> targets, ResourceLocation targetEntityId, String targetPlayerName) {
        for (String target : targets) {
            if (matchesSingleTarget(target, targetEntityId, targetPlayerName)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean matchesSingleTarget(String target, ResourceLocation targetEntityId, String targetPlayerName) {
        if (target.equals("*")) return true;
        if ((target.equals("@a") || target.equals("@p") || target.equals("@r")) && targetPlayerName != null) return true;
        if (target.equals("@e")) return true;
        if (target.equals("@s")) return true;
        if (targetPlayerName != null && target.equalsIgnoreCase(targetPlayerName)) return true;
        
        if (targetEntityId != null) {
            if (target.contains(":")) {
                if (isValidResourceLocation(target)) {
                    try {
                        ResourceLocation targetRL = new ResourceLocation(target.toLowerCase());
                        if (targetRL.equals(targetEntityId)) return true;
                    } catch (Exception ignored) {}
                }
            } else {
                if (target.equalsIgnoreCase(targetEntityId.getPath())) return true;
                if (isValidResourceLocationPath(target.toLowerCase())) {
                    try {
                        ResourceLocation targetRL = new ResourceLocation("minecraft", target.toLowerCase());
                        if (targetRL.equals(targetEntityId)) return true;
                    } catch (Exception ignored) {}
                }
            }
        }
        return false;
    }
    
    private static boolean isValidResourceLocation(String str) {
        if (str == null || str.isEmpty()) return false;
        String[] parts = str.split(":", 2);
        if (parts.length != 2) return false;
        return isValidResourceLocationPath(parts[0]) && isValidResourceLocationPath(parts[1]);
    }
    
    private static boolean isValidResourceLocationPath(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' || c == '/')) {
                return false;
            }
        }
        return true;
    }

    // ============ Save/Load Methods ============
    
    public static void load(MinecraftServer server) {
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("anos_modcontrol");
        try {
            Files.createDirectories(configPath);
            loadSpawnData(configPath);
            loadBlockBreakData(configPath);
            loadAttackData(configPath);
            loadAngerData(configPath);
        } catch (IOException e) {
            AnosModControl.LOGGER.error("Failed to load Ano's ModControl data", e);
        }
    }

    private static void loadSpawnData(Path configPath) throws IOException {
        Path spawnFile = configPath.resolve("spawn_control.json");
        if (Files.exists(spawnFile)) {
            try (Reader reader = Files.newBufferedReader(spawnFile)) {
                SpawnSaveData data = GSON.fromJson(reader, SpawnSaveData.class);
                if (data != null) {
                    disabledEntities.clear();
                    disabledMods.clear();
                    if (data.disabledEntities != null) {
                        for (String s : data.disabledEntities) {
                            try { disabledEntities.add(new ResourceLocation(s)); } catch (Exception ignored) {}
                        }
                    }
                    if (data.disabledMods != null) disabledMods.addAll(data.disabledMods);
                }
            }
        }
    }

    private static void loadBlockBreakData(Path configPath) throws IOException {
        Path blockBreakFile = configPath.resolve("block_break_control.json");
        if (Files.exists(blockBreakFile)) {
            try (Reader reader = Files.newBufferedReader(blockBreakFile)) {
                SpawnSaveData data = GSON.fromJson(reader, SpawnSaveData.class);
                if (data != null) {
                    blockBreakDisabledEntities.clear();
                    blockBreakDisabledMods.clear();
                    if (data.disabledEntities != null) {
                        for (String s : data.disabledEntities) {
                            try { blockBreakDisabledEntities.add(new ResourceLocation(s)); } catch (Exception ignored) {}
                        }
                    }
                    if (data.disabledMods != null) blockBreakDisabledMods.addAll(data.disabledMods);
                }
            }
        }
    }

    private static void loadAttackData(Path configPath) throws IOException {
        Path attackFile = configPath.resolve("attack_control.json");
        if (Files.exists(attackFile)) {
            try (Reader reader = Files.newBufferedReader(attackFile)) {
                FullTargetedSaveData data = GSON.fromJson(reader, FullTargetedSaveData.class);
                if (data != null) {
                    attackDisabledEntities.clear();
                    attackDisabledMods.clear();
                    attackForcedEntities.clear();
                    attackForcedMods.clear();
                    if (data.disabledEntities != null) {
                        data.disabledEntities.forEach((k, v) -> {
                            try { attackDisabledEntities.put(new ResourceLocation(k), new HashSet<>(v)); } catch (Exception ignored) {}
                        });
                    }
                    if (data.disabledMods != null) {
                        data.disabledMods.forEach((k, v) -> attackDisabledMods.put(k, new HashSet<>(v)));
                    }
                    if (data.forcedEntities != null) {
                        data.forcedEntities.forEach((k, v) -> {
                            try { attackForcedEntities.put(new ResourceLocation(k), new HashSet<>(v)); } catch (Exception ignored) {}
                        });
                    }
                    if (data.forcedMods != null) {
                        data.forcedMods.forEach((k, v) -> attackForcedMods.put(k, new HashSet<>(v)));
                    }
                }
            }
        }
    }

    private static void loadAngerData(Path configPath) throws IOException {
        Path angerFile = configPath.resolve("anger_control.json");
        if (Files.exists(angerFile)) {
            try (Reader reader = Files.newBufferedReader(angerFile)) {
                FullTargetedSaveData data = GSON.fromJson(reader, FullTargetedSaveData.class);
                if (data != null) {
                    angerDisabledEntities.clear();
                    angerDisabledMods.clear();
                    angerForcedEntities.clear();
                    angerForcedMods.clear();
                    if (data.disabledEntities != null) {
                        data.disabledEntities.forEach((k, v) -> {
                            try { angerDisabledEntities.put(new ResourceLocation(k), new HashSet<>(v)); } catch (Exception ignored) {}
                        });
                    }
                    if (data.disabledMods != null) {
                        data.disabledMods.forEach((k, v) -> angerDisabledMods.put(k, new HashSet<>(v)));
                    }
                    if (data.forcedEntities != null) {
                        data.forcedEntities.forEach((k, v) -> {
                            try { angerForcedEntities.put(new ResourceLocation(k), new HashSet<>(v)); } catch (Exception ignored) {}
                        });
                    }
                    if (data.forcedMods != null) {
                        data.forcedMods.forEach((k, v) -> angerForcedMods.put(k, new HashSet<>(v)));
                    }
                }
            }
        }
    }

    public static void save(MinecraftServer server) {
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("anos_modcontrol");
        try {
            Files.createDirectories(configPath);
            saveSpawnData(configPath);
            saveBlockBreakData(configPath);
            saveAttackData(configPath);
            saveAngerData(configPath);
        } catch (IOException e) {
            AnosModControl.LOGGER.error("Failed to save Ano's ModControl data", e);
        }
    }

    private static void saveSpawnData(Path configPath) throws IOException {
        Path spawnFile = configPath.resolve("spawn_control.json");
        SpawnSaveData data = new SpawnSaveData();
        data.disabledEntities = new HashSet<>();
        disabledEntities.forEach(rl -> data.disabledEntities.add(rl.toString()));
        data.disabledMods = new HashSet<>(disabledMods);
        try (Writer writer = Files.newBufferedWriter(spawnFile)) { GSON.toJson(data, writer); }
    }

    private static void saveBlockBreakData(Path configPath) throws IOException {
        Path blockBreakFile = configPath.resolve("block_break_control.json");
        SpawnSaveData data = new SpawnSaveData();
        data.disabledEntities = new HashSet<>();
        blockBreakDisabledEntities.forEach(rl -> data.disabledEntities.add(rl.toString()));
        data.disabledMods = new HashSet<>(blockBreakDisabledMods);
        try (Writer writer = Files.newBufferedWriter(blockBreakFile)) { GSON.toJson(data, writer); }
    }

    private static void saveAttackData(Path configPath) throws IOException {
        Path attackFile = configPath.resolve("attack_control.json");
        FullTargetedSaveData data = new FullTargetedSaveData();
        data.disabledEntities = new HashMap<>();
        attackDisabledEntities.forEach((k, v) -> data.disabledEntities.put(k.toString(), new HashSet<>(v)));
        data.disabledMods = new HashMap<>();
        attackDisabledMods.forEach((k, v) -> data.disabledMods.put(k, new HashSet<>(v)));
        data.forcedEntities = new HashMap<>();
        attackForcedEntities.forEach((k, v) -> data.forcedEntities.put(k.toString(), new HashSet<>(v)));
        data.forcedMods = new HashMap<>();
        attackForcedMods.forEach((k, v) -> data.forcedMods.put(k, new HashSet<>(v)));
        try (Writer writer = Files.newBufferedWriter(attackFile)) { GSON.toJson(data, writer); }
    }

    private static void saveAngerData(Path configPath) throws IOException {
        Path angerFile = configPath.resolve("anger_control.json");
        FullTargetedSaveData data = new FullTargetedSaveData();
        data.disabledEntities = new HashMap<>();
        angerDisabledEntities.forEach((k, v) -> data.disabledEntities.put(k.toString(), new HashSet<>(v)));
        data.disabledMods = new HashMap<>();
        angerDisabledMods.forEach((k, v) -> data.disabledMods.put(k, new HashSet<>(v)));
        data.forcedEntities = new HashMap<>();
        angerForcedEntities.forEach((k, v) -> data.forcedEntities.put(k.toString(), new HashSet<>(v)));
        data.forcedMods = new HashMap<>();
        angerForcedMods.forEach((k, v) -> data.forcedMods.put(k, new HashSet<>(v)));
        try (Writer writer = Files.newBufferedWriter(angerFile)) { GSON.toJson(data, writer); }
    }

    private static class SpawnSaveData {
        Set<String> disabledEntities;
        Set<String> disabledMods;
    }

    private static class FullTargetedSaveData {
        Map<String, Set<String>> disabledEntities;
        Map<String, Set<String>> disabledMods;
        Map<String, Set<String>> forcedEntities;
        Map<String, Set<String>> forcedMods;
    }
}