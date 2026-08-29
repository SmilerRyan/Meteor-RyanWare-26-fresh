package smilerryan.ryanware.modules_standard;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import com.example.addon.AddonTemplate;

import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ForceColoredChat extends Module {
    private final Pattern colorCodePattern = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    public ForceColoredChat() {
        super(AddonTemplate.CATEGORY, "RyanWare-Force-Colored-Chat", "Replaces & color codes with § color codes in received messages everywhere.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent e) {
        Text original = e.getMessage();
        String content = original.getString();
        if (colorCodePattern.matcher(content).find()) {
            e.setMessage(replaceColorCodes(original));
        }
    }

    private Text replaceColorCodes(Text text) {
        MutableText result = Text.empty().setStyle(text.getStyle());
        text.visit((style, string) -> {
            String replaced = colorCodePattern.matcher(string).replaceAll("§$1");
            result.append(Text.literal(replaced).setStyle(style));
            return java.util.Optional.empty();
        }, text.getStyle());
        return result;
    }
}