package com.example.sweeping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;

public class SweepCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                RegistrationEnvironment environment) {
        dispatcher.register(literal("sweeping")
                .requires(src -> src.hasPermissionLevel(2))
                // 启用
                .then(literal("enable").executes(ctx -> {
                    SweepingMod.CONFIG.enabled = true;
                    SweepingMod.CONFIG.save();
                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：哼！既然你求我，那我就继续当清洁工好了，要感恩戴德哦！"), true);
                    return 1;
                }))
                // 禁用
                .then(literal("disable").executes(ctx -> {
                    SweepingMod.CONFIG.enabled = false;
                    SweepingMod.CONFIG.save();
                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：切～关掉清扫？以后垃圾山堆起来可别哭着来找我！"), true);
                    return 1;
                }))
                // 清理间隔
                .then(literal("interval")
                        .then(argument("ticks", IntegerArgumentType.integer(20, 72000))
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    SweepingMod.CONFIG.intervalTicks = ticks;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：每 " + ticks + " ticks 扫一次，你可真会使唤人！"), true);
                                    return 1;
                                })))
                // 最小存活时间
                .then(literal("minage")
                        .then(argument("ticks", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    SweepingMod.CONFIG.minAgeTicks = ticks;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：东西要在地上躺 " + ticks + " ticks 才处理，哼，我可是很仁慈的！"), true);
                                    return 1;
                                })))
                // 玩家保护半径
                .then(literal("playerRadius")
                        .then(argument("blocks", IntegerArgumentType.integer(0, 256))
                                .executes(ctx -> {
                                    int r = IntegerArgumentType.getInteger(ctx, "blocks");
                                    SweepingMod.CONFIG.playerRadius = r;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：你周围 " + r + " 格的东西我都假装没看到，满意了吧？"), true);
                                    return 1;
                                })))
                // 预警时间
                .then(literal("warning")
                        .then(argument("seconds", IntegerArgumentType.integer(0, 3600))
                                .executes(ctx -> {
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    SweepingMod.CONFIG.warningSeconds = sec;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：提前 " + sec + " 秒通知你，再漏捡我可要嘲笑你了！"), true);
                                    return 1;
                                })))
                // 玩家忽略白名单
                .then(literal("playerIgnore")
                        .then(literal("add")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                            SweepingMod.CONFIG.ignoredPlayers.add(player.getUuid());
                                            SweepingMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：" + player.getName().getString() + " 这个懒虫也护着了，啧，特权阶级！"), true);
                                            return 1;
                                        })))
                        .then(literal("remove")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                            SweepingMod.CONFIG.ignoredPlayers.remove(player.getUuid());
                                            SweepingMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：" + player.getName().getString() + " 的保护取消，掉了东西被扫走可别怪我！"), true);
                                            return 1;
                                        })))
                        .then(literal("list").executes(ctx -> {
                            StringBuilder sb = new StringBuilder("§dJay1145酱：这些家伙的东西我会高抬贵手哒：\n");
                            for (UUID uuid : SweepingMod.CONFIG.ignoredPlayers) {
                                String name = ctx.getSource().getServer().getPlayerManager().getPlayer(uuid) != null ?
                                        ctx.getSource().getServer().getPlayerManager().getPlayer(uuid).getName().getString() : uuid.toString();
                                sb.append("§e - ").append(name).append("\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            return 1;
                        })))
                // 区块忽略
                .then(literal("chunkIgnore")
                        .then(literal("add")
                                .then(argument("chunkX", IntegerArgumentType.integer())
                                        .then(argument("chunkZ", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                                    int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                                    ServerWorld world = ctx.getSource().getWorld();
                                                    String dimId = world.getRegistryKey().getValue().toString();
                                                    String entry = dimId + ":" + cx + "," + cz;
                                                    SweepingMod.CONFIG.ignoredChunks.add(entry);
                                                    SweepingMod.CONFIG.save();
                                                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：区块 [" + cx + ", " + cz + "] 在 " + dimId + " 被标记跳过啦，里面的东西我不碰。"), true);
                                                    return 1;
                                                })))
                                .then(argument("dimension", DimensionArgumentType.dimension())
                                        .then(argument("chunkX", IntegerArgumentType.integer())
                                                .then(argument("chunkZ", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                                            int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                                            ServerWorld dim = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
                                                            String dimId = dim.getRegistryKey().getValue().toString();
                                                            String entry = dimId + ":" + cx + "," + cz;
                                                            SweepingMod.CONFIG.ignoredChunks.add(entry);
                                                            SweepingMod.CONFIG.save();
                                                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：区块 [" + cx + ", " + cz + "] 在 " + dimId + " 被标记跳过啦，里面的东西我不碰。"), true);
                                                            return 1;
                                                        })))))
                        .then(literal("remove")
                                .then(argument("chunkX", IntegerArgumentType.integer())
                                        .then(argument("chunkZ", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                                    int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                                    ServerWorld world = ctx.getSource().getWorld();
                                                    String dimId = world.getRegistryKey().getValue().toString();
                                                    String entry = dimId + ":" + cx + "," + cz;
                                                    SweepingMod.CONFIG.ignoredChunks.remove(entry);
                                                    SweepingMod.CONFIG.save();
                                                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：区块 [" + cx + ", " + cz + "] 的保护解除，以后也会清扫这里啦。"), true);
                                                    return 1;
                                                }))))
                        .then(literal("list").executes(ctx -> {
                            StringBuilder sb = new StringBuilder("§dJay1145酱：这些区块我可是特意避开的哦：\n");
                            for (String s : SweepingMod.CONFIG.ignoredChunks) {
                                sb.append("§e - ").append(s).append("\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            return 1;
                        })))
                // 区域保护
                .then(literal("regionProtect")
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("pos1", BlockPosArgumentType.blockPos())
                                                .then(argument("pos2", BlockPosArgumentType.blockPos())
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            BlockPos p1 = BlockPosArgumentType.getBlockPos(ctx, "pos1");
                                                            BlockPos p2 = BlockPosArgumentType.getBlockPos(ctx, "pos2");
                                                            ServerWorld world = ctx.getSource().getWorld();
                                                            String dimId = world.getRegistryKey().getValue().toString();
                                                            int minX = Math.min(p1.getX(), p2.getX());
                                                            int minY = Math.min(p1.getY(), p2.getY());
                                                            int minZ = Math.min(p1.getZ(), p2.getZ());
                                                            int maxX = Math.max(p1.getX(), p2.getX());
                                                            int maxY = Math.max(p1.getY(), p2.getY());
                                                            int maxZ = Math.max(p1.getZ(), p2.getZ());
                                                            SweepingConfig.RegionEntry re = new SweepingConfig.RegionEntry(name, dimId, minX, minY, minZ, maxX, maxY, maxZ);
                                                            SweepingMod.CONFIG.protectedRegions.add(re);
                                                            SweepingMod.CONFIG.save();
                                                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：保护区 '" + name + "' 已圈定，里面的宝贝我绝不动。"), true);
                                                            return 1;
                                                        })))))
                        .then(literal("remove")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean removed = SweepingMod.CONFIG.protectedRegions.removeIf(re -> re.name.equals(name));
                                            if (removed) {
                                                SweepingMod.CONFIG.save();
                                                ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：保护区 '" + name + "' 已删除，那些地方不再特殊了哦。"), true);
                                            } else {
                                                ctx.getSource().sendError(Text.literal("§dJay1145酱：诶？没找到叫 '" + name + "' 的保护区，你是不是记错了？"));
                                            }
                                            return removed ? 1 : 0;
                                        })))
                        .then(literal("list").executes(ctx -> {
                            StringBuilder sb = new StringBuilder("§dJay1145酱：当前保护区列表：\n");
                            for (SweepingConfig.RegionEntry re : SweepingMod.CONFIG.protectedRegions) {
                                sb.append("§e - ").append(re.name)
                                        .append(" [").append(re.dimension).append("] ")
                                        .append("(").append(re.minX).append(",").append(re.minY).append(",").append(re.minZ).append(") -> (")
                                        .append(re.maxX).append(",").append(re.maxY).append(",").append(re.maxZ).append(")\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            return 1;
                        })))
                // 生物清理命令（已修复 lambda 变量问题）
                .then(literal("entityClean")
                        .then(literal("add")
                                .then(argument("entityType", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String inputType = StringArgumentType.getString(ctx, "entityType");
                                            final String type = inputType.contains(":") ? inputType : "minecraft:" + inputType;
                                            if (SweepingMod.CONFIG.entitiesToClean.contains(type)) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：§e" + type + " 早就在清扫名单里了，你是不是记忆力只有三秒？"), false);
                                                return 0;
                                            }
                                            SweepingMod.CONFIG.entitiesToClean.add(type);
                                            SweepingMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：§a已把 " + type + " 加入黑名单，下次见面直接踹飞！"), true);
                                            return 1;
                                        })))
                        .then(literal("remove")
                                .then(argument("entityType", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String inputType = StringArgumentType.getString(ctx, "entityType");
                                            final String type = inputType.contains(":") ? inputType : "minecraft:" + inputType;
                                            if (!SweepingMod.CONFIG.entitiesToClean.contains(type)) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：§e" + type + " 本来就不在名单里，你眼花了？"), false);
                                                return 0;
                                            }
                                            SweepingMod.CONFIG.entitiesToClean.remove(type);
                                            SweepingMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：§a算 " + type + " 走运，暂时放过它了。"), true);
                                            return 1;
                                        })))
                        .then(literal("list").executes(ctx -> {
                            List<String> list = SweepingMod.CONFIG.entitiesToClean;
                            if (list.isEmpty()) {
                                ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：§e清扫名单是空的，你是想让我偷懒吗？"), false);
                            } else {
                                StringBuilder sb = new StringBuilder("§dJay1145酱：§a当前要清扫的小怪：\n");
                                for (String s : list) sb.append("§e - ").append(s).append("\n");
                                ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            }
                            return 1;
                        }))
                )
                // 状态查询
                .then(literal("status").executes(ctx -> {
                    SweepingConfig cfg = SweepingMod.CONFIG;
                    ctx.getSource().sendFeedback(() -> Text.literal(
                            "§dJay1145酱：哼～让我看看你的配置有多糟糕：\n" +
                            "§6===== 定时清扫 =====\n" +
                            "§e启用: " + (cfg.enabled ? "§a是" : "§c否") + " | 间隔: " + cfg.intervalTicks + " ticks (" + (cfg.intervalTicks/20) + "秒)\n" +
                            "§e最小存活: " + cfg.minAgeTicks + " ticks | 预警: " + cfg.warningSeconds + "秒\n" +
                            "§e玩家保护半径: " + cfg.playerRadius + "格 | 忽略玩家: " + cfg.ignoredPlayers.size() + "人\n" +
                            "§e忽略区块: " + cfg.ignoredChunks.size() + " | 保护区: " + cfg.protectedRegions.size() + "\n" +
                            "§6===== 强制清扫 =====\n" +
                            "§e启用: " + (cfg.forceCleanEnabled ? "§a是" : "§c否") + " | 物品阈值: " + cfg.forceCleanItemThreshold + " | 经验阈值: " + cfg.forceCleanXpThreshold + "\n" +
                            "§e强制无视年龄: " + (cfg.forceCleanIgnoreAge ? "§a是" : "§c否") + "\n" +
                            "§6===== 超远清理 =====\n" +
                            "§e启用: " + (cfg.distantCleanEnabled ? "§a是" : "§c否") + " | 距离阈值: " + cfg.distantCleanRadius + "格\n" +
                            "§6===== 生物清扫 =====\n" +
                            "§e启用: " + (cfg.entityCleanEnabled ? "§a是" : "§c否") + " | 名单: " + cfg.entitiesToClean.toString()
                    ), false);
                    return 1;
                }))
                // 重载配置
                .then(literal("reload").executes(ctx -> {
                    SweepingMod.CONFIG = SweepingConfig.load();
                    ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：配置重新读了一遍，你这笨蛋是不是又改错了？"), true);
                    return 1;
                }))
                // 手动立即清理
                .then(literal("clean")
                        .then(literal("now").executes(ctx -> {
                            ServerWorld world = ctx.getSource().getWorld();
                            SweepScheduler.cleanNow(world);
                            ctx.getSource().sendFeedback(() -> Text.literal("§dJay1145酱：手动清理完成啦！脏活累活都丢给我，真会使唤人！"), true);
                            return 1;
                        })))
        );
    }
}
