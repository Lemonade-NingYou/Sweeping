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
import net.minecraft.world.World;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;

public class SweepCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                RegistrationEnvironment environment) {
        dispatcher.register(literal("sweeping")
                .requires(src -> src.hasPermissionLevel(2))
                .then(literal("enable").executes(ctx -> {
                    SweepingMod.CONFIG.enabled = true;
                    SweepingMod.CONFIG.save();
                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：哼，既然你这么求我，那就开启清理吧..."), true);
                    return 1;
                }))
                .then(literal("disable").executes(ctx -> {
                    SweepingMod.CONFIG.enabled = false;
                    SweepingMod.CONFIG.save();
                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：诶？关掉的话，以后满地垃圾可别来找我！"), true);
                    return 1;
                }))
                .then(literal("interval")
                        .then(argument("ticks", IntegerArgumentType.integer(20, 72000))
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    SweepingMod.CONFIG.intervalTicks = ticks;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：知道了知道了，每隔 " + ticks + " ticks 打扫一次，行了吧？"), true);
                                    return 1;
                                })))
                .then(literal("minage")
                        .then(argument("ticks", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    SweepingMod.CONFIG.minAgeTicks = ticks;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：物品至少存在 " + ticks + " ticks 才会被清理哦，新鲜掉落才不会误删呢。"), true);
                                    return 1;
                                })))
                .then(literal("playerRadius")
                        .then(argument("blocks", IntegerArgumentType.integer(0, 256))
                                .executes(ctx -> {
                                    int r = IntegerArgumentType.getInteger(ctx, "blocks");
                                    SweepingMod.CONFIG.playerRadius = r;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：保护半径设成 " + r + " 格，靠近你的东西我都假装看不见。"), true);
                                    return 1;
                                })))
                .then(literal("warning")
                        .then(argument("seconds", IntegerArgumentType.integer(0, 3600))
                                .executes(ctx -> {
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    SweepingMod.CONFIG.warningSeconds = sec;
                                    SweepingMod.CONFIG.save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：提前 " + sec + " 秒预警，够你捡起来的吧？"), true);
                                    return 1;
                                })))
                .then(literal("playerIgnore")
                        .then(literal("add")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                            SweepingMod.CONFIG.ignoredPlayers.add(player.getUuid());
                                            SweepingMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：" + player.getName().getString() + " 被加入白名单了，哼，特殊照顾呢。"), true);
                                            return 1;
                                        })))
                        .then(literal("remove")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                            SweepingMod.CONFIG.ignoredPlayers.remove(player.getUuid());
                                            SweepingMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：" + player.getName().getString() + " 被移出白名单了，掉宝可别哭鼻子。"), true);
                                            return 1;
                                        })))
                        .then(literal("list").executes(ctx -> {
                            StringBuilder sb = new StringBuilder("Jay1145酱：哼，这些家伙的东西我才不碰呢：\n");
                            for (UUID uuid : SweepingMod.CONFIG.ignoredPlayers) {
                                String name = ctx.getSource().getServer().getPlayerManager().getPlayer(uuid) != null ?
                                        ctx.getSource().getServer().getPlayerManager().getPlayer(uuid).getName().getString() : uuid.toString();
                                sb.append("§e - ").append(name).append("\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            return 1;
                        })))
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
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：区块 [" + cx + ", " + cz + "] 在维度 " + dimId + " 已标记，里面的东西我绕开走。"), true);
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
                                                            ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：区块 [" + cx + ", " + cz + "] 在维度 " + dimId + " 已标记，里面的东西我绕开走。"), true);
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
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：区块 [" + cx + ", " + cz + "] 保护解除，现在可以被清扫啦。"), true);
                                                    return 1;
                                                }))))
                        .then(literal("list").executes(ctx -> {
                            StringBuilder sb = new StringBuilder("Jay1145酱：这些区块我可是特意避开的哦：\n");
                            for (String s : SweepingMod.CONFIG.ignoredChunks) {
                                sb.append("§e - ").append(s).append("\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            return 1;
                        })))
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
                                                            ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：保护区 '" + name + "' 已圈定，里面的宝贝我绝不动。"), true);
                                                            return 1;
                                                        })))))
                        .then(literal("remove")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            boolean removed = SweepingMod.CONFIG.protectedRegions.removeIf(re -> re.name.equals(name));
                                            if (removed) {
                                                SweepingMod.CONFIG.save();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：保护区 '" + name + "' 已删除，那些地方不再特殊了哦。"), true);
                                            } else {
                                                ctx.getSource().sendError(Text.literal("Jay1145酱：诶？没找到叫 '" + name + "' 的保护区，你是不是记错了？"));
                                            }
                                            return removed ? 1 : 0;
                                        })))
                        .then(literal("list").executes(ctx -> {
                            StringBuilder sb = new StringBuilder("Jay1145酱：当前保护区列表：\n");
                            for (SweepingConfig.RegionEntry re : SweepingMod.CONFIG.protectedRegions) {
                                sb.append("§e - ").append(re.name)
                                        .append(" [").append(re.dimension).append("] ")
                                        .append("(").append(re.minX).append(",").append(re.minY).append(",").append(re.minZ).append(") -> (")
                                        .append(re.maxX).append(",").append(re.maxY).append(",").append(re.maxZ).append(")\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                            return 1;
                        })))
                .then(literal("status").executes(ctx -> {
                    SweepingConfig cfg = SweepingMod.CONFIG;
                    ctx.getSource().sendFeedback(() -> Text.literal(
                            "Jay1145酱：哼，看在你求我的份上，给你看看状态：\n" +
                                    "§6===== Sweeping 状态 =====\n" +
                                    "§e启用: " + (cfg.enabled ? "§a是" : "§c否") + "\n" +
                                    "§e清理间隔: " + cfg.intervalTicks + " ticks (" + (cfg.intervalTicks / 20) + " 秒)\n" +
                                    "§e最小存活时间: " + cfg.minAgeTicks + " ticks (" + (cfg.minAgeTicks / 20) + " 秒)\n" +
                                    "§e玩家忽略半径: " + cfg.playerRadius + " 格\n" +
                                    "§e预警时间: " + cfg.warningSeconds + " 秒\n" +
                                    "§e忽略玩家数: " + cfg.ignoredPlayers.size() + "\n" +
                                    "§e忽略区块数: " + cfg.ignoredChunks.size() + "\n" +
                                    "§e保护区数: " + cfg.protectedRegions.size()
                    ), false);
                    return 1;
                }))
                .then(literal("reload").executes(ctx -> {
                    SweepingMod.CONFIG = SweepingConfig.load();
                    ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：配置已重新加载，不要老是改来改去啦！"), true);
                    return 1;
                }))
                .then(literal("clean")
                        .then(literal("now").executes(ctx -> {
                            ServerWorld world = ctx.getSource().getWorld();
                            SweepScheduler.cleanNow(world);
                            ctx.getSource().sendFeedback(() -> Text.literal("Jay1145酱：手动清理已执行，去看看成果吧！"), true);
                            return 1;
                        })))
        );
    }
}
