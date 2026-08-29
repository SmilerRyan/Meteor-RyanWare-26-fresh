package com.example.addon.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import com.example.addon.AddonTemplate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.regex.Pattern;

public class ForceColoredChat extends Module {
    private final Pattern colorCodePattern = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    public ForceColoredChat() {
        super(AddonTemplate.CATEGORY, "RyanWare-Force-Colored-Chat", "Replaces & color codes with § color codes in received messages everywhere.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent e) {
        Component original = e.getMessage();
        String content = original.getString();
        if (colorCodePattern.matcher(content).find()) {
            e.setMessage(replaceColorCodes(original));
        }
    }

    private Component replaceColorCodes(Text text) {
        MutableComponent result = Component.empty().setStyle(text.getStyle());
        text.visit((style, string) -> {
            String replaced = colorCodePattern.matcher(string).replaceAll("§$1");
            result.append(Component.literal(replaced).setStyle(style));
            return java.util.Optional.empty();
        }, text.getStyle());
        return result;
    }
}