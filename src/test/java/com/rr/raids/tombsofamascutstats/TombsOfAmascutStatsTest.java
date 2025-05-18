package com.rr.raids.tombsofamascutstats;

import com.attacktimer.AttackTimerMetronomePlugin;
import com.rr.bosses.yama.YamaUtilitiesPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TombsOfAmascutStatsTest {

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(TombsOfAmascutStatsPlugin.class, AttackTimerMetronomePlugin.class, YamaUtilitiesPlugin.class);
        RuneLite.main(args);
    }
}
