package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;
import rr.raids.tombsofamascutstats.stats.phases.BabaPhase;
import java.util.LinkedHashMap;
import java.util.Map;

import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.BABA;
import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.DMG_FORMAT;

@Getter
@Setter
public class BabaStats extends BossStats
{

    private Map<BabaPhase, String> phaseCompletionTimes = new LinkedHashMap<>(); // key = phase name, value = phase completion time

    public BabaStats()
    {
        initializePhaseCompletionTimesMap();
        initializeSetOfEnemyNames();
        initializeBossDamageMaps();
    }

    private void initializeBossDamageMaps()
    {
        for (String enemyName: getEnemyNames())
        {
            getPersonalDamage().put(enemyName, 0);
            getTotalDamage().put(enemyName, 0);
        }
    }

    private void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(BABA);
    }

    private void initializePhaseCompletionTimesMap()
    {
        for (BabaPhase phase: BabaPhase.values())
        {
            phaseCompletionTimes.put(phase, null);
        }
    }

    @Override
    public void resetStats()
    {
        setStartTick(-1);
        setPreviousPhaseEndTick(-1);
        initializePhaseCompletionTimesMap();
        initializeBossDamageMaps();
    }

    @Override
    public String getInfoBoxSplitTimesString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Times:</br>");
        for (Map.Entry<BabaPhase, String> entry: phaseCompletionTimes.entrySet())
        {
            String phaseName = entry.getKey().phaseName;
            String phaseTime = entry.getValue();
            stringBuilder.append(phaseName).append(phaseTime).append("</br>");
        }
        stringBuilder.delete(stringBuilder.lastIndexOf("</br>"), stringBuilder.length());
        return stringBuilder.toString();
    }

    @Override
    public String getInfoBoxBossDamageString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Damage Dealt:</br>Ba-Ba - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(BABA)));

        return stringBuilder.toString();
    }

}
