package com.github.nalamodikk.narasystem.nara.hud;

import net.minecraft.network.chat.Component;

public record NaraChoice(Component label, Runnable onSelect) {}
