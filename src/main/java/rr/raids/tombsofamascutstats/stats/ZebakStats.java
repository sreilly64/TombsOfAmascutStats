package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;

import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.*;

@Getter
@Setter
public class ZebakStats extends BossStats
{

    private String totalCompletionTime;

    public ZebakStats()
    {
        super();
    }

    @Override
    void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(ZEBAK);
    }

    @Override
    void initializePhaseCompletionTimes()
    {
        totalCompletionTime = null;
    }

    @Override
    public void resetStats()
    {
        setStartTick(-1);
        setPreviousPhaseEndTick(-1);
        initializePhaseCompletionTimes();
        initializeBossDamageMaps();
    }

    @Override
    public String getInfoBoxSplitTimesString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Times:</br>")
                .append("Total - ")
                .append(totalCompletionTime);
        return stringBuilder.toString();
    }

    @Override
    public String getInfoBoxBossDamageString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Damage Dealt:</br>Zebak - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(ZEBAK)));
        return stringBuilder.toString();
    }
}
