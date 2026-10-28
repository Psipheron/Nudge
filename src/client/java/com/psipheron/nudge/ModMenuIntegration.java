package com.psipheron.nudge;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Nudge Settings"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

            // Dropdown selector to choose between Logarithmic Tracking and Nudge Mode
            general.addEntry(entryBuilder.startEnumSelector(Component.literal("Aim Assist Mode"), ModConfig.AimMode.class, config.aimMode)
                    .setDefaultValue(ModConfig.AimMode.LOG)
                    .setTooltip(Component.literal("LOG = Smooth tracking | NUDGE = Snappy/Aggressive logarithmic tracking"))
                    .setSaveConsumer(val -> config.aimMode = val)
                    .build());

            general.addEntry(entryBuilder.startDoubleField(Component.literal("Max Range"), config.maxRange)
                    .setDefaultValue(4.5)
                    .setTooltip(Component.literal("Detection range limit in blocks"))
                    .setSaveConsumer(val -> config.maxRange = val)
                    .build());

            general.addEntry(entryBuilder.startDoubleField(Component.literal("FOV Threshold"), config.fovThreshold)
                    .setDefaultValue(35.0)
                    .setMin(5.0)
                    .setMax(180.0)
                    .setTooltip(Component.literal("Degrees offset allowed for target locking"))
                    .setSaveConsumer(val -> config.fovThreshold = val)
                    .build());

            general.addEntry(entryBuilder.startDoubleField(Component.literal("Nudge Strength"), config.strength)
                    .setDefaultValue(0.15)
                    .setMin(0.01)
                    .setMax(1.0)
                    .setTooltip(Component.literal("Multiplier for logarithmic camera pull"))
                    .setSaveConsumer(val -> config.strength = val)
                    .build());

            // Tuning slider specifically targeting your new aggressive Nudge Mode snap mechanics
            general.addEntry(entryBuilder.startDoubleField(Component.literal("Nudge Jerk Power"), config.nudgeJerkPower)
                    .setDefaultValue(1.3)
                    .setMin(1.0)
                    .setMax(3.0)
                    .setTooltip(Component.literal("Only affects NUDGE mode. Higher values increase initial crosshair snapping velocity."))
                    .setSaveConsumer(val -> config.nudgeJerkPower = val)
                    .build());

            builder.setSavingRunnable(config::save);
            return builder.build();
        };
    }
}
