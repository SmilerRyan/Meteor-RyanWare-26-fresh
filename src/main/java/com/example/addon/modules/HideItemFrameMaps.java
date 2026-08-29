package com.example.addon.modules;

import java.util.HashSet;
import java.util.Set;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import com.example.addon.AddonTemplate;


public class HideItemFrameMaps extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final Set<Integer> allowed = new HashSet<>();

    public HideItemFrameMaps() {
        super(
            AddonTemplate.CATEGORY,
            "RyanWare-Hide-Item-Frame-Maps",
            "Hides maps from item frames until you choose to unblock them."
        );
    }

    // TOGGLE NOW HANDLED BY ATTACK INSTEAD OF INTERACT EVENT
    @EventHandler
    private void onAttack(meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent event) {
        if (mc.player == null) return;
        if (!(event.entity instanceof ItemFrame frame)) return;

        // SHIFT = normal vanilla behavior (do nothing)
        if (mc.player.isSneaking()) return;

        ItemStack stack = frame.getItem();
        Integer id = extractMapId(stack);
        if (id == null) return;

        if (allowed.contains(id)) {
            allowed.remove(id);
        } else {
            allowed.add(id);
        }

        event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;
        mc.level.getEntitiesOfClass(ItemFrame.class, mc.player.getBoundingBox().expand(64), e -> true)
            .forEach(this::updateFrame);
    }

    private void updateFrame(ItemFrame frame) {
        ItemStack stack = frame.getItem();
        Integer id = extractMapId(stack);
        if (id == null) return;

        if (stack.getItem() == Items.FILLED_MAP) {
            if (allowed.contains(id)) return;

            ItemStack barrier = Items.BARRIER.getDefaultStack();
            barrier.set(DataComponents.CUSTOM_NAME, Component.literal("§4" + id));
            frame.setItem(barrier);
        }

        if (stack.getItem() == Items.BARRIER) {
            if (!allowed.contains(id)) return;

            ItemStack map = Items.FILLED_MAP.getDefaultStack();
            map.set(DataComponents.MAP_ID, new MapId(id));
            frame.setItem(map);
        }
    }

    private Integer extractMapId(ItemStack stack) {
        if (stack.getItem() == Items.FILLED_MAP) {
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId != null) return mapId.id();
        }

        if (stack.getItem() == Items.BARRIER) {
            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName == null) return null;
            String stripped = customName.getString()
                .replaceAll("§4", "")
                .trim();

            try {
                return Integer.parseInt(stripped);
            } catch (Exception ignored) {}
        }

        return null;
    }
}