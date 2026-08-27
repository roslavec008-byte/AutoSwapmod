package com.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class AutoSwapClient implements ClientModInitializer {
	private static KeyBinding swapKey;       // F - общий свап
	private static KeyBinding elytraKey;     // G - нагрудник <-> элитры
	private static KeyBinding windKey;       // X - воздушный заряд

	@Override
	public void onInitializeClient() {
		swapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoswap.swap",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_F,
				KeyBinding.Category.MISC
		));
		elytraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoswap.elytra",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				KeyBinding.Category.MISC
		));
		windKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoswap.wind",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_X,
				KeyBinding.Category.MISC
		));

		UseItemCallback.EVENT.register((player, world, hand) -> {
			AutoSwapHandler.scheduleWindReturn();
			return ActionResult.PASS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}
			while (swapKey.wasPressed()) {
				AutoSwapHandler.handleSwap(client);
			}
			while (elytraKey.wasPressed()) {
				AutoSwapHandler.handleElytraSwap(client);
			}
			while (windKey.wasPressed()) {
				AutoSwapHandler.handleWindSwap(client);
			}
			AutoSwapHandler.tickWindReturn(client);
			AutoSwapHandler.tickMace(client);
		});
	}
}
