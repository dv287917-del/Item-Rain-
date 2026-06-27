package com.itemrain;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Holds mutable runtime state for the active item-rain session.
 * Lives only in memory – recreated each time rain is started.
 */
public final class ItemRainState {

    private final ServerPlayerEntity sourcePlayer;
    private ItemStack rainItem;
    private int tickAccumulator = 0;

    public ItemRainState(ServerPlayerEntity player, ItemStack item) {
        this.sourcePlayer = player;
        this.rainItem     = item.copy();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public ServerPlayerEntity getSourcePlayer() {
        return sourcePlayer;
    }

    public ItemStack getRainItem() {
        return rainItem;
    }

    public void setRainItem(ItemStack stack) {
        this.rainItem = stack.copy();
    }

    // ── Tick counter ─────────────────────────────────────────────────────────

    /** Increments the accumulator and returns true if a spawn batch is due. */
    public boolean shouldSpawnThisTick(int speed) {
        tickAccumulator++;
        if (tickAccumulator >= speed) {
            tickAccumulator = 0;
            return true;
        }
        return false;
    }
}
