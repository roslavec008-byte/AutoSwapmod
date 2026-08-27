package com.autoswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class AutoSwapHandler {
	// Общий свап (клавиша F): offhand <-> выбранный слот
	private static int sourceSlot = -1;
	private static boolean swapped = false;
	private static boolean selecting = false;

	// Elytra (клавиша G): нагрудник <-> элитры
	private static boolean elytraSwapped = false;
	private static int elytraChestSlot = -1;

	// Wind charge (клавиша X)
	private static boolean windActive = false;
	private static int windSourceSlot = -1;
	private static ItemStack windPrevMain = ItemStack.EMPTY;
	private static boolean windReturnNextTick = false;

	// Mace (авто)
	private static boolean maceHeld = false;
	private static ItemStack macePrevMain = ItemStack.EMPTY;
	private static double fallAccum = 0.0;
	private static double prevY = Double.NaN;

	// ---------- Общий свап (F) ----------

	public static void enableSelection() {
		selecting = true;
		sendMessage("АвтоСвап: режим выбора включён. Наведи на предмет и нажми ЛКМ.");
	}

	public static boolean isSelecting() {
		return selecting;
	}

	public static void setSelecting(boolean value) {
		selecting = value;
	}

	public static void selectFromSlot(Slot slot) {
		sourceSlot = slot.getIndex();
		swapped = false;
		sendMessage("АвтоСвап: выбран предмет \"" + slot.getStack().getName().getString() + "\". Жми F для свапа.");
	}

	public static void handleSwap(MinecraftClient client) {
		if (client.player == null) return;
		PlayerInventory inv = client.player.getInventory();

		if (sourceSlot == -1) {
			sendMessage("АвтоСвап: открой инвентарь, нажми кнопку \"Выбрать предмет\" и кликни ЛКМ по предмету.");
			return;
		}

		if (!swapped) {
			ItemStack source = inv.getStack(sourceSlot);
			if (source.isEmpty()) {
				sendMessage("АвтоСвап: выбранный слот пуст. Выбери предмет заново.");
				sourceSlot = -1;
				return;
			}
			ItemStack offhand = client.player.getOffHandStack().copy();
			inv.setStack(PlayerInventory.OFF_HAND_SLOT, source.copy());
			inv.setStack(sourceSlot, offhand);
			swapped = true;
			sendMessage("АвтоСвап: в offhand установлен " + source.getName().getString());
		} else {
			ItemStack offhand = client.player.getOffHandStack();
			ItemStack target = inv.getStack(sourceSlot);
			inv.setStack(PlayerInventory.OFF_HAND_SLOT, target.copy());
			inv.setStack(sourceSlot, offhand);
			swapped = false;
			sendMessage("АвтоСвап: offhand возвращён в исходное состояние");
		}
	}

	// ---------- Elytra (G) ----------

	public static void handleElytraSwap(MinecraftClient client) {
		if (client.player == null) return;
		PlayerInventory inv = client.player.getInventory();
		ItemStack currentChest = inv.getStack(38); // слот нагрудника

		if (!elytraSwapped) {
			int elytraSlot = findItemSlot(inv, Registries.ITEM.get(Identifier.ofVanilla("elytra")));
			if (elytraSlot == -1 || elytraSlot == 38) {
				sendMessage("АвтоСвап: элитры не найдены в инвентаре.");
				return;
			}
			ItemStack elytra = inv.getStack(elytraSlot).copy();
			inv.setStack(38, elytra);
			inv.setStack(elytraSlot, currentChest);
			elytraSwapped = true;
			elytraChestSlot = elytraSlot;
			sendMessage("АвтоСвап: нагрудник заменён на элитры. Жми G чтобы вернуть.");
		} else {
			ItemStack chest = inv.getStack(38);
			int slot = elytraChestSlot != -1 ? elytraChestSlot : findItemSlot(inv, Registries.ITEM.get(Identifier.ofVanilla("elytra")));
			if (slot == -1 || slot == 38) return;
			inv.setStack(slot, chest.copy());
			inv.setStack(38, inv.getStack(38).copy());
			elytraSwapped = false;
			elytraChestSlot = -1;
			sendMessage("АвтоСвап: элитры возвращены, нагрудник на месте.");
		}
	}

	// ---------- Wind charge (X) ----------

	public static void handleWindSwap(MinecraftClient client) {
		if (client.player == null) return;
		PlayerInventory inv = client.player.getInventory();

		if (!windActive) {
			int slot = findWindCharge(inv);
			if (slot == -1) {
				sendMessage("АвтоСвап: воздушный заряд не найден в инвентаре.");
				return;
			}
			windPrevMain = client.player.getMainHandStack().copy();
			windSourceSlot = slot;
			client.player.setStackInHand(Hand.MAIN_HAND, inv.getStack(slot).copy());
			inv.setStack(slot, windPrevMain.copy());
			windActive = true;
			sendMessage("АвтоСвап: воздушный заряд в руке. Кидай ПКМ, потом вернётся.");
		} else {
			restoreWind(client);
		}
	}

	public static void scheduleWindReturn() {
		if (windActive) {
			windReturnNextTick = true;
		}
	}

	public static void tickWindReturn(MinecraftClient client) {
		if (!windActive) return;
		if (windReturnNextTick) {
			windReturnNextTick = false;
			restoreWind(client);
			return;
		}
		ItemStack main = client.player.getMainHandStack();
		if (main.isEmpty() || !main.isOf(Items.WIND_CHARGE)) {
			restoreWind(client);
		}
	}

	private static void restoreWind(MinecraftClient client) {
		if (!windActive) return;
		client.player.setStackInHand(Hand.MAIN_HAND, windPrevMain.copy());
		windActive = false;
		windSourceSlot = -1;
		windPrevMain = ItemStack.EMPTY;
		sendMessage("АвтоСвап: воздушный заряд убран, предмет вернулся.");
	}

	// ---------- Mace (авто) ----------

	public static void tickMace(MinecraftClient client) {
		if (client.player == null) return;
		boolean onGround = client.player.isOnGround();
		double curY = client.player.getY();
		if (prevY == Double.NaN) {
			prevY = curY;
		}

		if (!onGround && curY < prevY) {
			fallAccum += (prevY - curY);
		}

		if (!maceHeld && fallAccum > 3.0) {
			PlayerInventory inv = client.player.getInventory();
			int slot = findItemSlot(inv, Registries.ITEM.get(Identifier.ofVanilla("mace")));
			if (slot != -1) {
				macePrevMain = client.player.getMainHandStack().copy();
				ItemStack mace = inv.getStack(slot);
				if (macePrevMain.isEmpty()) {
					inv.setStack(slot, ItemStack.EMPTY);
				} else {
					inv.setStack(slot, macePrevMain.copy());
				}
				client.player.setStackInHand(Hand.MAIN_HAND, mace.copy());
				maceHeld = true;
				sendMessage("АвтоСвап: булава в руке (падение >3 блоков)!");
			}
		}

		if (onGround && maceHeld) {
			client.player.setStackInHand(Hand.MAIN_HAND, macePrevMain.copy());
			maceHeld = false;
			macePrevMain = ItemStack.EMPTY;
		}

		if (onGround) {
			fallAccum = 0.0;
		}
		prevY = curY;
	}

	// ---------- helpers ----------

	private static int findItemSlot(PlayerInventory inv, Item item) {
		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			if (!s.isEmpty() && s.isOf(item)) return i;
		}
		return -1;
	}

	private static int findWindCharge(PlayerInventory inv) {
		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			if (!s.isEmpty() && s.isOf(Items.WIND_CHARGE)) return i;
		}
		return -1;
	}

	public static void sendMessage(String msg) {
		if (MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().player.sendMessage(
					net.minecraft.text.Text.literal(msg).formatted(Formatting.AQUA), false);
		}
	}
}
