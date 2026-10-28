package com.psipheron.nudge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(), 
            "nudge_config.json"
    );

    public enum AimMode {
        LOG,    // Smooth logarithmic tracking
        NUDGE   // Aggressive logarithmic tracking with high initial jerk/snap
    }

    public AimMode aimMode = AimMode.LOG; 
    public double maxRange = 4.5;
    public double fovThreshold = 35.0;
    public double strength = 0.15;
    public double logBase = 2.0;
    public double nudgeJerkPower = 1.3; // Higher values make Nudge Mode snap onto targets much faster from a distance

    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static ModConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    INSTANCE = config;
                    return config;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        INSTANCE = config;
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
