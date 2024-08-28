package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.widget;

import dev.bnjc.blockgameprofiles.helper.GUIHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public abstract class ScrollableViewWidget extends ScrollableWidget {
  private static final Identifier SCROLLBAR_TEXTURE = GUIHelper.sprite("scroller");

  public ScrollableViewWidget(int x, int y, int width, int height, Text text) {
    super(x, y, width, height, text);
  }

  @Override
  public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    // This method is very similar to the super method, but removes the background box and padding around the contents
    if (!this.visible) {
      return;
    }

    context.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    context.getMatrices().push();
    context.getMatrices().translate(0.0, -this.getScrollY(), 0.0);
    this.renderContents(context, mouseX, mouseY, delta);
    context.getMatrices().pop();
    context.disableScissor();
    this.renderOverlay(context);
  }

  @Override
  protected void renderOverlay(DrawContext context) {
    if (this.overflows()) {
      this.drawScrollbar(context);
    }
  }

  protected int getScrollbarWidth() {
    return 6;
  }

  private void drawScrollbar(DrawContext context) {
    int h = this.getScrollbarThumbHeight();
    int x = this.getX() + this.getWidth();
    int y = Math.max(this.getY(), (int)this.getScrollY() * (this.height - h) / this.getMaxScrollY() + this.getY());
    context.drawGuiTexture(SCROLLBAR_TEXTURE, x - 2, y, this.getScrollbarWidth(), h);
  }

  private int getScrollbarThumbHeight() {
    return MathHelper.clamp((int)((float)(this.height * this.height) / (float)this.getContentsHeightWithPadding()), 32, this.height);
  }

  private int getContentsHeightWithPadding() {
    return this.getContentsHeight();
  }

  @Override
  public double getScrollY() {
    // Change the visibility of this method to public
    return super.getScrollY();
  }

  @Override
  protected int getPadding() {
    return 0;
  }

  @Override
  protected double getDeltaYPerScroll() {
    return 9.0f;
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    // Not sure if there is anything we need here
  }
}
