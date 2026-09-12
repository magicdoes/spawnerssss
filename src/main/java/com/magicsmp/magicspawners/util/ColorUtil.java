package com.magicsmp.magicspawners.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private ColorUtil() {}
    public static Component component(String input) { return LEGACY.deserialize(input == null ? "" : input); }
}
