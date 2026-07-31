package com.example.sweeping;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SweepingMod implements ModInitializer {
    public static final String MOD_ID = "sweeping";
    public static SweepingConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = SweepingConfig.load();
        CommandRegistrationCallback.EVENT.register(SweepCommand::register);
        ServerTickEvents.END_SERVER_TICK.register(SweepScheduler::onServerTick);
    }
}
