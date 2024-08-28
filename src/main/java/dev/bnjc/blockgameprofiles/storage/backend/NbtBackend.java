package dev.bnjc.blockgameprofiles.storage.backend;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate.StatProfile;
import dev.bnjc.blockgameprofiles.helper.FileHelper;
import dev.bnjc.blockgameprofiles.storage.BlockgameData;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NbtBackend extends FileBasedBackend {
  @Override
  public @Nullable BlockgameData load() {
    BlockgameData data = new BlockgameData();
    data.getStatProfiles().putAll(this.loadStatProfiles());
    return data;
  }

  @Override
  public boolean save(BlockgameData data) {
    boolean result = FileHelper.saveToNbt(data.getStatProfiles(), BlockgameData.STAT_PROFILES_CODEC, FileHelper.getBlockgamePath().resolve(STAT_PROFILES_NAME + extension()));

    // Save other data here. Example:
    //  result &= FileHelper.saveToNbt(data.getEntries(), BlockgameData.ENTRIES_CODEC, FileHelper.getBlockgamePath().resolve(ENTRIES_NAME + extension()));

    return result;
  }

  private Map<String, StatProfile> loadStatProfiles() {
    Path statProfilesPath = FileHelper.getBlockgamePath().resolve(STAT_PROFILES_NAME + extension());
    var result = FileHelper.loadFromNbt(BlockgameData.STAT_PROFILES_CODEC, statProfilesPath);
    if (result.isPresent()) {
      return result.get();
    }

    BlockgameProfiles.LOGGER.warn("Failed to load stat profiles from {}", statProfilesPath);
    return new HashMap<>();
  }

  @Override
  public String extension() {
    return ".nbt";
  }
}
