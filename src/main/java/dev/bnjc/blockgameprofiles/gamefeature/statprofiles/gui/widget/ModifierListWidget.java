package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.widget;

import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute.PlayerAttribute;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.screen.StatScreen;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier.ModifierDisplay;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier.ModifierType;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier.StatModifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class ModifierListWidget extends ScrollableViewWidget {
  private final StatScreen parent;
  private int bottomY = 0;

  private final List<ModifierDisplay> offenseModifiers = new ArrayList<>();
  private final List<ModifierDisplay> defenseModifiers = new ArrayList<>();
  private final List<ModifierDisplay> unknownModifiers = new ArrayList<>();

  public ModifierListWidget(StatScreen parent, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
  }

  public void build() {
    this.offenseModifiers.clear();
    this.defenseModifiers.clear();
    this.unknownModifiers.clear();

    Map<String, ModifierDisplay> modifiers = new HashMap<>();

    var attributes = this.parent.getScreenManager().getStatAllocator().getAttributes();
    if (attributes != null) {
      for (PlayerAttribute attr : attributes.values()) {
        for (StatModifier buff : attr.getBuffs()) {
          if (!modifiers.containsKey(buff.getName())) {
            modifiers.put(buff.getName(), new ModifierDisplay(buff.getName(), buff.getType(), buff.getCategory()));
          }

          modifiers.get(buff.getName()).addValue(buff.getValue() * attr.getSpent());
        }
      }
    }

    for (ModifierDisplay modifier : modifiers.values()) {
      if (modifier.getValue() == 0) continue; // TODO: Maybe add config to show 0 value modifiers?

      switch (modifier.getCategory()) {
        case OFFENSE -> this.offenseModifiers.add(modifier);
        case DEFENSE -> this.defenseModifiers.add(modifier);
        default -> this.unknownModifiers.add(modifier);
      }
    }

    this.offenseModifiers.sort(Comparator.comparingInt(ModifierDisplay::getOrder));
    this.defenseModifiers.sort(Comparator.comparingInt(ModifierDisplay::getOrder));
    this.unknownModifiers.sort(Comparator.comparingInt(ModifierDisplay::getOrder));

    this.visible = true;
  }

  @Override
  protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
    if (offenseModifiers.isEmpty() && defenseModifiers.isEmpty() && unknownModifiers.isEmpty()) {
      return;
    }

    // Create a semi-transparent black background
    context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), Math.max(this.bottomY + 4, this.height), 0x80_000000);

    int y = this.getY() + 8;
    int x = this.getX() + 8;
    int maxW = 0;
    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    // Offense Stats
    if (!offenseModifiers.isEmpty()) {
      MutableText titleText = Text.literal("Offense").formatted(Formatting.BOLD);
      maxW = Math.max(maxW, textRenderer.getWidth(titleText));

      context.drawTextWithShadow(textRenderer, titleText, x, y, 0xFFFFFF);
      y += 12;
      for (ModifierDisplay modifier : offenseModifiers) {
        MutableText modifierText = getModifierText(modifier);
        maxW = Math.max(maxW, textRenderer.getWidth(modifierText));

        context.drawText(textRenderer, modifierText, x, y, 0xFFFFFF, false);
        y += 12;
      }

      if (!defenseModifiers.isEmpty() || !unknownModifiers.isEmpty()) {
        y += 8;
      }
    }

    // Defense Stats
    if (!defenseModifiers.isEmpty()) {
      MutableText titleText = Text.literal("Defense").formatted(Formatting.BOLD);
      maxW = Math.max(maxW, textRenderer.getWidth(titleText));
      context.drawTextWithShadow(textRenderer, titleText, x, y, 0xFFFFFF);
      y += 12;

      for (ModifierDisplay modifier : defenseModifiers) {
        MutableText modifierText = getModifierText(modifier);
        maxW = Math.max(maxW, textRenderer.getWidth(modifierText));

        context.drawText(textRenderer, modifierText, x, y, 0xFFFFFF, false);
        y += 12;
      }

      if (!unknownModifiers.isEmpty()) {
        y += 8;
      }
    }

    // Other Stats
    if (!unknownModifiers.isEmpty()) {
      MutableText titleText = Text.literal("Other").formatted(Formatting.BOLD);
      maxW = Math.max(maxW, textRenderer.getWidth(titleText));
      context.drawTextWithShadow(textRenderer, titleText, x, y, 0xFFFFFF);
      y += 12;

      for (ModifierDisplay modifier : unknownModifiers) {
        MutableText modifierText = getModifierText(modifier);
        maxW = Math.max(maxW, textRenderer.getWidth(modifierText));

        context.drawText(textRenderer, modifierText, x, y, 0xFFFFFF, false);
        y += 12;
      }
    }

    this.bottomY = y;
    this.setWidth(Math.max(maxW + 8, this.getWidth()));
    this.setX(this.parent.width - this.getWidth());
  }

  @Override
  protected int getContentsHeight() {
    return this.bottomY - this.getY();
  }

  private MutableText getModifierText(ModifierDisplay modifier) {
    double value = modifier.getValue();

    // Format the value to 2 decimal places at most.
    String valueStr = String.format("%.2f", value);

    // Remove trailing zeroes
    // Ex:
    // - 1.00 -> 1
    // - 1.50 -> 1.5
    // - 1.75 -> 1.75
    if (valueStr.contains(".") && valueStr.endsWith("0")) {
      valueStr = valueStr.replaceAll("0*$", "");

      // Remove the decimal point if there are no more digits after it.
      if (valueStr.endsWith(".")) {
        valueStr = valueStr.substring(0, valueStr.length() - 1);
      }
    }

    // Add a '%' sign if the modifier is relative.
    if (modifier.getType() == ModifierType.RELATIVE) {
      valueStr += "%";
    }

    // Add a '+' sign if the value is positive.
    if (value > 0) {
      valueStr = "+" + valueStr;
    }

    return Text.literal(modifier.getIcon() + " " + modifier.getDisplayName() + ": ")
        .append(Text.literal(valueStr).formatted(modifier.getValueColor()))
        .formatted(Formatting.GRAY);
  }
}
