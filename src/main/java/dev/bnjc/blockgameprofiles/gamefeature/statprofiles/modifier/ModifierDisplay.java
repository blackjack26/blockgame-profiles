package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier;

import lombok.Getter;
import net.minecraft.util.Formatting;

@Getter
public class ModifierDisplay {
  private final String name;
  private final ModifierType type;
  private final ModifierCategory category;
  private final int order;
  private final String icon;

  private double value;

  public ModifierDisplay(String name, ModifierType type, ModifierCategory category) {
    this.name = name;
    this.type = type;
    this.category = category;
    this.order = ModifierDisplay.getOrder(name);
    this.icon = ModifierDisplay.getIcon(name);

    this.value = 0;
  }

  public void addValue(double value) {
    this.value += value;
  }

  public String getDisplayName() {
    return switch (name) {
      case "Critical Strike Chance" -> "Critical Chance";
      case "Critical Strike Power" -> "Critical Power";
      case "Health Regeneration" -> "Health Regen";
      default -> name;
    };
  }

  public Formatting getValueColor() {
    if (value > 0) {
      return Formatting.GREEN;
    } else if (value < 0) {
      return Formatting.RED;
    } else {
      return Formatting.WHITE;
    }
  }

  private static String getIcon(String name) {
    return switch (name) {
      case "Critical Strike Chance", "Critical Strike Power", "All Damage", "Weapon Damage", "Backstab Damage",
           "PVE Damage", "PVP Damage" -> "\uD83D\uDDE1";
      case "Magic Damage" -> "☄";
      case "Projectile Damage" -> "\uD83C\uDFF9";
      case "Thaumaturgy Power" -> "☮";
      case "AOE Size Amplifier" -> "◎";
      case "Cooldown Reduction" -> "⏳";
      case "Movement Speed" -> "⌚";
      case "Max Health", "Health Regeneration", "Healing Received" -> "❤";
      case "Defense", "Block Cooldown Reduction", "Block Power", "Block Rating", "Knockback Resistance",
           "Damage Reduction", "PVE Damage Reduction", "PVP Damage Reduction" -> "⛨";
      case "Threat" -> "⚠";
      case "Mining Skill" -> "⛏";
      case "Logging Skill" -> "🪓";
      case "Archaeology Skill" -> "☠";
      case "Fishing Skill" -> "🎣";
      case "Herbalism Skill" -> "☘";
      case "Hunting Skill" -> "🐺";
      default -> "?";
    };
  }

  private static int getOrder(String name) {
    return switch (name) {
      case "Critical Strike Chance" -> 0;
      case "Critical Strike Power" -> 1;
      case "All Damage" -> 2;
      case "Weapon Damage" -> 3;
      case "Magic Damage" -> 4;
      case "Projectile Damage" -> 5;
      case "Thaumaturgy Power" -> 6;
      case "Backstab Damage" -> 7;
      case "PVE Damage" -> 8;
      case "PVP Damage" -> 9;
      case "AOE Size Amplifier" -> 10;
      case "Cooldown Reduction" -> 11;
      case "Movement Speed" -> 12;
      case "Threat" -> 13;
      case "Max Health" -> 14;
      case "Health Regeneration" -> 15;
      case "Healing Received" -> 16;
      case "Defense" -> 17;
      case "Block Rating" -> 18;
      case "Block Power" -> 19;
      case "Block Cooldown Reduction" -> 20;
      case "Knockback Resistance" -> 21;
      case "Damage Reduction" -> 22;
      case "PVE Damage Reduction" -> 23;
      case "PVP Damage Reduction" -> 24;
      case "Mining Skill" -> 25;
      case "Logging Skill" -> 26;
      case "Archaeology Skill" -> 27;
      case "Fishing Skill" -> 28;
      case "Herbalism Skill" -> 29;
      case "Hunting Skill" -> 30;
      default -> -1;
    };
  }
}
