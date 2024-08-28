package dev.bnjc.blockgameprofiles.helper;

import com.mojang.serialization.Codec;
import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import lombok.SneakyThrows;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class FileHelper {
  private static final String BLOCKGAME_FOLDER_NAME = "blockgame";
  private static final Path GAME_FOLDER_PATH = Path.of(FabricLoader.getInstance().getConfigDir() + "/" + BLOCKGAME_FOLDER_NAME);

  @SneakyThrows
  public static Path getBlockgamePath() {
    // Create the game folder directory in case it does not exist
    if(Files.notExists(GAME_FOLDER_PATH)) {
      Files.createDirectory(GAME_FOLDER_PATH);
    }

    return Path.of(GAME_FOLDER_PATH + "/");
  }

  public static <T> boolean saveToNbt(T object, Codec<T> codec, Path path) {
    try {
      Files.createDirectories(path.getParent());
      var tag = codec.encodeStart(NbtOps.INSTANCE, object).get();
      var result = tag.left();
      var err = tag.right();
      if (err.isPresent()) {
        throw new IOException("Error encoding to NBT %s".formatted(err.get()));
      }

      if (result.isPresent() && result.get() instanceof NbtCompound compoundTag) {
        NbtIo.writeCompressed(compoundTag, path.toFile());
        return true;
      }

      throw new IOException("Error encoding to NBT: %s".formatted(result));
    } catch (IOException ex) {
      BlockgameProfiles.LOGGER.error("[Blockgame Profiles] Error saving NBT to {}", path, ex);
      return false;
    }
  }

  public static <T> Optional<T> loadFromNbt(Codec<T> codec, Path path) {
    if (Files.isRegularFile(path)) {
      try {
        FileInputStream stream = new FileInputStream(path.toFile());
        NbtCompound tag = NbtIo.readCompressed(stream);
        var loaded = codec.decode(NbtOps.INSTANCE, tag).get();
        if (loaded.right().isPresent()) {
          throw new IOException("Invalid NBT: %s".formatted(loaded.right().get()));
        } else {
          return Optional.ofNullable(loaded.left().get().getFirst());
        }
      } catch (IOException ex) {
        BlockgameProfiles.LOGGER.error("[Blockgame Profiles] Error loading NBT from {}", path, ex);
        FileHelper.tryMove(path, path.resolveSibling(path.getFileName() + ".corrupted"), StandardCopyOption.REPLACE_EXISTING);
      }
    }
    return Optional.empty();
  }

  public static void tryMove(Path from, Path to, CopyOption... options) {
    try {
      Files.move(from, to, options);
    } catch (IOException ex) {
      BlockgameProfiles.LOGGER.error("[Blockgame Profiles] Error moving file from {} to {}", from, to, ex);
    }
  }

  public static boolean deleteIfExists(Path to) {
    try {
      Files.deleteIfExists(to);
      return true;
    } catch (IOException ex) {
      BlockgameProfiles.LOGGER.error("[Blockgame Profiles] Error deleting file {}", to, ex);
      return false;
    }
  }
}
