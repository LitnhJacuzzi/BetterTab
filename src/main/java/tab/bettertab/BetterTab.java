package tab.bettertab;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tab.bettertab.config.ConfigScreen;
import tab.bettertab.mixin.PlayerListHudAccess;
import tab.bettertab.mixin.PlayerListHudMixin;

public class BetterTab implements ModInitializer {
	public static final String MOD_ID = "better-tab";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static double tabScroll = 0;

	public static boolean useExamples = false;
	public static boolean renderColumnNumbers = true;

	public static final KeyBinding openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			Text.translatable("tab.better.tab.keybind.open_config").getString(),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT,
			Text.translatable("tab.better.tab.keybind.title").getString()
	));
	public static final KeyBinding rightScroll = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			Text.translatable("tab.better.tab.keybind.scroll_right").getString(),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT,
			Text.translatable("tab.better.tab.keybind.title").getString()
	));
	public static final KeyBinding leftScroll = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			Text.translatable("tab.better.tab.keybind.scroll_left").getString(),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT,
			Text.translatable("tab.better.tab.keybind.title").getString()
	));
	public static final KeyBinding useExamplesBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"Use Examples",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_KP_3,
			"BetterTab"
	));

	@Override
	public void onInitialize() {
		new ConfigSystem().checkConfig();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (((PlayerListHudAccess)client.inGameHud.getPlayerListHud()).getVisible()) {
				if (openConfig.wasPressed()) {
					MinecraftClient.getInstance().setScreen(new ConfigScreen(null));
				}
				if (rightScroll.wasPressed()) {
					tabScroll ++;
				}
				if (leftScroll.wasPressed()) {
					tabScroll --;
				}
				if (useExamplesBind.wasPressed()) {
					useExamples = !useExamples;
				}
			}
		});
	}

	public int parseColor(String colorString) {
		try {
			if (colorString.startsWith("#")) {
				colorString = colorString.substring(1);
			}

			int alpha = 255;
			if (colorString.length() == 8) {
				alpha = Integer.parseInt(colorString.substring(6, 8), 16);
				colorString = colorString.substring(0, 6);
			}

			int red = Integer.parseInt(colorString.substring(0, 2), 16);
			int green = Integer.parseInt(colorString.substring(2, 4), 16);
			int blue = Integer.parseInt(colorString.substring(4, 6), 16);

			return (alpha << 24) | (red << 16) | (green << 8) | blue;
		} catch (Exception e) {
			return 0;
		}
	}
}