package dev.bnjc.blockgameprofiles.storage.backend;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.helper.FileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class FileBasedBackend implements Backend {
  public static final String STAT_PROFILES_NAME = "stat_profiles";

  @Override
  public void delete() {
    getRelevantPaths().forEach(path -> {
      if (Files.isRegularFile(path)) {
        try {
          Files.delete(path);
          BlockgameProfiles.LOGGER.debug("Deleted file {}", path);
        } catch (IOException e) {
          BlockgameProfiles.LOGGER.error("Failed to delete file {}", path, e);
        }
      }
    });
  }

  public abstract String extension();

  protected List<Path> getRelevantPaths() {
    return List.of(
        FileHelper.getBlockgamePath().resolve(STAT_PROFILES_NAME + extension())
    );
  }
}
