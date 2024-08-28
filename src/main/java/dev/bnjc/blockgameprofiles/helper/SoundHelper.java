package dev.bnjc.blockgameprofiles.helper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvents;

public class SoundHelper {
  public static void playDownSound() {
    SoundHelper.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
  }

  public static void playItemPickupSound() {
    SoundHelper.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F));
  }

  public static void playVillagerNoSound() {
    SoundHelper.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_VILLAGER_NO, 1.0F));
  }

  private static SoundManager getSoundManager() {
    return MinecraftClient.getInstance().getSoundManager();
  }
}
