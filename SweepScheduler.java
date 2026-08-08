package com.example.sweeping;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class SweepScheduler {
    private static final Map<RegistryKey<World>, Integer> tickCounters = new HashMap<>();
    private static final Map<RegistryKey<World>, Integer> lastWarningSecond = new HashMap<>();
    private static final Map<RegistryKey<World>, Integer> forceCleanCounters = new HashMap<>();

    public static void onServerTick(MinecraftServer server) {
        SweepingConfig config = SweepingMod.CONFIG;
        if (!config.enabled) return;

        int warningTicks = config.warningSeconds * 20;
        if (warningTicks > config.intervalTicks) warningTicks = config.intervalTicks;

        for (ServerWorld world : server.getWorlds()) {
            RegistryKey<World> dim = world.getRegistryKey();
            int counter = tickCounters.getOrDefault(dim, 0) + 1;
            tickCounters.put(dim, counter);

            // 预警
            if (config.warningSeconds > 0 && counter >= config.intervalTicks - warningTicks && counter < config.intervalTicks) {
                int remainingTicks = config.intervalTicks - counter;
                int currentSecond = (remainingTicks + 19) / 20;
                int lastSec = lastWarningSecond.getOrDefault(dim, -1);
                if (currentSecond != lastSec) {
                    lastWarningSecond.put(dim, currentSecond);
                    Text msg = Text.literal("§dJay1145酱：§c" + currentSecond + " §d秒后垃圾和恶心的小怪统统消失！没捡完就哭吧！");
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        player.sendMessage(msg, true);
                    }
                }
            }

            // 定时清理
            if (counter >= config.intervalTicks) {
                lastWarningSecond.remove(dim);
                performFullClean(world, false);
                tickCounters.put(dim, 0);
            }

            // 强制清扫检查
            if (config.forceCleanEnabled) {
                int scanCounter = forceCleanCounters.getOrDefault(dim, 0) + 1;
                if (scanCounter >= config.forceCleanScanInterval) {
                    forceCleanCounters.put(dim, 0);
                    checkAndForceClean(world);
                } else {
                    forceCleanCounters.put(dim, scanCounter);
                }
            }
        }
    }

    private static void checkAndForceClean(ServerWorld world) {
        SweepingConfig config = SweepingMod.CONFIG;
        int itemCount = 0, xpCount = 0;
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof ItemEntity) itemCount++;
            else if (entity instanceof ExperienceOrbEntity) xpCount++;
            if (itemCount >= config.forceCleanItemThreshold && xpCount >= config.forceCleanXpThreshold) break;
        }
        if (itemCount >= config.forceCleanItemThreshold || xpCount >= config.forceCleanXpThreshold) {
            performFullClean(world, config.forceCleanIgnoreAge);
            for (ServerPlayerEntity player : world.getPlayers()) {
                player.sendMessage(Text.literal("§dJay1145酱：§c脏死了！§6" + itemCount + "个垃圾、" + xpCount + "个经验球§c堆成山，我忍不了啦！"), false);
            }
        }
    }

    private static void performFullClean(ServerWorld world, boolean ignoreAge) {
        SweepingConfig config = SweepingMod.CONFIG;
        long minAge = ignoreAge ? 0 : config.minAgeTicks;
        String dimId = world.getRegistryKey().getValue().toString();

        // 受保护玩家位置
        List<Vec3d> protectedPlayerPositions = new ArrayList<>();
        List<UUID> protectedUuids = new ArrayList<>(config.ignoredPlayers);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (protectedUuids.contains(player.getUuid())) {
                protectedPlayerPositions.add(player.getPos());
            }
        }

        Set<ChunkPos> ignoredChunkSet = config.resolveIgnoredChunks().getOrDefault(dimId, Set.of());
        List<SweepingConfig.RegionEntry> regions = config.getRegionsForDimension(dimId);

        // 所有玩家位置（用于超远清理）
        List<Vec3d> allPlayerPositions = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            allPlayerPositions.add(player.getPos());
        }

        // 清理物品
        int itemCleaned = 0;
        for (ItemEntity item : world.getEntitiesByType(EntityType.ITEM, e -> true)) {
            if (!ignoreAge && item.getItemAge() < minAge) continue;
            if (isProtected(world, item.getPos(), item.getBlockPos(), item.getChunkPos(),
                    protectedPlayerPositions, ignoredChunkSet, regions, allPlayerPositions, config)) continue;
            item.kill();
            itemCleaned++;
        }

        // 清理生物（直接移除，不触发死亡掉落）
        int entityCleaned = 0;
        if (config.entityCleanEnabled && !config.entitiesToClean.isEmpty()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof PlayerEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity)
                    continue;
                String typeId = EntityType.getId(entity.getType()).toString();
                if (!config.entitiesToClean.contains(typeId)) continue;

                // 带标签的保护
                if (entity.hasCustomName()) continue;

                // 区块/区域保护内的生物交给原版
                BlockPos bp = entity.getBlockPos();
                ChunkPos cp = entity.getChunkPos();
                if (ignoredChunkSet.contains(cp)) continue;
                boolean inRegion = false;
                for (SweepingConfig.RegionEntry re : regions) {
                    if (bp.getX() >= re.minX && bp.getX() <= re.maxX &&
                        bp.getY() >= re.minY && bp.getY() <= re.maxY &&
                        bp.getZ() >= re.minZ && bp.getZ() <= re.maxZ) {
                        inRegion = true;
                        break;
                    }
                }
                if (inRegion) continue;

                // 玩家周围保护
                if (!protectedPlayerPositions.isEmpty()) {
                    Vec3d epos = entity.getPos();
                    int r2 = config.playerRadius * config.playerRadius;
                    boolean nearPlayer = false;
                    for (Vec3d pp : protectedPlayerPositions) {
                        if (pp.squaredDistanceTo(epos) <= r2) {
                            nearPlayer = true;
                            break;
                        }
                    }
                    if (nearPlayer) continue;
                }

                // 超远清理规则
                if (config.distantCleanEnabled && !allPlayerPositions.isEmpty()) {
                    double d2 = config.distantCleanRadius * config.distantCleanRadius;
                    boolean near = false;
                    for (Vec3d pp : allPlayerPositions) {
                        if (pp.squaredDistanceTo(entity.getPos()) <= d2) {
                            near = true;
                            break;
                        }
                    }
                    if (!near) {
                        // 直接删除，不触发死亡动画/掉落
                        entity.remove(Entity.RemovalReason.DISCARDED);
                        entityCleaned++;
                        continue;
                    }
                }

                // 正常清理：直接删除
                entity.remove(Entity.RemovalReason.DISCARDED);
                entityCleaned++;
            }
        }

        // 傲娇总结
        int total = itemCleaned + entityCleaned;
        if (total > 0) {
            String msg = "§dJay1145酱：§a大扫除结束！扔掉了§6" + itemCleaned + "§a个垃圾";
            if (entityCleaned > 0) msg += "，还踹飞了§6" + entityCleaned + "§a只讨厌的小怪";
            msg += "。§d以后自己打扫！";
            for (ServerPlayerEntity player : world.getPlayers()) {
                player.sendMessage(Text.literal(msg), true);
            }
        } else if (!ignoreAge) {
            Text msg = Text.literal("§dJay1145酱：§a哼，难得这么干净，但别指望我会夸你！");
            for (ServerPlayerEntity player : world.getPlayers()) {
                player.sendMessage(msg, true);
            }
        }
    }

    private static boolean isProtected(ServerWorld world, Vec3d pos, BlockPos blockPos, ChunkPos chunkPos,
                                       List<Vec3d> protectedPlayerPositions,
                                       Set<ChunkPos> ignoredChunkSet,
                                       List<SweepingConfig.RegionEntry> regions,
                                       List<Vec3d> allPlayerPositions,
                                       SweepingConfig config) {
        if (!protectedPlayerPositions.isEmpty()) {
            int r2 = config.playerRadius * config.playerRadius;
            for (Vec3d pp : protectedPlayerPositions) {
                if (pp.squaredDistanceTo(pos) <= r2) return true;
            }
        }
        if (ignoredChunkSet.contains(chunkPos)) return true;
        for (SweepingConfig.RegionEntry re : regions) {
            if (blockPos.getX() >= re.minX && blockPos.getX() <= re.maxX &&
                blockPos.getY() >= re.minY && blockPos.getY() <= re.maxY &&
                blockPos.getZ() >= re.minZ && blockPos.getZ() <= re.maxZ) return true;
        }
        if (config.distantCleanEnabled && !allPlayerPositions.isEmpty()) {
            double d2 = (double) config.distantCleanRadius * config.distantCleanRadius;
            boolean near = false;
            for (Vec3d pp : allPlayerPositions) {
                if (pp.squaredDistanceTo(pos) <= d2) {
                    near = true;
                    break;
                }
            }
            if (!near) return false;
        }
        return false;
    }

    public static void cleanNow(ServerWorld world) {
        performFullClean(world, SweepingMod.CONFIG.forceCleanIgnoreAge);
    }
}
