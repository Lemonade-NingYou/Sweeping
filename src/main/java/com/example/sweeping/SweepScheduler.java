package com.example.sweeping;

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

    public static void onServerTick(MinecraftServer server) {
        SweepingConfig config = SweepingMod.CONFIG;
        if (!config.enabled) return;

        int warningTicks = config.warningSeconds * 20;
        if (warningTicks > config.intervalTicks) warningTicks = config.intervalTicks;

        for (ServerWorld world : server.getWorlds()) {
            RegistryKey<World> dim = world.getRegistryKey();
            int counter = tickCounters.getOrDefault(dim, 0) + 1;
            tickCounters.put(dim, counter);

            // 预警倒计时 - 傲娇语气
            if (config.warningSeconds > 0 && counter >= config.intervalTicks - warningTicks && counter < config.intervalTicks) {
                int remainingTicks = config.intervalTicks - counter;
                int currentSecond = (remainingTicks + 19) / 20;
                int lastSec = lastWarningSecond.getOrDefault(dim, -1);
                if (currentSecond != lastSec) {
                    lastWarningSecond.put(dim, currentSecond);
                    Text msg = Text.literal("§eJay1145酱：§c" + currentSecond + " §e秒后就要把地板上的东西全扫光啦！赶紧捡啊笨蛋！");
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        player.sendMessage(msg, true); // action bar
                    }
                }
            }

            // 达到间隔，执行清理
            if (counter >= config.intervalTicks) {
                lastWarningSecond.remove(dim);
                int cleaned = performClean(world);
                Text doneMsg = Text.literal("Jay1145酱：§a才，才不是为你清理了 §6" + cleaned + " §a个掉落物喵！");
                for (ServerPlayerEntity player : world.getPlayers()) {
                    player.sendMessage(doneMsg, true);
                }
                tickCounters.put(dim, 0);
            }
        }
    }

    // 修改返回值为清理数量
    private static int performClean(ServerWorld world) {
        SweepingConfig config = SweepingMod.CONFIG;
        long minAge = config.minAgeTicks;
        int radiusSq = config.playerRadius * config.playerRadius;
        String dimId = world.getRegistryKey().getValue().toString();

        List<Vec3d> protectedPlayerPositions = world.getPlayers().stream()
                .filter(p -> config.ignoredPlayers.contains(p.getUuid()))
                .map(PlayerEntity::getPos)
                .toList();

        Set<ChunkPos> ignoredChunkSet = config.resolveIgnoredChunks().getOrDefault(dimId, Set.of());
        List<SweepingConfig.RegionEntry> regions = config.getRegionsForDimension(dimId);

        int count = 0;

        for (ItemEntity item : world.getEntitiesByType(net.minecraft.entity.EntityType.ITEM, e -> true)) {
            if (item.getItemAge() < minAge) continue;

            if (!protectedPlayerPositions.isEmpty()) {
                Vec3d ipos = item.getPos();
                boolean near = false;
                for (Vec3d pp : protectedPlayerPositions) {
                    if (pp.squaredDistanceTo(ipos) <= radiusSq) {
                        near = true;
                        break;
                    }
                }
                if (near) continue;
            }

            ChunkPos cp = item.getChunkPos();
            if (ignoredChunkSet.contains(cp)) continue;

            BlockPos bp = item.getBlockPos();
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

            item.kill();
            count++;
        }
        return count;
    }

    public static void cleanNow(ServerWorld world) {
        int cleaned = performClean(world);
        Text msg = Text.literal("Jay1145酱：§a哼，手动清理罢了... §6" + cleaned + " §a个垃圾已经消失啦，可别指望我感谢你！");
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(msg, true);
        }
    }
}
