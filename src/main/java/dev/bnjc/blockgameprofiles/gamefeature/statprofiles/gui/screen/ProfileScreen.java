package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.StatScreenManager;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate.StatProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class ProfileScreen extends GenericContainerScreen {
  private static final int BUTTON_SIZE = 12;
  private static final int BUTTON_PADDING = 3;

  private final StatScreenManager screenManager;

  private ButtonWidget addProfileButton;

  @Nullable
  private StatProfile previewingProfile;

  private int left = 0;
  private int top = 0;

  public ProfileScreen(
      GenericContainerScreenHandler handler,
      PlayerInventory inventory,
      Text title,
      StatScreenManager screenManager
  ) {
    super(handler, inventory, title);

    this.screenManager = screenManager;
    this.previewingProfile = null;
  }

  @Override
  protected void init() {
    this.left = (this.width - this.backgroundWidth) / 2;
    this.top = (this.height - this.backgroundHeight) / 2;

    super.init();

    this.addProfileButton = new ButtonWidget.Builder(Text.literal("+"), (button) -> {
      this.previewingProfile = null;

      if (this.screenManager.getStatAllocator().isPreview()) {
        this.closeProfilePreview();
      } else {
        this.openProfilePreview();
      }
    })
        .size(BUTTON_SIZE, BUTTON_SIZE)
        .position(this.left + this.backgroundWidth - (BUTTON_SIZE + BUTTON_PADDING) - 4, this.top + 4)
        .tooltip(Tooltip.of(Text.translatable("menu.blockgame.stats.create_profile")))
        .build();
    this.addProfileButton.visible = !this.screenManager.getStatAllocator().isPreview();
    this.addDrawableChild(this.addProfileButton);
  }

  @Override
  protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
    // Render stuff here before the tooltip
    RenderSystem.disableDepthTest();
    context.getMatrices().push();
    context.getMatrices().translate(this.x, this.y, 0.0f);

    this.renderSlotEnhancements(context);

    context.getMatrices().pop();
    RenderSystem.enableDepthTest();

    super.drawMouseoverTooltip(context, x, y);
  }

  public void onAttributesChanged() {
    // Do something
  }

  public void onReallocationSlotSet() {
    // Do something
  }

  @Override
  public void close() {
    super.close();
    this.screenManager.onScreenClose();
  }

  private void openProfilePreview() {
    this.addProfileButton.setMessage(Text.literal("Cancel"));
    this.addProfileButton.setWidth(40);
    this.addProfileButton.setPosition(this.left + this.backgroundWidth - 40 - BUTTON_PADDING - 4, this.top + 4);
    this.addProfileButton.setTooltip(Tooltip.of(Text.translatable("menu.blockgame.stats.cancel_preview")));

    this.screenManager.getStatAllocator().setPreview(true, this.previewingProfile);
  }

  private void closeProfilePreview() {
    this.addProfileButton.setMessage(Text.literal("+"));
    this.addProfileButton.setWidth(BUTTON_SIZE);
    this.addProfileButton.setPosition(this.left + this.backgroundWidth - BUTTON_SIZE - BUTTON_PADDING - 4, this.top + 4);
    this.addProfileButton.setTooltip(Tooltip.of(Text.translatable("menu.blockgame.stats.create_profile")));

    this.screenManager.getStatAllocator().setPreview(false);
  }

  // region Rendering

  private void renderSlotEnhancements(DrawContext context) {
    int barColor = MathHelper.hsvToRgb(1.0f / 3.0f, 1.0f, 1.0f);
    if (this.screenManager.getStatAllocator().isPreview()) {
      barColor = 0xFBD15E;
    }

    var attributes = this.screenManager.getStatAllocator().getAttributes();
    if (attributes != null) {
      for (var attr : attributes.values()) {
        for (var slotId : attr.getSlots()) {
          var slot = this.handler.slots.get(slotId);

          if (this.screenManager.getStatAllocator().isPreview()) {
            // Light green overlay
            context.fill(RenderLayer.getGuiOverlay(), slot.x, slot.y, slot.x + 16, slot.y + 16, 0x0E9AFF78);
          }

          int barX = slot.x + 1;
          int barY = slot.y + 13;
          int barWidth = 14;

          context.fill(RenderLayer.getGuiOverlay(), barX, barY, barX + barWidth, barY + 2, -16777216);
          context.fill(RenderLayer.getGuiOverlay(), barX, barY, barX + attr.getBarStep(barWidth), barY + 1, barColor | 0xFF000000);
        }
      }
    }
  }

  // endregion Rendering
}
