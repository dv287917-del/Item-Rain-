package com.itemrain;

import com.itemrain.config.ItemRainConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRainTickHandler {

    /** Items fall from this Y level under normal gravity. */
    private static final int SPAWN_Y = 320;

    /** Overworld dimension – items only rain here to avoid nether/end weirdness. */
    private static final RegistryKey<World> OVERWORLD = World.OVERWORLD;

    /** Action-bar refresh interval in ticks (every second). */
    private static final int ACTION_BAR_INTERVAL = 20;

    private static int actionBarTick = 0;

    private static final Random RANDOM = new Random();

    private ItemRainTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ItemRainTickHandler::onServerTick);
    }

    // ── Main tick ─────────────────────────────────────────────────────────────

    private static void onServerTick(MinecraftServer server) {
        ItemRainState state = ItemRainMod.getState();
        if (state == null) return;

        ItemRainConfig cfg = ItemRainConfig.get();
        if (!cfg.enabled) return;

        ServerPlayerEntity player = state.getSourcePlayer();
        if (player == null || !player.isAlive() || player.isRemoved()) {
            ItemRainMod.stopRain();
            return;
        }

        // Track item changes in real time
        ItemStack held = player.getMainHandStack();
        if (!held.isEmpty() && !ItemStack.areItemsEqual(held, state.getRainItem())) {
            state.setRainItem(held);
        }

        // Action-bar update (every 20 ticks)
        actionBarTick++;
        if (actionBarTick >= ACTION_BAR_INTERVAL) {
            actionBarTick = 0;
            sendActionBar(player, state, cfg);
        }

        // Spawn batch check
        if (!state.shouldSpawnThisTick(cfg.speed)) return;

        ItemStack item = state.getRainItem();
        if (item.isEmpty()) return;

        ServerWorld world = server.getWorld(OVERWORLD);
        if (world == null) world = (ServerWorld) player.getWorld();

        if (cfg.global) {
            spawnGlobal(world, item, cfg.amount);
        } else {
            spawnAroundPlayer(world, player, item, cfg.amount, cfg.radius);
        }
    }

    // ── Spawn: around player ──────────────────────────────────────────────────

    private static void spawnAroundPlayer(ServerWorld world,
                                          ServerPlayerEntity player,
                                          ItemStack item,
                                          int amount,
                                          int radius) {
        double cx = player.getX();
        double cz = player.getZ();

        for (int i = 0; i < amount; i++) {
            double angle = RANDOM.nextDouble() * 2.0 * Math.PI;
            double dist  = Math.sqrt(RANDOM.nextDouble()) * radius; // uniform disk distribution
            double x = cx + Math.cos(angle) * dist;
            double z = cz + Math.sin(angle) * dist;
            spawnOne(world, item, x, z);
        }
    }

    // ── Spawn: all loaded chunks ───────────────────────────────────────────────

    private static void spawnGlobal(ServerWorld world, ItemStack item, int amount) {
        // Snapshot loaded chunk positions without holding a reference to the stream
        List<ChunkPos> positions = new ArrayList<>();
        world.getChunkManager().getLoadedChunks().forEach(chunk -> {
            positions.add(chunk.getPos());
        });

        if (positions.isEmpty()) return;

        for (int i = 0; i < amount; i++) {
            ChunkPos cp = positions.get(RANDOM.nextInt(positions.size()));
            // Pick a random block inside the 16×16 chunk
            double x = (cp.x << 4) + RANDOM.nextDouble() * 16.0;
            double z = (cp.z << 4) + RANDOM.nextDouble() * 16.0;
            spawnOne(world, item, x, z);
        }
    }

    // ── Single item entity spawn ───────────────────────────────────────────────

    private static void spawnOne(ServerWorld world, ItemStack template, double x, double z) {
        ItemStack stack = template.copy();
        stack.setCount(1);

        ItemEntity entity = new ItemEntity(world, x, SPAWN_Y, z, stack);

        // Small horizontal nudge – pure gravity does the vertical work from Y=320
        entity.setVelocity(
            (RANDOM.nextDouble() - 0.5) * 0.05,
            0.0,
            (RANDOM.nextDouble() - 0.5) * 0.05
        );
        entity.setPickupDelay(20);  // 1 second before pickup to avoid instant-collect

        world.spawnEntity(entity);
    }

    // ── Action bar ────────────────────────────────────────────────────────────

    private static void sendActionBar(ServerPlayerEntity player,
                                      ItemRainState state,
                                      ItemRainConfig cfg) {
        String itemName = state.getRainItem().isEmpty()
                ? "None"
                : state.getRainItem().getName().getString();

        Text bar = Text.literal(
            "§a§lItem Rain §r§8| " +
            "§7Item: §f" + itemName + " §8| " +
            "§7Amount: §f" + cfg.amount + " §8| " +
            "§7Radius: §f" + cfg.radius + " §8| " +
            "§7Speed: §f" + cfg.speed + "t §8| " +
            "§7Global: " + (cfg.global ? "§aON" : "§cOFF")
        );
        player.sendMessage(bar, true);
    }
}
