package com.piggyplugins.JadAutoPrayers;

import com.piggyplugins.EthanApiPlugin.Collections.NPCs;
import com.piggyplugins.EthanApiPlugin.EthanApiPlugin;
import com.piggyplugins.InteractionApi.PrayerInteraction;
import com.piggyplugins.PacketUtils.PacketUtilsPlugin;
import com.piggyplugins.Packets.MousePackets;
import com.piggyplugins.Packets.MovementPackets;
import com.piggyplugins.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PluginDescriptor(
        name = "Jad Flicker",
        description = "Automatically switches & flicks prayer at Jad (multiple jads not supported)",
        tags = {"ethan", "piggy"}
)
@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@Slf4j
public class JadAutoPrayersPlugin extends Plugin {

    @Inject
    private Client client;
    @Inject
    private MousePackets mousePackets;
    @Inject
    private WidgetPackets widgetPackets;
    @Inject
    private EthanApiPlugin api;
    @Inject
    private MovementPackets movementPackets;
    @Inject
    private JadAutoPrayersConfig config;

    private Prayer shouldPray = Prayer.PROTECT_FROM_MELEE;
    private final List<Prayer> extraPrayerList = new ArrayList<>();
    private int ticksSinceAnimation = 0;

    @Provides
    private JadAutoPrayersConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(JadAutoPrayersConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!inFight()) {
            return;
        }

        ticksSinceAnimation++;

        if (ticksSinceAnimation > 3) {
            // back to melee cus healers? :shrug:
            shouldPray = Prayer.PROTECT_FROM_MELEE;
        }

        if (config.eagleEye() || config.rigour()) {
            oneTickMultiFlick();
        } else {
            oneTickFlick();
        }
    }

    private void oneTickFlick() {
        if (PrayerInteraction.isPrayerActive(shouldPray)) {
            PrayerInteraction.togglePrayer(shouldPray);
        }
        PrayerInteraction.togglePrayer(shouldPray);
    }

    private void oneTickMultiFlick() {
        if (config.eagleEye()) {
            if (PrayerInteraction.isPrayerActive(shouldPray)) {
                PrayerInteraction.toggleMultiplePrayers(shouldPray, Prayer.EAGLE_EYE);
            }
            PrayerInteraction.toggleMultiplePrayers(shouldPray, Prayer.EAGLE_EYE);
            return;
        }

        if (config.rigour()) {
            if (PrayerInteraction.isPrayerActive(shouldPray)) {
                PrayerInteraction.toggleMultiplePrayers(shouldPray, Prayer.RIGOUR);
            }
            PrayerInteraction.toggleMultiplePrayers(shouldPray, Prayer.RIGOUR);
        }
    }


    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        this.setupJadPrayers();
    }

    private void setupJadPrayers() {
        Optional<NPC> tztokJad = NPCs.search().withName("TzTok-Jad").first();
        Optional<NPC> jaltokJad = NPCs.search().withName("JalTok-Jad").first();
        NPC jad;
        if (tztokJad.isPresent()) {
            jad = tztokJad.get();
        } else if (jaltokJad.isPresent()) {
            jad = jaltokJad.get();
        } else {
            return;
        }

        int animationID = EthanApiPlugin.getAnimation(jad);
        if (animationID == 2655 || animationID == 7592) {
            ticksSinceAnimation = 0;
            shouldPray = Prayer.PROTECT_FROM_MAGIC;
        } else if (animationID == 2652 || animationID == 7593) {
            ticksSinceAnimation = 0;
            shouldPray = Prayer.PROTECT_FROM_MISSILES;
        }
    }

    private boolean inFight() {
        return NPCs.search().withName("TzTok-Jad").first().isPresent()
                || NPCs.search().withName("JalTok-Jad").first().isPresent();
    }
}
