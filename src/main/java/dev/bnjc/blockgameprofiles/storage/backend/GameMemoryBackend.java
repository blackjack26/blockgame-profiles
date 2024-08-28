package dev.bnjc.blockgameprofiles.storage.backend;

import dev.bnjc.blockgameprofiles.storage.BlockgameData;
import org.jetbrains.annotations.Nullable;

public class GameMemoryBackend implements Backend {
  @Nullable
  private BlockgameData data = null;

  @Override
  public @Nullable BlockgameData load() {
    return data;
  }

  @Override
  public void delete() {
    data = null;
  }

  @Override
  public boolean save(BlockgameData data) {
    this.data = data;
    return true;
  }
}
