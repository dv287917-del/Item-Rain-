package com.itemrain.command;

import com.itemrain.ItemRainMod;
import com.itemrain.config.ItemRainConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ItemRainCommand {

    private ItemRainCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("itemrain")
                .requires(src -> src.hasPermissionLevel(2))

                .then(CommandManager.literal("start")
                    .executes(ctx -> start(ctx.getSource())))

                .then(CommandManager.literal("stop")
                    .executes(ctx -> stop(ctx.getSource())))

                .then(CommandManager.literal("toggle")
                    .executes(ctx -> toggle(ctx.getSource())))

                .then(CommandManager.literal("speed")
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 200))
                        .executes(ctx -> {
                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                            return setSpeed(ctx.getSource(), ticks);
                        })))

                .then(CommandManager.literal("amount")
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 500))
                        .executes(ctx -> {
                            int count = IntegerArgumentType.getInteger(ctx, "count");
                            return setAmount(ctx.getSource(), count);
                        })))

                .then(CommandManager.literal("radius")
                    .then(CommandManager.argument("blocks", IntegerArgumentType.integer(1, 30000))
                        .executes(ctx -> {
                            int blocks = IntegerArgumentType.getInteger(ctx, "blocks");
                            return setRadius(ctx.getSource(), blocks);
                        })))

                .then(CommandManager.literal("global")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            return setGlobal(ctx.getSource(), value);
                        })))
        );
    }

    // ── start ────────────────────────────────────────────────────────────────

    private static int start(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            src.sendError(Text.literal("[ItemRain] Only players can start the rain."));
            return 0;
        }

        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            src.sendError(Text.literal("[ItemRain] Hold an item in your main hand first."));
            return 0;
        }

        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.enabled = true;
        cfg.save();

        ItemRainMod.startRain(player, held);

        src.sendFeedback(() -> Text.literal(
            "§a[ItemRain] Started raining §e" + held.getName().getString() +
            "§a. Amount: §f" + cfg.amount +
            " §a| Speed: §f" + cfg.speed + "t" +
            " §a| Radius: §f" + cfg.radius +
            " §a| Global: §f" + cfg.global
        ), true);
        return 1;
    }

    // ── stop ─────────────────────────────────────────────────────────────────

    private static int stop(ServerCommandSource src) {
        ItemRainMod.stopRain();
        src.sendFeedback(() -> Text.literal("§c[ItemRain] Rain stopped."), true);
        return 1;
    }

    // ── toggle ───────────────────────────────────────────────────────────────

    private static int toggle(ServerCommandSource src) {
        if (ItemRainConfig.get().enabled) {
            return stop(src);
        } else {
            return start(src);
        }
    }

    // ── speed ────────────────────────────────────────────────────────────────

    private static int setSpeed(ServerCommandSource src, int ticks) {
        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.speed = ticks;
        cfg.save();
        src.sendFeedback(() -> Text.literal(
            "§a[ItemRain] Speed set to §f" + ticks + "§a ticks between batches."
        ), false);
        return 1;
    }

    // ── amount ───────────────────────────────────────────────────────────────

    private static int setAmount(ServerCommandSource src, int count) {
        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.amount = count;
        cfg.save();
        src.sendFeedback(() -> Text.literal(
            "§a[ItemRain] Amount set to §f" + count + "§a items per batch."
        ), false);
        return 1;
    }

    // ── radius ───────────────────────────────────────────────────────────────

    private static int setRadius(ServerCommandSource src, int blocks) {
        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.radius = blocks;
        cfg.save();
        src.sendFeedback(() -> Text.literal(
            "§a[ItemRain] Radius set to §f" + blocks + "§a blocks."
        ), false);
        return 1;
    }

    // ── global ───────────────────────────────────────────────────────────────

    private static int setGlobal(ServerCommandSource src, boolean value) {
        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.global = value;
        cfg.save();
        src.sendFeedback(() -> Text.literal(
            "§a[ItemRain] Global mode " + (value ? "§aENABLED§a (all loaded chunks)." : "§cDISABLED§a (around player).")
        ), false);
        return 1;
    }
}
