package com.example.sweeping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.ChunkPos;
import java.io.*;
import java.util.*;

public class SweepingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("sweeping.json").toFile();

    public boolean enabled = true;
    public int intervalTicks = 6000;       // 公共清理间隔
    public int minAgeTicks = 6000;        // 公共最小存活时间（物品、经验、生物共用）
    public int playerRadius = 32;
    public int warningSeconds = 10;
    public Set<UUID> ignoredPlayers = new HashSet<>();
    public List<String> ignoredChunks = new ArrayList<>();          // 格式: "维度:chunkX,chunkZ"
    public List<RegionEntry> protectedRegions = new ArrayList<>();

    // 强制清扫
    public boolean forceCleanEnabled = true;
    public int forceCleanItemThreshold = 2000;
    public int forceCleanXpThreshold = 1000;
    public boolean forceCleanIgnoreAge = true;
    public int forceCleanScanInterval = 100;

    // 超远清理
    public boolean distantCleanEnabled = true;
    public int distantCleanRadius = 128;

    // 生物清理（简化）
    public boolean entityCleanEnabled = true;
    public List<String> entitiesToClean = new ArrayList<>(Arrays.asList(
            "minecraft:creeper",
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:enderman",
            "minecraft:silverfish",
            "minecraft:endermite"
    ));

    public static class RegionEntry {
        public String name;
        public String dimension;
        public int minX, minY, minZ;
        public int maxX, maxY, maxZ;
        public RegionEntry() {}
        public RegionEntry(String name, String dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.name = name; this.dimension = dimension;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        }
    }

    public static SweepingConfig load() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, SweepingConfig.class);
            } catch (IOException e) { e.printStackTrace(); }
        }
        SweepingConfig config = new SweepingConfig();
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public Map<String, Set<ChunkPos>> resolveIgnoredChunks() {
        Map<String, Set<ChunkPos>> map = new HashMap<>();
        for (String entry : ignoredChunks) {
            int lastColonIndex = entry.lastIndexOf(':');
            if (lastColonIndex == -1) continue;
            String dim = entry.substring(0, lastColonIndex);
            String coordPart = entry.substring(lastColonIndex + 1);
            String[] coords = coordPart.split(",");
            if (coords.length != 2) continue;
            try {
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);
                map.computeIfAbsent(dim, k -> new HashSet<>()).add(new ChunkPos(x, z));
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    public List<RegionEntry> getRegionsForDimension(String dimId) {
        List<RegionEntry> list = new ArrayList<>();
        for (RegionEntry re : protectedRegions) {
            if (re.dimension.equals(dimId)) list.add(re);
        }
        return list;
    }
}
