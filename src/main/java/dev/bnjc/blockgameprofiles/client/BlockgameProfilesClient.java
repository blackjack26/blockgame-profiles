package dev.bnjc.blockgameprofiles.client;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.gamefeature.GameFeature;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.StatProfileGameFeature;
import dev.bnjc.blockgameprofiles.storage.Storage;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class BlockgameProfilesClient implements ClientModInitializer {
  @Getter
  private static final ArrayList<GameFeature> loadedGameFeatures = new ArrayList<>();

  @Getter
  @Setter
  private static int errors;

  @Getter
  private static final int maxErrorsBeforeCrash = 5;

  @Override
  public void onInitializeClient() {
    loadGameFeatures();

    Storage.setup();
  }

  private void loadGameFeatures() {
    BlockgameProfiles.LOGGER.info("[Blockgame Profiles] Loading game features...");

    loadGameFeature(new StatProfileGameFeature());

    // Tick all game features after client ticks
    ClientTickEvents.END_CLIENT_TICK.register((client) -> {
      client.getProfiler().push("tickGameFeatures");

      for (GameFeature gameFeature : loadedGameFeatures) {
        client.getProfiler().push(gameFeature.getClass().getSimpleName());

        // Try to tick and don't crash if it fails
        try {
          gameFeature.tick(client);
        } catch (Exception e) {
          // Crash if there's been too many errors
          if(errors > maxErrorsBeforeCrash) {
            throw e;
          }

          ClientPlayerEntity player = client.player;
          if(player != null) {
            player.sendMessage(Text.of("§4§l=== PLEASE REPORT THIS AS A BUG ==="), false);
            player.sendMessage(Text.of(String.format("§cAn error occurred in %s!", gameFeature.getClass().getSimpleName())), false);
            player.sendMessage(Text.of(e.getClass().getName() + ": §7" + e.getMessage()), false);
            player.sendMessage(Text.of("§4§l================================="), false);
            errors++;
          }
        }

        client.getProfiler().pop();
      }

      client.getProfiler().pop();
    });
  }

  /**
   * Loads a specific game feature.
   * @param gameFeature Instance of the game feature to load
   */
  private void loadGameFeature(GameFeature gameFeature) {
    if(!gameFeature.isEnabled()) {
      BlockgameProfiles.LOGGER.info("Skipping load of {} because it's disabled", gameFeature.getClass().getSimpleName().replace("GameFeature", " game feature"));
      return;
    }

    String featureName = gameFeature.getClass().getSimpleName().replace("GameFeature", " game feature");
    BlockgameProfiles.LOGGER.info("Loading {}", featureName);

    try {
      gameFeature.init(MinecraftClient.getInstance(), this);
      loadedGameFeatures.add(gameFeature);
    }
    catch (Exception e) {
      BlockgameProfiles.LOGGER.error("Failed to load {} game feature: {}", featureName, e.getMessage());
    }
  }
}
