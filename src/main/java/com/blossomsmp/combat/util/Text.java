package com.blossomsmp.combat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {

    // Understands &a style colours and &#FF69B4 hex colours
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    private Text() {
    }

    public static Component color(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(text);
    }
}
