package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier;

import lombok.Getter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

@Getter
public class StatModifier {
  private static final Pattern LORE_PATTERN = Pattern.compile(".*([+-][0-9.]+)(%?) (.+) \\(([+-]?[0-9.]+)(%?)\\).*");

  private final String name;
  private final double value;
  private final double playerValue;
  private final ModifierType type;
  private final ModifierCategory category;

  public StatModifier(String name, double value, double playerValue, ModifierType type) {
    this.name = name;
    this.value = value;
    this.playerValue = playerValue;
    this.type = type;
    this.category = determineCategory(name);
  }

  public MutableText toLore(int count) {
    double newPlayerValue = value * count;
    //   +1.0% Critical Strike Chance (+0.5%)
    return Text.literal("  ").formatted(Formatting.GRAY)
        .append(value > 0 ? "+" : "")
        .append(String.format("%.1f", value))
        .append(type == ModifierType.RELATIVE ? "%" : "")
        .append(" " + name)
        .append(" (")
        .append(Text.literal(newPlayerValue > 0 ? "+" : "")
            .append(String.format("%.1f", newPlayerValue))
            .append(type == ModifierType.RELATIVE ? "%" : "")
            .formatted(playerValue > 0 ? Formatting.GREEN : (playerValue < 0 ? Formatting.RED : Formatting.WHITE))
        )
        .append(")");
  }

  public static @Nullable StatModifier fromLore(String lore) {
    if (lore.isEmpty()) {
      return null;
    }

    // Regex to match the value and name
    // Example: "  +0.5% Critical Strike Chance (+1.5%)"
    // - value: 0.5
    // - name: "Critical Strike Chance"
    // - playerValue: 1.5
    // - type: RELATIVE
    var matcher = LORE_PATTERN.matcher(lore);
    if (!matcher.matches()) {
      return null;
    }

    String name = matcher.group(3);
    double value = Double.parseDouble(matcher.group(1));
    double playerValue = Double.parseDouble(matcher.group(4));

    ModifierType type = ModifierType.FLAT;
    if (matcher.group(2).equals("%")) {
      type = ModifierType.RELATIVE;
    }

    return new StatModifier(name, value, playerValue, type);
  }

  private ModifierCategory determineCategory(String name) {
    return switch (name) {
      case "Critical Strike Chance", "Critical Strike Power", "All Damage", "Weapon Damage", "Magic Damage",
           "Projectile Damage", "Thaumaturgy Power", "Backstab Damage", "PVE Damage", "PVP Damage",
           "AOE Size Amplifier", "Cooldown Reduction", "Movement Speed", "Threat" -> ModifierCategory.OFFENSE;
      case "Max Health", "Health Regeneration", "Healing Received", "Defense", "Block Rating", "Block Power",
           "Block Cooldown Reduction", "Knockback Resistance", "Damage Reduction", "PVE Damage Reduction",
           "PVP Damage Reduction" -> ModifierCategory.DEFENSE;
      case "Mining Skill", "Logging Skill", "Archaeology Skill", "Fishing Skill", "Herbalism Skill",
           "Hunting Skill" -> ModifierCategory.PROFESSION;
      default -> ModifierCategory.UNKNOWN;
    };
  }

  @Override
  public String toString() {
    return "StatModifier{" +
        "name='" + name + '\'' +
        ", value=" + value +
        ", playerValue=" + playerValue +
        ", type=" + type +
        ", category=" + category +
        '}';
  }
}
