package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import com.example.addon.AddonTemplate;
import net.minecraft.client.KeyMapping;


public class AutoWalkForwards extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoJump = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-jump")
        .description("Automatically holds the jump key.")
        .defaultValue(false)
        .build()
    );

    public AutoWalkForwards() {
        super(AddonTemplate.CATEGORY, "RyanWare-Auto-Walk-Forwards", "Automatically presses keyUp for you.");
    }

    @Override
    public void onDeactivate() {
        if (mc.options == null) return;
        KeyMapping.set(mc.options.keyUp.getKey(), false);
        if (autoJump.get()) KeyMapping.set(mc.options.keyJump.getKey(), false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.options == null) return;
        KeyMapping.set(mc.options.keyUp.getKey(), true);
        if (autoJump.get()) {
            KeyMapping.set(mc.options.keyJump.getKey(), !mc.options.keyShift.isDown());
        }
    }

}
