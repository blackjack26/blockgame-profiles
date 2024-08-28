package dev.bnjc.blockgameprofiles.storage;

import com.mojang.serialization.Codec;
import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate.StatProfile;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Getter
public class BlockgameData {
  public static final Codec<Map<String, StatProfile>> STAT_PROFILES_CODEC = Codec
      .unboundedMap(Codec.STRING, StatProfile.CODEC)
      .xmap(HashMap::new, Function.identity());

  @Nullable
  public static BlockgameData INSTANCE = null;

  private final Map<String, StatProfile> statProfiles;

  public BlockgameData() {
    this.statProfiles = new HashMap<>();
  }

  public static void loadOrCreate() {
    BlockgameData.unload();
    BlockgameData.INSTANCE = Storage.load().orElseGet(BlockgameData::new);
    BlockgameData.save();
  }

  public static void save() {
    if (BlockgameData.INSTANCE == null) {
      return;
    }

    Storage.save(BlockgameData.INSTANCE);
    BlockgameProfiles.LOGGER.debug("Blockgame data saved");
  }

  public static void unload() {
    if (BlockgameData.INSTANCE == null) {
      return;
    }

    Storage.save(BlockgameData.INSTANCE);
    BlockgameData.INSTANCE = null;
    BlockgameProfiles.LOGGER.debug("Blockgame data unloaded");
  }

  public static void saveProfile(StatProfile profile) {
    if (BlockgameData.INSTANCE == null) {
      return;
    }

    BlockgameProfiles.LOGGER.debug("Saving profile: \"{}\"", profile.getName());
    BlockgameData.INSTANCE.statProfiles.put(profile.getName(), profile);
    BlockgameData.save();
  }

  public static void removeProfile(String name) {
    if (BlockgameData.INSTANCE == null) {
      return;
    }

    BlockgameProfiles.LOGGER.debug("Removing profile: \"{}\"", name);
    BlockgameData.INSTANCE.statProfiles.remove(name);
    BlockgameData.save();
  }
}
