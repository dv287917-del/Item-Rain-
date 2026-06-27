package com.itemrain;

import com.itemrain.command.ItemRainCommand;
import com.itemrain.config.ItemRainConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItemRainMod implements ModInitializer {

    public static final String  MOD_ID = "itemrain";
    public static final Logger  LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Null when rain is inactive; non-null while rain is running. */
    private static volatile ItemRainState activeState = null;

    @Override
    public void onInitialize() {
        LOGGER.info("[ItemRain] Initialising – MC 1.21.1 / Fabric");

        // Force disabled on startup regardless of what the JSON says
        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.enabled = false;
        cfg.save();

        CommandRegistrationCallback.EVENT.register(ItemRainCommand::register);
        ItemRainTickHandler.register();

        LOGGER.info("[ItemRain] Ready. /itemrain start to begin.");
    }

    // ── Session API ───────────────────────────────────────────────────────────

    public static void startRain(ServerPlayerEntity player, ItemStack item) {
        activeState = new ItemRainState(player, item);
        LOGGER.info("[ItemRain] Rain started: {} for player {}",
                item.getItem(), player.getName().getString());
    }

    public static void stopRain() {
        if (activeState != null) {
            LOGGER.info("[ItemRain] Rain stopped.");
            activeState = null;
        }
        ItemRainConfig cfg = ItemRainConfig.get();
        cfg.enabled = false;
        cfg.save();
    }

    /** Returns the active {@link ItemRainState}, or {@code null} if rain is off. */
    public static ItemRainState getState() {
        return activeState;
    }
}
