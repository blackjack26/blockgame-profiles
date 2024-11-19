package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute;

import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier.StatModifier;
import dev.bnjc.blockgameprofiles.helper.NbtHelper;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public class PlayerAttribute {
  private static final Pattern POINTS_PATTERN = Pattern.compile("Points Spent: (\\d+)/(\\d+)");
  private static final Pattern COST_PATTERN = Pattern.compile("Click to level up for (\\d+) attribute point.*");
  private static final Pattern CURRENT_POINTS_PATTERN = Pattern.compile(".*Current Attribute Points: (\\d+)");

  private final String name;
  private final int max;
  private final int spent;
  private final Set<StatModifier> buffs;
  private final int cost;
  private final HashSet<Integer> slots;
  private final ItemStack itemStack;
  private final AttributeCategory category;

  public PlayerAttribute(String name, int max, int spent, Set<StatModifier> buffs, int cost, int slot, ItemStack itemStack) {
    this(name, max, spent, buffs, cost, new HashSet<>(List.of(slot)), itemStack);
  }

  public PlayerAttribute(String name, int max, int spent, Set<StatModifier> buffs, int cost, HashSet<Integer> slots, ItemStack itemStack) {
    this.name = name;
    this.max = max;
    this.spent = spent;
    this.buffs = buffs;
    this.cost = cost;
    this.slots = slots;
    this.itemStack = itemStack;
    this.category = determineCategory(name);

    // Adjust ItemStack max damage
//    itemStack.getItem().maxDamage = max;
//    itemStack.setDamage(max - spent);
//    itemStack.setCount(spent);
  }

  public PlayerAttribute reset() {
    return this.withSpent(0);
  }

  public PlayerAttribute adjust(int spentAmount) {
    if (this.spent + spentAmount < 0 || this.spent + spentAmount > this.max) {
      return this;
    }

    return new PlayerAttribute(this.name, this.max, this.spent + spentAmount, this.buffs, this.cost, this.slots, this.itemStack);
  }

  public PlayerAttribute withSpent(int spent) {
    return new PlayerAttribute(this.name, this.max, spent, this.buffs, this.cost, this.slots, this.itemStack);
  }

  public int getFirstSlot() {
    return slots.stream().findFirst().orElse(-1);
  }

  /**
   * When in preview mode, the lore text does not correctly display the current spent points, or modifiers. This
   * method fixes the lore text to display the correct values.
   */
  public void fixLoreText() {
    List<MutableText> loreList = new ArrayList<>();

    loreList.add(Text.literal(""));
    loreList.add(
        Text.literal("Points Spent: ")
            .formatted(Formatting.GRAY)
            .append(Text.literal("" + this.spent).formatted(Formatting.GOLD))
            .append(Text.literal("/").formatted(Formatting.GRAY))
            .append(Text.literal("" + this.max).formatted(Formatting.GOLD))
    );
    loreList.add(Text.literal(""));
    loreList.add(Text.literal("When Leveled Up:").formatted(Formatting.DARK_GRAY));

    for (StatModifier buff : this.buffs) {
      loreList.add(buff.toLore(this.spent));
    }

    loreList.add(Text.literal(""));
    loreList.add(Text.literal("Left-click to level up by  " + this.cost + ".").formatted(Formatting.YELLOW));
    loreList.add(Text.literal("Right-click to level down by " + this.cost + ".").formatted(Formatting.YELLOW));

    // Remove italics
    for (MutableText lore : loreList) {
      lore.styled(style -> style.withItalic(false));
    }

    itemStack.getOrCreateSubNbt("display").put("Lore", NbtHelper.buildLore(loreList));
  }

  public int getBarStep(float barWidth) {
    return Math.round((float)spent * barWidth / (float)max);
  }

  public static @Nullable PlayerAttribute fromItem(ItemStack stack, int slot) {
    if (stack == null || stack.getItem() == Items.AIR) {
      return null;
    }

    NbtList loreList = NbtHelper.getLore(stack);
    if (loreList == null) {
      return null;
    }

    String name = stack.getName().getString();
    if ("Profile".equals(name)) {
      return null;
    }

    int max = -1;
    int spent = -1;
    int cost = -1;
    Set<StatModifier> buffs = new HashSet<>();

    boolean inBuffs = false;
    for (int i = 0; i < loreList.size(); i++) {
      MutableText loreText = NbtHelper.parseLore(loreList, i);
      if (loreText == null) {
        continue;
      }

      String loreString = loreText.getString();
      if (loreString.isEmpty()) {
        inBuffs = false;
        continue;
      }

      // Line matching "Points Spent: 1/10" is the max and spent values
      var matcher = POINTS_PATTERN.matcher(loreString);
      if (matcher.matches()) {
        spent = Integer.parseInt(matcher.group(1));
        max = Integer.parseInt(matcher.group(2));
        continue;
      }

      // Buffs start with a "When Leveled Up:" line
      if (loreString.startsWith("When Leveled Up:")) {
        inBuffs = true;
        continue;
      }

      // Buffs are in the format of "  +1.0% Critical Strike Chance (+1%)"
      if (inBuffs) {
        StatModifier buff = StatModifier.fromLore(loreString);
        if (buff != null) {
          buffs.add(buff);
          continue;
        }
      }

      // Get cost from "Click to level up for 1 attribute point."
      matcher = COST_PATTERN.matcher(loreString);
      if (matcher.matches()) {
        cost = Integer.parseInt(matcher.group(1));
        continue;
      }
    }

    return new PlayerAttribute(name, max, spent, buffs, cost, slot, stack);
  }

  public static int getAvailablePoints(ItemStack stack) {
    NbtList loreList = NbtHelper.getLore(stack);
    if (loreList == null) {
      return -1;
    }

    // Start from the end of the lore list to find the most recent "Current Attribute Points: X" line
    for (int i = loreList.size() - 1; i >= 0; i--) {
      MutableText loreText = NbtHelper.parseLore(loreList, i);
      if (loreText == null) {
        continue;
      }

      String loreString = loreText.getString();
      var matcher = CURRENT_POINTS_PATTERN.matcher(loreString);
      if (matcher.matches()) {
        return Integer.parseInt(matcher.group(1));
      }
    }

    return -1;
  }

  private AttributeCategory determineCategory(String name) {
    return switch (name) {
      case "Ferocity", "Spartan", "Dexterity", "Alacrity", "Intelligence", "Precision", "Glass Cannon",
           "Bravery", "Bullying", "Assassin" -> AttributeCategory.OFFENSE;
      case "Wisdom", "Pacifist", "Volition", "Somatics", "Bloom", "Time Lord", "Beef Cake", "Chunky Soup" -> AttributeCategory.SUPPORT;
      case "Tenacity", "Shield Mastery", "Fortress", "Juggernaut", "Battleship", "Dreadnought" -> AttributeCategory.DEFENSE;
      default -> AttributeCategory.OTHER;
    };
  }

  public PlayerAttribute copy() {
    return new PlayerAttribute(
        this.name,
        this.max,
        this.spent,
        new HashSet<>(this.buffs),
        this.cost,
        new HashSet<>(this.slots),
        this.itemStack.copy()
    );
  }
}