package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.screen;

import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.StatScreenManager;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate.StatProfile;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute.PlayerAttribute;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.widget.ModifierListWidget;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.widget.StatGridWidget;
import dev.bnjc.blockgameprofiles.helper.GUIHelper;
import dev.bnjc.blockgameprofiles.storage.BlockgameData;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class StatScreen extends Screen {
  private static final Identifier BACKGROUND_SPRITE = GUIHelper.sprite("background");

  private static final int BUTTON_SIZE = 14;

  private static final int MENU_WIDTH = 176;
  private static final int MENU_HEIGHT = 154; // 5 * 18 + 48 + 16

  private static final int TITLE_LEFT = 8;
  private static final int TITLE_TOP = 8;

  @Getter
  private final StatScreenManager screenManager;

  private int left = 0;
  private int top = 0;

  @Nullable
  private PlayerAttribute hoveredAttribute;
  @Nullable
  private StatProfile previewingProfile;

  private StatGridWidget statGridWidget;
  private ModifierListWidget modifierListWidget;
  private TextFieldWidget profileNameField;

  private TexturedButtonWidget reallocateButton;
  private TexturedButtonWidget addProfileButton;
  private TexturedButtonWidget cancelPreviewButton;
  private TexturedButtonWidget saveProfileButton;
  private TexturedButtonWidget deleteProfileButton;
  private Map<String, TexturedButtonWidget> profileButtons;

  private boolean isEditing = false;

  public StatScreen(Text title, StatScreenManager screenManager) {
    super(title);

    this.screenManager = screenManager;
    this.hoveredAttribute = null;
    this.previewingProfile = null;

    if (!this.screenManager.getStatAllocator().isAllocating()) {
      // This allows any errors to be cleared when the screen is opened again
      this.screenManager.changeState(StatScreenManager.State.IDLE);
    }
  }

  @Override
  protected void init() {
    this.left = (this.width - getMenuWidth()) / 2;
    this.top = (this.height - getMenuHeight()) / 2;

    super.init();

    // Grid widget
    this.statGridWidget = new StatGridWidget(this, this.left + 7, this.top + 40);
    this.statGridWidget.visible = true;
    this.statGridWidget.setOnAttributeHover((attribute) -> this.hoveredAttribute = attribute);
    this.addDrawableChild(this.statGridWidget);

    // Reallocation button
    this.reallocateButton = GUIHelper.button(
        this.left + getMenuWidth() - (3 + BUTTON_SIZE),
        this.top + 5,
        "widgets/reallocate",
        "menu.blockgame.stats.reallocate",
        (button) -> {
          this.screenManager.getStatAllocator().resetStats();
        });
    this.reallocateButton.visible = this.screenManager.getSyncId() != -1;
    this.addDrawableChild(this.reallocateButton);

    // Add Profile Button
    this.addProfileButton = GUIHelper.button(
        this.left + 7,
        this.top + 5 + (3 + BUTTON_SIZE),
        "widgets/create",
        "menu.blockgame.stats.create_profile",
        (button) -> {
          this.previewingProfile = null;
          this.openProfilePreview();
        }
    );
    this.addProfileButton.visible = !this.screenManager.getStatAllocator().isPreview();
    this.addDrawableChild(this.addProfileButton);

    // Profile Buttons
    this.profileButtons = new HashMap<>();
    if (BlockgameData.INSTANCE != null) {
      var profiles = new ArrayList<>(BlockgameData.INSTANCE.getStatProfiles().values());
      profiles.sort(Comparator.comparingInt(StatProfile::getOrder));
      for (int i = 0; i < profiles.size(); i++) {
        this.addProfileButton(profiles.get(i), i);
      }
    }

    // Profile name field
    this.profileNameField = this.addDrawableChild(new TextFieldWidget(
        this.textRenderer,
        this.left + 8,
        this.top + 6 + (3 + BUTTON_SIZE),
        getMenuWidth() - 16,
        12,
        this.profileNameField,
        Text.literal("")
    ));
    this.profileNameField.setPlaceholder(Text.translatable("menu.blockgame.stats.profile_placeholder"));
    this.profileNameField.setDrawsBackground(true);
    this.profileNameField.setEditableColor(0xFFFFFF);
    this.profileNameField.visible = this.screenManager.getStatAllocator().isPreview();

    // Cancel Preview Button
    this.cancelPreviewButton = GUIHelper.button(
        this.left + getMenuWidth() - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        "widgets/close",
        "menu.blockgame.stats.cancel_preview",
        (button) -> this.closeProfilePreview()
    );
    this.cancelPreviewButton.visible = this.screenManager.getStatAllocator().isPreview();
    this.addDrawableChild(this.cancelPreviewButton);

    // Save Profile button
    this.saveProfileButton = GUIHelper.button(
        this.left + getMenuWidth() - 3 * (3 + BUTTON_SIZE),
        this.top + 5,
        "widgets/save_profile",
        "menu.blockgame.stats.save_profile",
        (button) -> {
          int order;
          if (this.previewingProfile != null) {
            BlockgameData.removeProfile(this.previewingProfile.getName());
            order = this.previewingProfile.getOrder();
          } else {
            // Get the next available order
            order = 0;
            for (StatProfile profile : BlockgameData.INSTANCE.getStatProfiles().values()) {
              if (profile.getOrder() >= order) {
                order = profile.getOrder() + 1;
              }
            }
          }

          StatProfile newProfile = new StatProfile(this.profileNameField.getText(), order);
          newProfile.fromAttributes(this.screenManager.getStatAllocator().getAttributes());
          BlockgameData.saveProfile(newProfile);

          if (this.previewingProfile != null) {
            // Update existing profile button
            TexturedButtonWidget existingButton = this.profileButtons.get(this.previewingProfile.getName());
            if (existingButton != null) {
              existingButton.setTooltip(Tooltip.of(Text.literal(newProfile.getName())));
            }
          } else {
            // Add new profile button
            this.addProfileButton(newProfile, this.profileButtons.size());
          }

          this.closeProfilePreview();
          this.adjustProfileButtons();

          // Reallocate stats to the new profile
          this.getScreenManager().getStatAllocator().allocate(newProfile);
        });
    this.saveProfileButton.visible = this.screenManager.getStatAllocator().isPreview();
    this.addDrawableChild(this.saveProfileButton);

    // Delete Profile button
    this.deleteProfileButton = GUIHelper.button(
        this.left + 7,
        this.top + getMenuHeight() - (3 + BUTTON_SIZE) - 3,
        "widgets/remove",
        "menu.blockgame.stats.delete_profile",
        (button) -> {
          if (this.previewingProfile != null) {
            BlockgameData.removeProfile(this.previewingProfile.getName());
            this.remove(this.profileButtons.remove(this.previewingProfile.getName()));

            this.closeProfilePreview();
            this.adjustProfileButtons();
          }
        });
    this.deleteProfileButton.visible = this.screenManager.getStatAllocator().isPreview();
    this.addDrawableChild(this.deleteProfileButton);

    // Modifier list widget
//    this.modifierListWidget = new ModifierListWidget(this, this.left + getMenuWidth() + 4, this.top, 175, getMenuHeight());
    int modifierListWidth = Math.min(200, this.width - (this.left + getMenuWidth() + 4));
    this.modifierListWidget = new ModifierListWidget(this, this.width - modifierListWidth, 0, modifierListWidth, this.height);
    this.modifierListWidget.visible = false;
    this.addDrawableChild(this.modifierListWidget);

    this.adjustProfileButtons();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    // Title
    context.drawText(this.textRenderer, this.title, this.left + TITLE_LEFT, this.top + TITLE_TOP, 0xFFFFFF, false);

    // Centered below the book item, "Available X/Y"
    int totalPoints = this.screenManager.getStatAllocator().getTotalPoints();
    MutableText text = Text.translatable("menu.blockgame.stats.available");
    if (totalPoints == -1) {
      text.append(Text.literal("?").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
    } else {
      text.append(Text.literal("" + this.screenManager.getStatAllocator().getAvailablePoints())
              .formatted(this.screenManager.getStatAllocator().getAvailablePoints() > 0 ? Formatting.GREEN : Formatting.RED, Formatting.BOLD))
          .append(Text.literal("/" + totalPoints));
    }

    int availableX = this.left + (getMenuWidth() - this.textRenderer.getWidth(text)) / 2;
    // Place below the inventory grid
    int availableY = this.top + 40 + (5 * 18) + 7;

    context.drawText(this.textRenderer, text, availableX, availableY, 0xFFFFFF, false);

    // Render tooltip
    this.renderTooltip(context, mouseX, mouseY);
  }

  private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
    if (this.hoveredAttribute == null) {
      return;
    }

    // Render tooltip
    context.getMatrices().push();
    context.getMatrices().translate(0, 0, 200.0f);
    context.drawItemTooltip(this.textRenderer, this.hoveredAttribute.getItemStack(), mouseX, mouseY);
    context.getMatrices().pop();
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    if (this.screenManager.getStatAllocator().isPreview()) {
      // Background border
      context.fillGradient(this.left - 1, this.top - 1, this.left + getMenuWidth() + 2, this.top + getMenuHeight() + 2, 0x88_FBDB6C, 0x88_FBDB6C);
    }

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, getMenuWidth(), getMenuHeight());
  }

  @Override
  public void close() {
    super.close();

    // Send a packet to the server saying we have closed the screen
    if (this.client != null && this.client.player != null) {
      this.client.player.closeHandledScreen();
    }

    this.screenManager.onScreenClose();
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_E) {
      // Check to see if any buttons are hovered
      for (TexturedButtonWidget button : this.profileButtons.values()) {
        if (button.isHovered()) {
          this.isEditing = true;
          button.onPress();
          return true;
        }
      }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  public void onReallocationSlotSet() {
    this.reallocateButton.visible = true;
  }

  public void onAttributesChanged() {
    this.modifierListWidget.build();
  }

  public int getMenuWidth() {
    return MENU_WIDTH;
  }

  public int getMenuHeight() {
    return MENU_HEIGHT;
  }

  private void openProfilePreview() {
    this.screenManager.getStatAllocator().setPreview(true, this.previewingProfile);

    this.addProfileButton.visible = false;
    for (TexturedButtonWidget button : this.profileButtons.values()) {
      button.visible = false;
    }

    this.saveProfileButton.visible = true;
    this.deleteProfileButton.visible = true;
    this.cancelPreviewButton.visible = true;
    this.profileNameField.visible = true;

    if (this.previewingProfile == null) {
      this.profileNameField.setText("");
    }
  }

  private void closeProfilePreview() {
    this.screenManager.getStatAllocator().setPreview(false);

    this.addProfileButton.visible = true;
    for (TexturedButtonWidget button : this.profileButtons.values()) {
      button.visible = true;
    }

    this.saveProfileButton.visible = false;
    this.deleteProfileButton.visible = false;
    this.cancelPreviewButton.visible = false;
    this.profileNameField.visible = false;
    this.previewingProfile = null;
  }

  private void adjustProfileButtons() {
    if (BlockgameData.INSTANCE == null) {
      return;
    }

    var statProfiles = BlockgameData.INSTANCE.getStatProfiles();

    // Only show the add profile button if we are not previewing and there are less than 5 profiles
    this.addProfileButton.visible = !this.screenManager.getStatAllocator().isPreview() && statProfiles.size() < 5;
    this.addProfileButton.setX(
        this.left + 7 + (3 + BUTTON_SIZE) * statProfiles.size()
    );

    @Nullable var attributes = this.screenManager.getStatAllocator().getAttributes();

    // Reorder the profile buttons
    var profiles = new ArrayList<>(statProfiles.values());
    profiles.sort(Comparator.comparingInt(StatProfile::getOrder));

    for (int i = 0; i < profiles.size(); i++) {
      StatProfile profile = profiles.get(i);

      TexturedButtonWidget button = this.profileButtons.get(profile.getName());
      if (button != null) {
        button.setX(this.left + 7 + (3 + BUTTON_SIZE) * i);

        Tooltip tooltip = Tooltip.of(Text.empty());
        tooltip.lines = new ArrayList<>();
        tooltip.lines.add(Text.literal(profile.getName()).asOrderedText());

        // Determine current profile
        if (profile.matches(attributes)) {
          // Set tooltip to "<Name> (Current)" with current in green
          tooltip.lines.add(
              Text.literal("(")
                  .append(Text.translatable("menu.blockgame.stats.current").formatted(Formatting.GREEN))
                  .append(Text.literal(")")).asOrderedText()
          );

          button.textures = new ButtonTextures(GUIHelper.sprite("widgets/current_profile/button"), GUIHelper.sprite("widgets/current_profile/button_highlighted"));
        } else {
          button.textures = new ButtonTextures(GUIHelper.sprite("widgets/change_profile/button"), GUIHelper.sprite("widgets/change_profile/button_highlighted"));
        }

        tooltip.lines.add(Text.empty().asOrderedText());
        tooltip.lines.add(Text.literal("Left-click to select").formatted(Formatting.GRAY).asOrderedText());
        tooltip.lines.add(Text.literal("Press ")
            .append(Text.literal("E").formatted(Formatting.YELLOW))
            .append(Text.literal(" to edit")).asOrderedText());

        button.setTooltip(tooltip);
      }
    }
  }

  private void addProfileButton(StatProfile profile, int index) {
    TexturedButtonWidget button = GUIHelper.button(
        this.left + 7 + (3 + BUTTON_SIZE) * index,
        this.top + 5 + (3 + BUTTON_SIZE),
        "widgets/change_profile",
        profile.getName(),
        (b) -> {
          if (this.isEditing) {
            this.previewingProfile = profile;
            this.profileNameField.setText(profile.getName());
            this.openProfilePreview();
            this.isEditing = false;
          }
          else {
            this.screenManager.getStatAllocator().allocate(profile);
            this.adjustProfileButtons();
          }
        });
    this.profileButtons.put(profile.getName(), button);
    this.addDrawableChild(button);
  }
}
