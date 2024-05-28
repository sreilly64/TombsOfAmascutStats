package rr.raids.tombsofamascutstats;

import com.attacktimer.AttackTimerMetronomePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TombsOfAmascutStatsTest {

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(TombsOfAmascutStatsPlugin.class);
        RuneLite.main(args);
    }
}
