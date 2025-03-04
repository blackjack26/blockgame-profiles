package dev.bnjc.blockgameprofiles.gamefeature.statprofiles;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate.StatAllocator;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute.PlayerAttribute;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.screen.ProfileScreen;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.gui.screen.StatScreen;
import dev.bnjc.blockgameprofiles.helper.NbtHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
public final class StatScreenManager {
  private final StatProfileGameFeature gameFeature;
  private final StatAllocator statAllocator;

  @Nullable
  private StatScreen screen;

  /**
   * The sync ID of the current screen. This changes every time we make a selection to allocate a stat. So
   * it is important to keep track of this ID to send the correct packet to the server. Otherwise, the server
   * rejects the packet.
   */
  private int syncId;

  /**
   * Whether the sync ID is dirty. This is used to determine if we need refresh the sync ID.
   */
  @Setter
  private boolean dirtySyncId;

  @Nullable
  private List<ItemStack> receivedInventory;
  private boolean shouldParsePoints;

  /**
   * Keeps track of the stat screen state to determine what to do next. This is particularly useful when
   * automatically allocating stats and waiting for the server to respond.
   */
  private State state;

  public StatScreenManager(StatProfileGameFeature gameFeature) {
    this.gameFeature = gameFeature;
    this.statAllocator = new StatAllocator(this);
    this.statAllocator.setOnAttributesChanged(() -> {
      if (this.screen != null) {
        this.screen.onAttributesChanged();
      }
    });

    this.screen = null;
    this.syncId = -1;
    this.dirtySyncId = true;
    this.receivedInventory = null;

    this.shouldParsePoints = false;

    this.state = State.IDLE;
  }

  public void openScreen(OpenScreenS2CPacket packet) {
    if (this.screen != null) {
      this.shouldParsePoints = true;
    }

    // Set the player's current screen handler to a 9x6 screen
    ClientPlayerEntity player = this.gameFeature.getMinecraftClient().player;
    if (player != null) {
      var screenHandler = ScreenHandlerType.GENERIC_9X6.create(packet.getSyncId(), player.getInventory());
      this.screen = new StatScreen(packet.getName(), this);
      player.currentScreenHandler = screenHandler;
      this.gameFeature.getMinecraftClient().setScreen(this.screen);
    }

    this.statAllocator.onScreenRecreate();
    this.setSyncId(packet.getSyncId());
  }

  public void receiveInventory(InventoryS2CPacket packet) {
    this.receivedInventory = packet.getContents();

    Map<String, PlayerAttribute> attributes = new HashMap<>();

    for (int slot = 0; slot < this.receivedInventory.size(); slot++) {
      // Only check the first 53 slots (6 rows of 9 slots)
      if (slot >= 6 * 9) {
        break;
      }

      ItemStack itemStack = this.receivedInventory.get(slot);

      BlockgameProfiles.LOGGER.info("[Blockgame Profiles] Received item: {}", itemStack.getName().getString());

      // Check if the item is the "Reallocate Attributes" item
      if (itemStack.getName().getString().equals("Reallocate Attributes")) {
        this.statAllocator.setReallocationSlot(slot);
        if (this.screen != null) {
          this.screen.onReallocationSlotSet();
        }
        this.parseReallocationItem(itemStack);
        continue;
      }

      PlayerAttribute attribute = PlayerAttribute.fromItem(itemStack, slot);
      if (attribute != null) {
        final int finalSlot = slot;
        attributes.compute(attribute.getName(), (key, value) -> {
          if (value == null) {
            return attribute;
          }

          value.getSlots().add(finalSlot);
          return value;
        });

        // If we haven't found the current points yet, try to find it
        if (this.statAllocator.getAvailablePoints() == -1 || this.shouldParsePoints) {
          this.statAllocator.setAvailablePoints(PlayerAttribute.getAvailablePoints(itemStack));
          this.shouldParsePoints = false;
        }
      }
    }

    this.statAllocator.setAttributes(attributes);
  }

  public void onScreenClose() {
    this.screen = null;

    // TODO: Do we want to reset sync id and inventory?
    BlockgameProfiles.LOGGER.info("[Blockgame Profiles] Screen closed");
  }

  public void invalidateScreen() {
    this.dirtySyncId = true;
  }

  public void setSyncId(int syncId) {
    this.syncId = syncId;
    this.dirtySyncId = false;

    if (this.state == State.WAITING_FOR_SYNC_ID) {
      this.changeState(State.IDLE);
      this.statAllocator.nextStep();
    }
  }

  public boolean waitingForScreen() {
    return this.dirtySyncId || this.syncId == -1 || this.screen == null;
  }

  public void changeState(State state) {
    this.state = state;
  }

  private void parseReallocationItem(ItemStack stack) {
    // Look for "You have spent a total of X attributes." in the lore
    NbtList loreList = NbtHelper.getLore(stack);
    if (loreList == null) {
      BlockgameProfiles.LOGGER.warn("No lore found for reallocation item");
      return;
    }

    for (int i = 0; i < loreList.size(); i++) {
      MutableText text = NbtHelper.parseLore(loreList, i);
      if (text == null) {
        continue;
      }

      String lore = text.getString();
      Pattern pattern = Pattern.compile("You have spent a total of (\\d+) attributes\\.");
      var matcher = pattern.matcher(lore);
      if (matcher.matches()) {
        this.statAllocator.setSpentPoints(Integer.parseInt(matcher.group(1)));
        return;
      }
    }
  }

  public enum State {
    IDLE,
    WAITING_FOR_RESET,
    WAITING_FOR_ALLOCATION,
    WAITING_FOR_SYNC_ID,
    WAITING_FOR_INVENTORY,
    CAPACITY_EXCEEDED,
    FAILED,
  }
}
