package com.itemrain.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ItemRainConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("itemrain.json");

    private static ItemRainConfig instance;

    // ── Persisted fields ────────────────────────────────────────────────────
    public boolean enabled = false;
    public int     speed   = 2;
    public int     amount  = 10;
    public int     radius  = 1000;
    public boolean global  = false;

    // ── Singleton ────────────────────────────────────────────────────────────
    private ItemRainConfig() {}

    public static ItemRainConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    // ── I/O ─────────────────────────────────────────────────────────────────
    private static ItemRainConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader r = new InputStreamReader(
                    Files.newInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                ItemRainConfig cfg = GSON.fromJson(r, ItemRainConfig.class);
                if (cfg != null) {
                    // Reset enabled to false on every server start for safety
                    cfg.enabled = false;
                    return cfg;
                }
            } catch (Exception e) {
                System.err.println("[ItemRain] Failed to load config, using defaults: " + e.getMessage());
            }
        }
        ItemRainConfig cfg = new ItemRainConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer w = new OutputStreamWriter(
                    Files.newOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            System.err.println("[ItemRain] Failed to save config: " + e.getMessage());
        }
    }
}
