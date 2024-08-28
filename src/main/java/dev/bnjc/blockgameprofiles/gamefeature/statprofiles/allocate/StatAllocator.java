package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.event.chat.ReceiveChatMessageEvent;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.StatScreenManager;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute.PlayerAttribute;
import dev.bnjc.blockgameprofiles.helper.SoundHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StatAllocator {
  private final StatScreenManager screenManager;

  /**
   * The slot that holds the item to clear the stats. This is important to keep track of when we need to reset
   * the stats.
   */
  @Setter
  private int reallocationSlot = -1;

  /**
   * The available points to allocate to the player's stats. The value is found by parsing the NBT data of a stat
   * item. The text at the bottom of the tooltip indicates the number of points available to allocate.
   */
  @Setter
  private int availablePoints;

  /**
   * The number of points spent on the player's stats. This value is found by parsing the NBT data of the reallocation
   * item. It indicates "You have spent a total of X attributes." where X is the number of points spent.
   */
  private int spentPoints;
  private int previewSpentPoints;
  private int previewTotalPoints;

  @Nullable
  private Map<String, PlayerAttribute> attributes;

  @Nullable
  private Map<String, PlayerAttribute> previewAttributes;

  @Getter
  private boolean preview;

  @Nullable
  private Iterator<Command> commandIterator;

  @Setter
  @Nullable
  private Runnable onAttributesChanged;

  public StatAllocator(StatScreenManager screenManager) {
    this.screenManager = screenManager;

    this.attributes = null;
    this.availablePoints = -1;
    this.spentPoints = -1;

    this.commandIterator = null;
    this.preview = false;
    this.previewAttributes = null;
    this.previewSpentPoints = -1;
    this.previewTotalPoints = -1;

    ReceiveChatMessageEvent.EVENT.register(this::handleReceiveChatMessage);
  }

  public boolean allocate(@Nullable StatProfile profile) {
    if (profile == null || this.isAllocating()) {
      return false;
    }

    StatProfile tempProfile = new StatProfile("temp");
    tempProfile.fromAttributes(this.attributes == null ? new HashMap<>() : this.attributes);

    this.commandIterator = profile.buildCommands(tempProfile);
    this.nextStep();
    return true;
  }

  public void onScreenRecreate() {
    this.attributes = null;
  }

  public void nextStep() {
    if (this.commandIterator == null || !this.commandIterator.hasNext()) {
      this.stop();
      return;
    }

    // Wait for sync ID and inventory
    if (this.missingRequirements()) {
      return;
    }

    Command command = this.commandIterator.next();
    if (command.isReset()) {
      this.resetStats();
    } else if (command.isIncrement()) {
      PlayerAttribute attribute = this.attributes.get(command.attrName());
      if (attribute == null) {
        BlockgameProfiles.LOGGER.warn("Attribute not found: {}", command.attrName());
        this.nextStep();
      } else {
        this.incrementStat(attribute);
      }
    } else {
      BlockgameProfiles.LOGGER.warn("Unknown command: {}", command);
      this.nextStep();
    }
  }

  /**
   * Reset the stats by clicking the reallocation item slot.
   */
  public void resetStats() {
    StatScreenManager.State state = this.screenManager.getState();
    if (state != StatScreenManager.State.IDLE && state != StatScreenManager.State.CAPACITY_EXCEEDED) {
      BlockgameProfiles.LOGGER.warn("Cannot reset stats while in state: {}", state);
      return;
    }

    if (this.screenManager.waitingForScreen()) {
      BlockgameProfiles.LOGGER.warn("Waiting for screen. Cannot reset stats.");
      this.stop(StatScreenManager.State.FAILED);
      return;
    }

    if (this.reallocationSlot == -1) {
      BlockgameProfiles.LOGGER.warn("Reallocation slot is not set. Cannot reset stats.");
      this.stop(StatScreenManager.State.FAILED);
      return;
    }

    // If we are previewing changes, do not actually make a request to the server. Instead, modify the preview
    // state by clearing the spent points and resetting the attributes.
    if (this.preview) {
      this.previewResetStats();
      return;
    }

    ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
    if (interactionManager == null) {
      BlockgameProfiles.LOGGER.warn("Interaction manager is not available. Cannot reset stats.");
      this.stop(StatScreenManager.State.FAILED);
      return;
    }

    this.screenManager.changeState(StatScreenManager.State.WAITING_FOR_RESET);
    this.screenManager.invalidateScreen();

    interactionManager.clickSlot(this.screenManager.getSyncId(), this.reallocationSlot, 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
  }

  private void previewResetStats() {
    this.setSpentPoints(0);
    SoundHelper.playItemPickupSound();

    if (this.previewAttributes != null) {
      for (Map.Entry<String, PlayerAttribute> entry : this.previewAttributes.entrySet()) {
        entry.setValue(entry.getValue().reset());
      }
    }
    this.notifyAttributesChanged();
  }

  /**
   * Increment the stat by clicking the attribute slot.
   * @param attribute The attribute to increment
   */
  public void incrementStat(PlayerAttribute attribute) {
    this.adjustStat(attribute, attribute.getCost());
  }

  /**
   * Decrement the stat by clicking the attribute slot.
   * NOTE: This can only be used in preview mode
   *
   * @param attribute The attribute to decrement
   */
  public void decrementStat(PlayerAttribute attribute) {
    this.adjustStat(attribute, -attribute.getCost());
  }

  private void adjustStat(PlayerAttribute attribute, int amount) {
    if (this.screenManager.getState() != StatScreenManager.State.IDLE) {
      BlockgameProfiles.LOGGER.warn("Cannot increment stat while in state: {}", this.screenManager.getState());
      return;
    }

    if (this.screenManager.waitingForScreen()) {
      BlockgameProfiles.LOGGER.warn("Waiting for screen. Cannot increment stat.");
      this.stop(StatScreenManager.State.FAILED);
      return;
    }

    // If we are previewing changes, do not actually make a request to the server. Instead, modify the preview
    // state by incrementing the attribute.
    if (this.preview) {
      this.previewStatChange(attribute, amount);
      return;
    }

    ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
    if (interactionManager == null) {
      BlockgameProfiles.LOGGER.warn("Interaction manager is not available. Cannot increment stat.");
      this.stop(StatScreenManager.State.FAILED);
      return;
    }

    this.screenManager.changeState(StatScreenManager.State.WAITING_FOR_ALLOCATION);
    this.screenManager.invalidateScreen();

    interactionManager.clickSlot(this.screenManager.getSyncId(), attribute.getFirstSlot(), 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
  }

  private void previewStatChange(PlayerAttribute attribute, int amount) {
    if (this.getSpentPoints() + amount > this.getTotalPoints() || this.getSpentPoints() + amount < 0) {
      BlockgameProfiles.LOGGER.warn("Cannot change attribute: {}", attribute.getName());
      SoundHelper.playVillagerNoSound();
      return;
    }

    PlayerAttribute previewAttribute = attribute.adjust(amount);
    if (previewAttribute == attribute) {
      BlockgameProfiles.LOGGER.warn("Failed to adjust attribute: {}", attribute.getName());
      SoundHelper.playVillagerNoSound();
      return;
    }

    if (this.previewAttributes == null) {
      this.previewAttributes = new HashMap<>();
    }
    this.setSpentPoints(this.getSpentPoints() + amount);
    SoundHelper.playItemPickupSound();

    this.previewAttributes.put(attribute.getName(), previewAttribute);
    this.notifyAttributesChanged();
  }

  public void stop() {
    this.stop(StatScreenManager.State.IDLE);
  }

  private void stop(StatScreenManager.State state) {
    this.commandIterator = null;
    this.screenManager.changeState(state);
  }

  // region Getters and Setters

  public @Nullable Map<String, PlayerAttribute> getAttributes() {
    return this.preview ? this.previewAttributes : this.attributes;
  }

  public void setAttributes(Map<String, PlayerAttribute> attributes) {
    this.attributes = attributes;
    this.notifyAttributesChanged();

    if (this.screenManager.getState() == StatScreenManager.State.WAITING_FOR_INVENTORY) {
      this.screenManager.changeState(StatScreenManager.State.IDLE);
      this.nextStep();
    }
  }

  public int getSpentPoints() {
    return this.preview ? this.previewSpentPoints : this.spentPoints;
  }

  public void setSpentPoints(int spentPoints) {
    if (this.preview) {
      this.previewSpentPoints = spentPoints;
    } else {
      this.spentPoints = spentPoints;
    }
  }

  public int getAvailablePoints() {
    if (this.preview) {
      return this.previewTotalPoints - this.previewSpentPoints;
    }

    return this.availablePoints;
  }

  public void setPreview(boolean preview) {
    this.setPreview(preview, null);
  }

  public void setPreview(boolean preview, @Nullable StatProfile profile) {
    this.preview = preview;

    if (this.preview) {
      this.previewAttributes = new HashMap<>();
      if (this.attributes != null) {
        for (Map.Entry<String, PlayerAttribute> entry : this.attributes.entrySet()) {
          PlayerAttribute attribute = entry.getValue().copy();

          if (profile != null) {
            Integer spent = profile.getAttributeAllocations().get(entry.getKey());
            if (spent != null) {
              attribute = attribute.withSpent(spent);
            } else {
              attribute = attribute.reset();
            }
          }

          this.previewAttributes.put(entry.getKey(), attribute);
        }
      }

      this.previewTotalPoints = this.availablePoints + this.spentPoints;
      this.previewSpentPoints = Math.min(profile == null ? this.spentPoints : profile.getSpentPoints(), this.previewTotalPoints);
    } else {
      this.previewAttributes = null;
      this.previewSpentPoints = -1;
      this.previewTotalPoints = -1;
    }

    this.notifyAttributesChanged();
  }

  public boolean isAllocating() {
    return this.commandIterator != null;
  }

  public int getTotalPoints() {
    if (this.preview) {
      return this.previewTotalPoints;
    }

    if (this.availablePoints == -1 || this.getSpentPoints() == -1) {
      return -1;
    }

    return this.availablePoints + this.getSpentPoints();
  }

  // endregion Getters and Setters

  private void notifyAttributesChanged() {
    if (this.onAttributesChanged != null) {
      this.onAttributesChanged.run();
    }
  }

  private boolean missingRequirements() {
    return !this.checkSyncId() || !this.checkInventory();
  }

  private boolean checkSyncId() {
    if (this.screenManager.isDirtySyncId() || this.screenManager.getSyncId() == -1) {
      this.screenManager.changeState(StatScreenManager.State.WAITING_FOR_SYNC_ID);
      return false;
    }

    return true;
  }

  private boolean checkInventory() {
    if (this.reallocationSlot == -1 || this.attributes == null) {
      this.screenManager.changeState(StatScreenManager.State.WAITING_FOR_INVENTORY);
      return false;
    }

    return true;
  }

  private ActionResult handleReceiveChatMessage(MinecraftClient client, String message) {
    StatScreenManager.State state = this.screenManager.getState();

    if (state == StatScreenManager.State.WAITING_FOR_RESET) {
      if (message.contains("You successfully reset your attributes")) {
        this.nextStep();
        return ActionResult.CONSUME;
      }
    }

    if (state == StatScreenManager.State.WAITING_FOR_ALLOCATION) {
      if (message.contains("You successfully leveled up your")) {
        this.nextStep();
        return ActionResult.CONSUME;
      }

      if (message.contains("You do not have 1 attribute point")) {
        this.stop(StatScreenManager.State.CAPACITY_EXCEEDED);

        // We need to mark the sync ID as not dirty because the screen does not get recreated
        // when the capacity is exceeded.
        this.screenManager.setDirtySyncId(false);
        return ActionResult.CONSUME;
      }
    }

    return ActionResult.PASS;
  }
}
