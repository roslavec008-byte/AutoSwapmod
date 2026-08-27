package com.autoswap.mixin;

import com.autoswap.AutoSwapHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void autoswap_addButton(CallbackInfo ci) {
		HandledScreen<?> self = ((HandledScreen<?>) (Object) this);
		if (!(self instanceof InventoryScreen)) {
			return;
		}
		int x = self.width - 200;
		int y = self.height / 2 - 100;
		((ScreenInvoker) (Object) this).invokeAddDrawableChild(ButtonWidget.builder(
				Text.literal("Выбрать предмет"),
				button -> AutoSwapHandler.enableSelection()
		).dimensions(x, y, 140, 20).build());
	}

	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
			at = @At("HEAD"), cancellable = true)
	private void autoswap_onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		if (!AutoSwapHandler.isSelecting()) {
			return;
		}
		if (button != 0 || slot == null || slot.getStack().isEmpty()) {
			return;
		}
		AutoSwapHandler.selectFromSlot(slot);
		AutoSwapHandler.setSelecting(false);
		MinecraftClient.getInstance().setScreen(null);
		ci.cancel();
	}
}
