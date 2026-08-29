package com.example.addon.modules;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import com.example.addon.AddonTemplate;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerList extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDoubleHalf = settings.createGroup("Double / Half Ping");
    private final SettingGroup sgSimilarPing = settings.createGroup("Similar Ping");
    private final SettingGroup sgOthers = settings.createGroup("Everyone else");

    public enum SortMode {
        Name,
        PingLowToHigh,
        PingHighToLow
    }

    public enum HighlightPriority {
        SimilarPingFirst,
        DoubleHalfFirst
    }

    // --- General Settings ---
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("How to sort the players in the list.")
        .defaultValue(SortMode.Name)
        .build()
    );

    private final Setting<HighlightPriority> highlightPriority = sgGeneral.add(new EnumSetting.Builder<HighlightPriority>()
        .name("highlight-priority")
        .description("Which highlight takes priority when a player matches multiple rules.")
        .defaultValue(HighlightPriority.DoubleHalfFirst)
        .build()
    );

    private final Setting<Boolean> hideNormalPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-normal-players")
        .description("Only show players matching one of the enabled highlight rules.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> format = sgGeneral.add(new StringSetting.Builder()
        .name("format")
        .description("The layout of the line. Use {name} and {ping_pad} and {ping_raw} as placeholders.")
        .defaultValue("{name}")
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale factor of the text.")
        .defaultValue(0.67)
        .min(0.1)
        .max(10.0)
        .sliderMax(10.0)
        .build()
    );
    
    // --- Double / Half Ping Settings ---
    private final Setting<Boolean> doubleHalfEnable = sgDoubleHalf.add(new BoolSetting.Builder()
        .name("enable")
        .description("Color players whose ping is roughly double or half of another player's ping.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> doubleHalfAmount = sgDoubleHalf.add(new IntSetting.Builder()
        .name("tolerance")
        .description("Allowed difference from an exact double/half relationship (0 is exact).")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<SettingColor> doubleHalfColor = sgDoubleHalf.add(new ColorSetting.Builder()
        .name("color")
        .description("The color for players with double/half pings of others.")
        .defaultValue(new SettingColor(255, 128, 0, 255)) // Default orange
        .build()
    );

    
    // --- Similar Ping Settings ---
    private final Setting<Boolean> similarPingEnable = sgSimilarPing.add(new BoolSetting.Builder()
        .name("enable")
        .description("Color players with similar pings.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> similarPingAmount = sgSimilarPing.add(new IntSetting.Builder()
        .name("amount")
        .description("Maximum ping difference to be considered similar.")
        .defaultValue(0)
        .min(0)
        .sliderMax(500)
        .build()
    );

    private final Setting<SettingColor> similarPingColor = sgSimilarPing.add(new ColorSetting.Builder()
        .name("color")
        .description("The color for players with similar pings.")
        .defaultValue(new SettingColor(255, 128, 0, 255))
        .build()
    );

    // --- Everyone else Settings ---
    private final Setting<SettingColor> textColor = sgOthers.add(new ColorSetting.Builder()
        .name("text-color")
        .description("The color for normal players.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );


    private List<PlayerInfo> sortedPlayers;
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 160); // Semi-transparent black background

    public PlayerList() {
        super(AddonTemplate.CATEGORY, "RyanWare-Player-List", "Custom player list, sortable by name, ping and detect similar or double/half pinging player patterns.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getConnection() == null) return;
        ClientPacketListener networkHandler = mc.getConnection();
        
        Comparator<PlayerInfo> comparator;
        switch (sortMode.get()) {
            case Name:
                comparator = Comparator.comparing(entry -> entry.getProfile().name(), String.CASE_INSENSITIVE_ORDER);
                break;
            case PingHighToLow:
                comparator = Comparator.comparingInt(PlayerInfo::getLatency).reversed();
                break;
            case PingLowToHigh:
            default:
                comparator = Comparator.comparingInt(PlayerInfo::getLatency);
                break;
        }

        sortedPlayers = networkHandler.getOnlinePlayers().stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (sortedPlayers == null || sortedPlayers.isEmpty()) return;
        
        double x = 10;
        double y = 20;
        
        double scaleValue = scale.get();
        
        // Calculate dynamic line height based on font metrics and scale
        int lineHeight = (int) (mc.font.lineHeight * scaleValue);
        
        // Find highest ping to determine layout padding dynamically
        int maxPing = 0;
        for (PlayerInfo entry : sortedPlayers) {
            if (entry.getLatency() > maxPing) {
                maxPing = entry.getLatency();
            }
        }
        int maxPingLength = String.valueOf(maxPing).length();

        // Track players with similar pings
        Set<PlayerInfo> similarPlayers = new HashSet<>();
        if (similarPingEnable.get()) {
            int amount = similarPingAmount.get();
            int size = sortedPlayers.size();
            for (int i = 0; i < size; i++) {
                PlayerInfo p1 = sortedPlayers.get(i);
                for (int j = i + 1; j < size; j++) {
                    PlayerInfo p2 = sortedPlayers.get(j);
                    if (Math.abs(p1.getLatency() - p2.getLatency()) <= amount) {
                        similarPlayers.add(p1);
                        similarPlayers.add(p2);
                    }
                }
            }
        }

        // Track players with double or half pings
        Set<PlayerInfo> doubleHalfPlayers = new HashSet<>();
        if (doubleHalfEnable.get()) {
            int tolerance = doubleHalfAmount.get();
            int size = sortedPlayers.size();
            for (int i = 0; i < size; i++) {
                PlayerInfo p1 = sortedPlayers.get(i);
                int lat1 = p1.getLatency();
                for (int j = i + 1; j < size; j++) {
                    PlayerInfo p2 = sortedPlayers.get(j);
                    int lat2 = p2.getLatency();

                    // Avoid matches where both players have 0 ping
                    if (lat1 == 0 && lat2 == 0) continue;

                    // Checks: Is lat1 roughly double lat2? OR is lat2 roughly double lat1?
                    if (Math.abs(lat1 - 2 * lat2) <= tolerance || Math.abs(lat2 - 2 * lat1) <= tolerance) {
                        doubleHalfPlayers.add(p1);
                        doubleHalfPlayers.add(p2);
                    }
                }
            }
        }
        
        // Calculate dimensions for the background dynamically using formatted text   
        double maxWidth = 0;
        int visiblePlayers = 0;
        for (PlayerInfo entry : sortedPlayers) {
            boolean matches = similarPlayers.contains(entry) || doubleHalfPlayers.contains(entry);
            if (hideNormalPlayers.get() && !matches) continue;
            String line = formatEntry(entry, maxPingLength);
            double width = mc.font.width(line) * scaleValue;
            if (width > maxWidth) maxWidth = width;
            visiblePlayers++;
        }
        if (visiblePlayers == 0) return;

        // Draw background using Meteor's 2D renderer.
        event.renderer.quad(
            x - padding,
            y - padding,
            backgroundWidth,
            backgroundHeight,
            BACKGROUND_COLOR
        );

        // Draw each player entry. Renderer2D text is already screen-space;
        // apply the configured scale to coordinates and text size by using
        // the scaled positions and keeping line spacing consistent.
        for (PlayerInfo entry : sortedPlayers) {
            boolean matches = similarPlayers.contains(entry) || doubleHalfPlayers.contains(entry);
            if (hideNormalPlayers.get() && !matches) continue;

            String line = formatEntry(entry, maxPingLength);
            Color colorToUse = textColor.get();

            switch (highlightPriority.get()) {
                case SimilarPingFirst:
                    if (similarPlayers.contains(entry)) colorToUse = similarPingColor.get();
                    else if (doubleHalfPlayers.contains(entry)) colorToUse = doubleHalfColor.get();
                    break;
                case DoubleHalfFirst:
                default:
                    if (doubleHalfPlayers.contains(entry)) colorToUse = doubleHalfColor.get();
                    else if (similarPlayers.contains(entry)) colorToUse = similarPingColor.get();
                    break;
            }

            event.renderer.text(line, x, y, colorToUse, true, scaleValue);
            y += lineHeight;
        }
    }

    private String formatEntry(PlayerInfo entry, int maxPingLength) {
        String name = entry.getProfile().name();
        int ping = entry.getLatency();
        
        // Always left pads with zeros up to maxPingLength (e.g. "005" or "120")
        String paddedPing = String.format("%0" + maxPingLength + "d", ping);
        
        // Replace custom formatting tags dynamically
        return format.get()
            .replace("{name}", name)
            .replace("{ping_pad}", paddedPing)
            .replace("{ping_raw}", Integer.toString(ping));
    }
}