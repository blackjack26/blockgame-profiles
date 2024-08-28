package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.allocate;

import org.jetbrains.annotations.Nullable;

public record Command(Type type, @Nullable String attrName) {
  public boolean isReset() {
    return this.type == Type.RESET;
  }

  public boolean isIncrement() {
    return this.type == Type.INCREMENT;
  }

  public static Command reset() {
    return new Command(Type.RESET, null);
  }

  public static Command increment(String attrName) {
    return new Command(Type.INCREMENT, attrName);
  }

  public enum Type {
    RESET,
    INCREMENT
  }
}
