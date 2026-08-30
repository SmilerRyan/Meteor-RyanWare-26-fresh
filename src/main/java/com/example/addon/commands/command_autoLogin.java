package com.example.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class command_autoLogin extends Command {
    private static final File ryanwareDir = new File(MeteorClient.FOLDER, "ryanware");
    private static final File f = new File(ryanwareDir, "autologin.txt");

    public command_autoLogin() {
        super("autologin", "Auto login per server and account.");
    }

    @EventHandler
    private void onMsg(ReceiveMessageEvent event) {
        if (!event.getMessage().getString().toLowerCase().contains("login")) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.getCurrentServer() == null) return;

        String key = mc.getCurrentServer().ip + "|" + mc.getUser().getName() + "|";

        for (String line : load()) {
            if (line.startsWith(key)) {
                String password = line.substring(key.length());

                if (!password.isEmpty()) {
                    ChatUtils.sendPlayerMsg("/login " + password);
                }

                break;
            }
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            argument("password/off/open", StringArgumentType.greedyString())
                .executes(context -> {
                    String argument = StringArgumentType.getString(
                        context,
                        "password/off/open"
                    );

                    if (argument.equalsIgnoreCase("open")) {
                        File parent = f.getParentFile();

                        if (parent != null) {
                            parent.mkdirs();
                        }

                        try {
                            if (!f.exists()) {
                                f.createNewFile();
                            }

                            Util.getPlatform().openFile(f);
                        } catch (IOException e) {
                            error("Failed to open auto-login file.");
                        }

                        return SINGLE_SUCCESS;
                    }

                    Minecraft mc = Minecraft.getInstance();

                    if (mc.player == null || mc.getCurrentServer() == null) {
                        error("You must be connected to a server.");
                        return SINGLE_SUCCESS;
                    }

                    String server = mc.getCurrentServer().ip;
                    String username = mc.getUser().getName();
                    String key = server + "|" + username + "|";

                    List<String> lines = load();

                    lines.removeIf(s -> s.startsWith(key));

                    if (!argument.equalsIgnoreCase("off")) {
                        lines.add(key + argument);
                    }

                    File parent = f.getParentFile();

                    if (parent != null) {
                        parent.mkdirs();
                    }

                    try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(f)
                    )) {
                        for (String line : lines) {
                            writer.write(line);
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        error("Failed to save auto-login data.");
                        return SINGLE_SUCCESS;
                    }

                    info(
                        (argument.equalsIgnoreCase("off") ? "Cleared" : "Saved")
                            + " password for " + username
                            + " at " + server + "."
                    );

                    return SINGLE_SUCCESS;
                })
        );
    }

    public static List<String> load() {
        List<String> lines = new ArrayList<>();

        if (!f.exists()) return lines;

        try (BufferedReader reader = new BufferedReader(
            new FileReader(f)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ignored) {
        }

        return lines;
    }
}