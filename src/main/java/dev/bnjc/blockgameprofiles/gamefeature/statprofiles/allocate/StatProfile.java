package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bnjc.blockgameprofiles.gamefeature.statprofiles.attribute.PlayerAttribute;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@Getter
public class StatProfile {
  public static final Codec<StatProfile> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING
              .fieldOf("name")
              .forGetter((StatProfile profile) -> profile.name),
          Codec.unboundedMap(Codec.STRING, Codec.INT)
              .xmap(HashMap::new, Function.identity())
              .fieldOf("attributeAllocations")
              .forGetter((StatProfile profile) -> profile.attributeAllocations),
          Codec.INT
              .optionalFieldOf("order", 0)
              .forGetter((StatProfile profile) -> profile.order)
      ).apply(instance, (name, attributeAllocations, order) -> {
        StatProfile profile = new StatProfile(name, order);
        profile.attributeAllocations.putAll(attributeAllocations);
        return profile;
      })
  );

  private final String name;
  private final HashMap<String, Integer> attributeAllocations;
  private final int order;

  public StatProfile(String name) {
    this(name, 0);
  }

  public StatProfile(String name, int order) {
    this.name = name;
    this.order = order;
    this.attributeAllocations = new HashMap<>();
  }

  public void fromAttributes(Map<String, PlayerAttribute> attributes) {
    this.reset();

    for (Map.Entry<String, PlayerAttribute> entry : attributes.entrySet()) {
      int spent = entry.getValue().getSpent();
      if (spent > 0) {
        this.attributeAllocations.put(entry.getKey(), spent);
      }
    }
  }

  public Iterator<Command> buildCommands(@Nullable StatProfile currentProfile) {
    List<Command> commands = new ArrayList<>();

    var profileDiff = this.diffProfile(currentProfile);

    // If currentProfile is null or there is a negative diff, reset the profile
    boolean shouldReset = currentProfile == null || profileDiff.values().stream().anyMatch(i -> i < 0);
    if (shouldReset) {
      commands.add(Command.reset());
    }

    for (Map.Entry<String, Integer> entry : this.attributeAllocations.entrySet()) {
      int goal = entry.getValue();

      // If we don't reset, only add the difference to reduce the number of commands
      if (!shouldReset) {
        goal = profileDiff.getOrDefault(entry.getKey(), 0);
      }

      for (int i = 0; i < goal; i++) {
        commands.add(Command.increment(entry.getKey()));
      }
    }

    return commands.iterator();
  }

  public void reset() {
    this.attributeAllocations.clear();
  }

  public int getSpentPoints() {
    return this.attributeAllocations.values().stream().mapToInt(Integer::intValue).sum();
  }

  public boolean matches(@Nullable Map<String, PlayerAttribute> attributes) {
    if (attributes == null) {
      return false;
    }

    for (Map.Entry<String, PlayerAttribute> entry : attributes.entrySet()) {
      int spent = entry.getValue().getSpent();
      int goal = this.attributeAllocations.getOrDefault(entry.getKey(), 0);
      if (spent != goal) {
        return false;
      }
    }

    return true;
  }

  public static StatProfile of(String name, Map<String, PlayerAttribute> attributes) {
    StatProfile profile = new StatProfile(name);
    profile.fromAttributes(attributes);
    return profile;
  }

  private Map<String, Integer> diffProfile(@Nullable StatProfile other) {
    Map<String, Integer> diff = new HashMap<>();

    if (other == null) {
      return this.attributeAllocations;
    }

    Set<String> combinedKeys = new HashSet<>(this.attributeAllocations.keySet());
    combinedKeys.addAll(other.attributeAllocations.keySet());

    for (String key : combinedKeys) {
      int spent = this.attributeAllocations.getOrDefault(key, 0);
      int otherSpent = other.attributeAllocations.getOrDefault(key, 0);
      if (spent != otherSpent) {
        diff.put(key, spent - otherSpent);
      }
    }

    return diff;
  }
}
