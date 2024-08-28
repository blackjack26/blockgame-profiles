package dev.bnjc.blockgameprofiles.config.modules;

import lombok.Getter;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

@Getter
@Config(name = "blockgameprofiles")
@Config.Gui.Background("minecraft:textures/block/polished_blackstone_bricks.png")
public class ModConfig  extends PartitioningSerializer.GlobalData {
  @Getter
  @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
  GeneralConfig generalConfig = new GeneralConfig();

  @Getter
  @ConfigEntry.Gui.CollapsibleObject(startExpanded = false)
  StorageConfig storageConfig = new StorageConfig();
}
