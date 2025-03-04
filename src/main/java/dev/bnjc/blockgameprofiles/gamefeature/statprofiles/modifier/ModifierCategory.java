package dev.bnjc.blockgameprofiles.gamefeature.statprofiles.modifier;

public enum ModifierCategory {
  OFFENSE("Offense"),
  DEFENSE("Defense"),
  PROFESSION("Profession"),
  UNKNOWN("Other");

  public final String title;

  ModifierCategory(String title) {
    this.title = title;
  }
}