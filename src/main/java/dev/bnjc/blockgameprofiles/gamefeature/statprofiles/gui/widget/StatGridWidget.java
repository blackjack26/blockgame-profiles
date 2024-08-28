package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.widget;

import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute.PlayerAttribute;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.screen.StatScreen;
import dev.bnjc.blockgameprofiles.helper.GUIHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class StatGridWidget extends ClickableWidget {
  public static final int GRID_SLOT_SIZE = 18;

  private static final Identifier BACKGROUND = GUIHelper.sprite("widgets/slot_background");
  private static final int GRID_COLUMNS = 9;
  private static final int GRID_ROWS = 5;

  @Getter
  private final StatScreen screen;

  private int mouseClickButton;

  @Setter
  @Nullable
  private Consumer<PlayerAttribute> onAttributeHover;

  public StatGridWidget(StatScreen screen, int x, int y) {
    super(x, y, GRID_COLUMNS * GRID_SLOT_SIZE, GRID_ROWS * GRID_SLOT_SIZE, Text.empty());

    this.screen = screen;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    this.mouseClickButton = button;

    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    int x = (int) ((mouseX - this.getX()) / GRID_SLOT_SIZE);
    int y = (int) ((mouseY - this.getY()) / GRID_SLOT_SIZE);
    int slot = (y * GRID_COLUMNS) + x;

    var attributes = this.screen.getScreenManager().getStatAllocator().getAttributes();
    if (attributes == null) {
      return;
    }

    for (PlayerAttribute attr : attributes.values()) {
      if (attr.getSlots().contains(slot)) {
        if (mouseClickButton == 0) {
          this.screen.getScreenManager().getStatAllocator().incrementStat(attr);
        } else if (mouseClickButton == 1) {
          this.screen.getScreenManager().getStatAllocator().decrementStat(attr);
        }
        break;
      }
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    context.drawGuiTexture(BACKGROUND, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    this.renderItems(context);
    this.renderTooltip(context, mouseX, mouseY);
  }

  private void renderItems(DrawContext context) {
    // Render attributes in their slot position
    var attributes = this.screen.getScreenManager().getStatAllocator().getAttributes();
    if (attributes == null) {
      return;
    }

    int barColor = MathHelper.hsvToRgb(1.0f / 3.0f, 1.0f, 1.0f);
    if (this.screen.getScreenManager().getStatAllocator().isPreview()) {
      barColor = 0xFBD15E;
    }

    for (PlayerAttribute attr : attributes.values()) {
      for (int slot : attr.getSlots()) {
        int x = this.getX() + GRID_SLOT_SIZE * (slot % GRID_COLUMNS);
        int y = this.getY() + GRID_SLOT_SIZE * (slot / GRID_COLUMNS);

        context.drawItem(attr.getItemStack(), x + 1, y + 1);

        int barX = x + 2;
        int barY = y + 14;
        int barWidth = 14;

        context.fill(RenderLayer.getGuiOverlay(), barX, barY, barX + barWidth, barY + 2, -16777216);
        context.fill(RenderLayer.getGuiOverlay(), barX, barY, barX + attr.getBarStep(barWidth), barY + 1, barColor | 0xFF000000);
      }
    }
  }

  private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
    if (!this.isHovered()) {
      this.setHoveredAttribute(null);
      return;
    }

    int x = (mouseX - this.getX()) / GRID_SLOT_SIZE;
    int y = (mouseY - this.getY()) / GRID_SLOT_SIZE;
    if (x < 0 || x > GRID_COLUMNS || y < 0 || y >= GRID_ROWS) {
      this.setHoveredAttribute(null);
      return;
    }

    // Hover indicator
    int slotX = this.getX() + x * GRID_SLOT_SIZE;
    int slotY = this.getY() + y * GRID_SLOT_SIZE;
    context.fill(slotX + 1, slotY + 1, slotX + GRID_SLOT_SIZE - 1, slotY + GRID_SLOT_SIZE - 1, 0x80_FFFFFF);

    var attributes = this.screen.getScreenManager().getStatAllocator().getAttributes();
    if (attributes == null) {
      this.setHoveredAttribute(null);
      return;
    }

    PlayerAttribute foundAttr = null;
    int slot = (y * GRID_COLUMNS) + x;
    for (PlayerAttribute attr : attributes.values()) {
      if (attr.getSlots().contains(slot)) {
        foundAttr = attr;
        break;
      }
    }

    this.setHoveredAttribute(foundAttr);
  }

  private void setHoveredAttribute(@Nullable PlayerAttribute attribute) {
    if (this.onAttributeHover != null) {
      if (attribute != null) {
        attribute.fixLoreText();
      }
      this.onAttributeHover.accept(attribute);
    }
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  @Override
  protected boolean isValidClickButton(int button) {
    return button == 0 || button == 1;
  }
}
