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
    public int intervalTicks = 6000;       // 清理间隔 (默认 5 分钟)
    public int minAgeTicks = 6000;        // 物品最短存活时间
    public int playerRadius = 32;         // 玩家忽略半径
    public int warningSeconds = 10;       // 预警倒计时秒数
    public Set<UUID> ignoredPlayers = new HashSet<>();
    public List<String> ignoredChunks = new ArrayList<>();      // 格式: "dimensionId:chunkX,chunkZ"
    public List<RegionEntry> protectedRegions = new ArrayList<>();

    public static class RegionEntry {
        public String name;
        public String dimension;
        public int minX, minY, minZ;
        public int maxX, maxY, maxZ;

        public RegionEntry() {}
        public RegionEntry(String name, String dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.name = name;
            this.dimension = dimension;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    public static SweepingConfig load() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, SweepingConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SweepingConfig config = new SweepingConfig();
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修复后的忽略区块解析方法：
     * 使用 lastIndexOf(':') 正确分离带有命名空间的维度 ID。
     */
    public Map<String, Set<ChunkPos>> resolveIgnoredChunks() {
        Map<String, Set<ChunkPos>> map = new HashMap<>();
        for (String entry : ignoredChunks) {
            // 找到最后一个冒号的位置，前面为维度 ID（可能包含命名空间），后面为坐标
            int lastColonIndex = entry.lastIndexOf(':');
            if (lastColonIndex == -1) continue; // 格式错误，跳过
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
