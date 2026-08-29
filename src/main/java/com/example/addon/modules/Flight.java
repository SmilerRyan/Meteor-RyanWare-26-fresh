package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import com.example.addon.AddonTemplate;

public class Flight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgBoat = settings.createGroup("Boat Fly");

    private final Setting<Boolean> speedEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("speed-enabled")
        .description("Allows you to go a custom fly speed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("How fast to fly.")
        .defaultValue(8.0)
        .min(0.1)
        .max(100.0)
        .sliderMax(10.0)
        .visible(speedEnabled::get)
        .build()
    );

    private final Setting<Boolean> instantTakeoff = sgGeneral.add(new BoolSetting.Builder()
        .name("instant-takeoff")
        .description("Take off immediately when jumping, without requiring double jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enableEveryTick = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-flight-every-tick")
        .description("Bypasses some anti-fly by re-enabling flight every tick.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> bypassAntiKick = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass-anti-kick")
        .description("Bypasses vanilla anti-kick while flying.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> antiSlowdown = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-slowdown")
        .description("Periodically forces zero movement to bypass slowdown effects.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> slowdownInterval = sgGeneral.add(new IntSetting.Builder()
        .name("anti-slowdown-interval")
        .description("Ticks between anti-slowdown bursts.")
        .defaultValue(20)
        .min(1)
        .max(200)
        .sliderMax(100)
        .visible(antiSlowdown::get)
        .build()
    );

    private final Setting<Integer> slowdownDuration = sgGeneral.add(new IntSetting.Builder()
        .name("anti-slowdown-duration")
        .description("How many ticks movement is forced to zero.")
        .defaultValue(2)
        .min(1)
        .max(20)
        .sliderMax(10)
        .visible(antiSlowdown::get)
        .build()
    );

    private final Setting<Double> slowdownSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("anti-slowdown-speed")
        .description("How fast to fly during the anti-slowdown time")
        .defaultValue(1.0)
        .min(0.0)
        .max(100.0)
        .sliderMax(10.0)
        .visible(antiSlowdown::get)
        .build()
    );

    private final Setting<Boolean> elytraBlockSlowdown = sgSafety.add(new BoolSetting.Builder()
        .name("elytra-block-slowdown")
        .description("Slows Elytra movement when very close to blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> elytraSlowdownSpeed = sgSafety.add(new DoubleSetting.Builder()
        .name("elytra-slow-speed")
        .description("Maximum Elytra speed when close to a block.")
        .defaultValue(1.0)
        .min(0.05)
        .max(10.0)
        .sliderMax(5.0)
        .visible(elytraBlockSlowdown::get)
        .build()
    );

    private final Setting<Boolean> boatFly = sgBoat.add(new BoolSetting.Builder()
        .name("enable")
        .description("Allows boats to fly while you are riding them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> boatFlySpeed = sgBoat.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Boat Fly movement speed.")
        .defaultValue(1.5)
        .min(0.05)
        .max(10.0)
        .sliderMax(5.0)
        .visible(boatFly::get)
        .build()
    );

    private final Minecraft mc = Minecraft.getInstance();

    private boolean wasJumping = false;
    private boolean isFlying = false;

    private int jumpTimer = 0;
    private static final int DOUBLE_TAP_WINDOW = 7;

    private int slowdownTick = 0;
    private int slowdownActive = 0;

    public Flight() {
        super(
            AddonTemplate.CATEGORY,
            "RyanWare-Flight",
            "Toggle flying with double jump, like creative mode."
        );
    }

    @Override
    public void onActivate() {
        wasJumping = false;
        isFlying = false;
        jumpTimer = 0;

        slowdownTick = 0;
        slowdownActive = 0;

        if (mc.player != null) {
            mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().flying = false;
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        boolean isJumping = mc.options.keyJump.isDown();

        // Instant takeoff mode
        if (instantTakeoff.get()) {
            if (!isFlying && isJumping && !mc.player.onGround()) {
                isFlying = true;
                mc.player.getAbilities().mayfly = true;
                mc.player.getAbilities().flying = true;
            }
        }

        // Double-tap detection
        if (!wasJumping && isJumping) {
            if (jumpTimer > 0) {
                isFlying = !isFlying;

                mc.player.getAbilities().mayfly = isFlying;
                mc.player.getAbilities().flying = isFlying;

                jumpTimer = 0;
            } else {
                jumpTimer = DOUBLE_TAP_WINDOW;
            }
        }

        // Countdown timer
        if (jumpTimer > 0) jumpTimer--;

        // Reset on ground
        if (mc.player.onGround() && isFlying && instantTakeoff.get()) {
            isFlying = false;
            mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().flying = false;
        }

        // Apply allowFlying every tick
        if (enableEveryTick.get() && isFlying) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
        }

        // Normal Flight speed
        if (isFlying && speedEnabled.get()) {
            mc.player.getAbilities().setFlyingSpeed(
                (float) (speed.get() * 0.05f)
            );
        }

        /*
         * Elytra block safety.
         *
         * Do NOT modify flyingSpeed here.
         * flyingSpeed belongs to Creative-style flight, while Elytra
         * movement is controlled by the player's velocity.
         */
        if (elytraBlockSlowdown.get()
            && mc.player.isFallFlying()
            && mc.level != null) {

            if (isNearSolidBlock()) {
                double maxSpeed = elytraSlowdownSpeed.get();

                var velocity = mc.player.getDeltaMovement();
                double horizontalSpeed = Math.sqrt(
                    velocity.x * velocity.x +
                    velocity.z * velocity.z
                );

                if (horizontalSpeed > maxSpeed && horizontalSpeed > 0.0) {
                    double multiplier = maxSpeed / horizontalSpeed;

                    mc.player.setDeltaMovement(
                        velocity.x * multiplier,
                        velocity.y,
                        velocity.z * multiplier
                    );
                }
            }
        }

        /*
         * Simple Boat Fly.
         *
         * This only changes the boat's velocity. It does not modify
         * player flight abilities or flying speed.
         */
        if (boatFly.get()) {
            handleBoatFly();
        }

        // Bypass Vanilla Anti-kick
        if (isFlying
            && !mc.player.onGround()
            && bypassAntiKick.get()
            && mc.player.tickCount % 10 < 2) {

            mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x,
                mc.player.getDeltaMovement().y
                    + (mc.player.tickCount % 10 == 0 ? -0.04 : 0.04),
                mc.player.getDeltaMovement().z
            );
        }

        // Anti-slowdown
        if (antiSlowdown.get() && isFlying) {
            if (slowdownActive > 0) {
                mc.player.getAbilities().setFlyingSpeed(
                    (float) (slowdownSpeed.get() * 0.05f)
                );

                slowdownActive--;
            } else {
                slowdownTick++;

                if (slowdownTick >= slowdownInterval.get()) {
                    slowdownTick = 0;
                    slowdownActive = slowdownDuration.get();
                }
            }
        }

        wasJumping = isJumping;
    }

    private boolean isNearSolidBlock() {
        var pos = mc.player.blockPosition();

        /*
         * Only check the immediately adjacent blocks.
         * This avoids slowing the Elytra simply because there is
         * terrain somewhere nearby.
         */
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    if (mc.level.getBlockState(
                        pos.offset(dx, dy, dz)
                    ).isSolid()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

private void handleBoatFly() {
    // Boat Fly is disabled: do absolutely nothing.
    if (!boatFly.get()) return;

    // Player not available.
    if (mc.player == null) return;

    // Not riding anything.
    if (mc.player.getVehicle() == null) return;

    var vehicle = mc.player.getVehicle();

    // Only continue for boats.
    if (vehicle == null) return;

    if (!vehicle.getType().toString().toLowerCase().contains("boat")) {
        return;
    }

    double speedValue = boatFlySpeed.get();

    // W/S movement.
    double forward = 0.0;

    if (mc.options.keyUp.isDown()) {
        forward += 1.0;
    }

    if (mc.options.keyDown.isDown()) {
        forward -= 1.0;
    }

    // Use the player's facing direction.
    double yaw = Math.toRadians(mc.player.getYRot());

    double moveX = -Math.sin(yaw) * forward;
    double moveZ = Math.cos(yaw) * forward;

    // Hover by default.
    double vertical = 0.0;

    // Space = up.
    if (mc.options.keyJump.isDown()) {
        vertical = speedValue;
    }

    // Configured sneak key = down.
    if (mc.options.keyShift.isDown()) {
        vertical = -speedValue;
    }

    vehicle.setDeltaMovement(
        moveX * speedValue,
        vertical,
        moveZ * speedValue
    );
}

}