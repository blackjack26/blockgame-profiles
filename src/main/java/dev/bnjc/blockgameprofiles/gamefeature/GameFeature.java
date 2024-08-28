package dev.bnjc.blockgameprofiles.gamefeature;

import dev.bnjc.blockgameprofiles.client.BlockgameProfilesClient;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

@Getter
public abstract class GameFeature {
  private MinecraftClient minecraftClient;
  private BlockgameProfilesClient blockgameClient;

  public void init(MinecraftClient minecraftClient, BlockgameProfilesClient blockgameClient) {
    this.minecraftClient = minecraftClient;
    this.blockgameClient = blockgameClient;
  }

  public void tick(MinecraftClient client) {
  }

  public boolean isEnabled() {
    return true;
  }
}
