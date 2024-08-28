package dev.bnjc.blockgameprofiles;

import dev.bnjc.blockgameprofiles.config.modules.ModConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockgameProfiles implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("BlockgameProfiles");
  public static final String MOD_ID = "blockgameprofiles";

  @Getter
  private static boolean modMenuPresent = false;

  @Getter
  private static ModConfig config;

  @Override
  public void onInitialize() {
    // Cloth Config
    AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

    // Detect if the ModMenu mod is present.
    if (FabricLoader.getInstance().isModLoaded("modmenu")) {
      modMenuPresent = true;
    }
  }

  public static Logger getLogger(String suffix) {
    return LoggerFactory.getLogger(BlockgameProfiles.class.getCanonicalName() + "/" + suffix);
  }
}
