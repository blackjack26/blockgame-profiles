package dev.bnjc.blockgameprofiles.gamefeature.statprofiles;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.client.BlockgameProfilesClient;
import dev.bnjc.blockgameprofiles.event.screen.ScreenOpenedEvent;
import dev.bnjc.blockgameprofiles.event.screen.ScreenReceivedInventoryEvent;
import dev.bnjc.blockgameprofiles.gamefeature.GameFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.ActionResult;

public class StatProfileGameFeature extends GameFeature {
  private final StatScreenManager screenManager;

  public StatProfileGameFeature() {
    this.screenManager = new StatScreenManager(this);
  }

  @Override
  public void init(MinecraftClient minecraftClient, BlockgameProfilesClient blockgameClient) {
    super.init(minecraftClient, blockgameClient);

    ScreenOpenedEvent.EVENT.register(this::handleScreenOpened);
    ScreenReceivedInventoryEvent.EVENT.register(this::handleScreenReceivedInventory);
  }

  private ActionResult handleScreenOpened(OpenScreenS2CPacket packet) {
    if (!BlockgameProfiles.getConfig().getGeneralConfig().enabled) {
      return ActionResult.PASS;
    }

    // Check if the screen is a 9x5 screen
    if (packet.getScreenHandlerType() != ScreenHandlerType.GENERIC_9X5) {
      return ActionResult.PASS;
    }

    // Check if the screen name is "Talents"
    if (!packet.getName().getString().equals("Talents")) {
      return ActionResult.PASS;
    }

    // Create or re-create the screen
    this.screenManager.openScreen(packet);

    return ActionResult.CONSUME;
  }

  private ActionResult handleScreenReceivedInventory(InventoryS2CPacket packet) {
    if (!BlockgameProfiles.getConfig().getGeneralConfig().enabled) {
      return ActionResult.PASS;
    }

    int expectedSyncId = this.screenManager.getSyncId();
    if (expectedSyncId == -1 || packet.getSyncId() != expectedSyncId) {
      return ActionResult.PASS;
    }

    this.screenManager.receiveInventory(packet);
    return ActionResult.CONSUME;
  }
}
