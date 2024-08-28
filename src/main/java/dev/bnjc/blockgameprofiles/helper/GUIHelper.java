package dev.bnjc.blockgameprofiles.helper;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class GUIHelper {
  public static Identifier sprite(String path) {
    return new Identifier(BlockgameProfiles.MOD_ID, path);
  }

  public static TexturedButtonWidget button(int x, int y, String path, String tooltipKey, ButtonWidget.PressAction pressAction) {
    TexturedButtonWidget button = new TexturedButtonWidget(
        x,
        y,
        12,
        12,
        new ButtonTextures(sprite(path + "/button"), sprite(path + "/button_highlighted")),
        pressAction
    );
    button.setTooltip(Tooltip.of(Text.translatable(tooltipKey)));
    return button;
  }
}
