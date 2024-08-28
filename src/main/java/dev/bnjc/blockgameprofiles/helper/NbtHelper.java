package dev.bnjc.blockgameprofiles.helper;

import dev.bnjc.blockgameprofiles.BlockgameProfiles;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NbtHelper {
  public static @Nullable NbtList getLore(ItemStack stack) {
    NbtCompound tag = stack.getNbt();
    if (tag == null) {
      return null;
    }

    if (!tag.contains(ItemStack.DISPLAY_KEY, NbtElement.COMPOUND_TYPE)) {
      return null;
    }

    NbtCompound display = tag.getCompound(ItemStack.DISPLAY_KEY);
    if (!display.contains(ItemStack.LORE_KEY, NbtElement.LIST_TYPE)) {
      return null;
    }

    return display.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
  }

  public static @Nullable MutableText parseLore(NbtList lore, int line) {
    if (line < 0 || line >= lore.size()) {
      return null;
    }

    return parseLoreLine(lore.getString(line));
  }

  public static @Nullable MutableText parseLoreLine(String line) {
    if (line.isEmpty()) {
      return null;
    }

    try {
      return Text.Serializer.fromJson(line);
    } catch (Exception e) {
      BlockgameProfiles.LOGGER.warn("Failed to parse lore line: {}", line, e);
      return null;
    }
  }

  public static NbtList buildLore(List<MutableText> lines) {
    NbtList lore = new NbtList();
    for (MutableText line : lines) {
      lore.add(NbtString.of(Text.Serializer.toJson(line)));
    }
    return lore;
  }
}
