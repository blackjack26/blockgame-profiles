package dev.bnjc.blockgameprofiles.storage;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.storage.backend.Backend;
import lombok.Setter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Storage {
  @Setter
  private static Backend backend;

  public static void setup() {
    BlockgameProfiles.getConfig().getStorageConfig().backendType.load();

    // Load data on join
    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      client.execute(BlockgameData::loadOrCreate);
    });

    // Unload data on disconnect
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      BlockgameData.unload();
    });

    // Save data on pause
    ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
      if (screen instanceof GameMenuScreen) {
        BlockgameData.save();
      }
    });
  }

  public static Optional<BlockgameData> load() {
    if (BlockgameData.INSTANCE != null) {
      return Optional.of(BlockgameData.INSTANCE);
    }

    BlockgameData data = backend.load();
    if (data == null) {
      return Optional.empty();
    }

    return Optional.of(data);
  }

  public static void save(@Nullable BlockgameData data) {
    if (data == null) {
      BlockgameProfiles.LOGGER.warn("BlockgameData is null, not saving");
      return;
    }

    boolean saved = backend.save(data);
    if (!saved) {
      BlockgameProfiles.LOGGER.error("Failed to save BlockgameData");
    }
  }
}
