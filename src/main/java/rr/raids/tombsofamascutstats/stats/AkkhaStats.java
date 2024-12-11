package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;
import rr.raids.tombsofamascutstats.stats.phases.AkkhaPhase;
import java.util.LinkedHashMap;
import java.util.Map;

import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.*;

@Getter
@Setter
public class AkkhaStats extends BossStats
{

    private Map<AkkhaPhase, String> phaseCompletionTimes; // key = phase name, value = phase completion time

    public AkkhaStats()
    {
        super();
        phaseCompletionTimes = new LinkedHashMap<>();
    }

    @Override
    void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(AKKHA);
        getEnemyNames().add(AKKHAS_SHADOW);
    }

    @Override
    void initializePhaseCompletionTimes()
    {
        if (phaseCompletionTimes == null)
        {
            phaseCompletionTimes = new LinkedHashMap<>();
        }
        for (AkkhaPhase phase: AkkhaPhase.values())
        {
            phaseCompletionTimes.put(phase, null);
        }
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
        stringBuilder.append("Times:</br>");
        for (Map.Entry<AkkhaPhase, String> entry: phaseCompletionTimes.entrySet())
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
        stringBuilder.append("</br>Damage Dealt:")
                .append("</br>Akkha - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(AKKHA)))
                .append(" (").append(DECIMAL_FORMAT.format(getPercentageOfDamageDealt(AKKHA))).append("%)")
                .append("</br>Akkha's Shadows - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(AKKHAS_SHADOW))).append(" (").append(DECIMAL_FORMAT.format(getPercentageOfDamageDealt(AKKHAS_SHADOW))).append("%)")
                .append("</br>Total Damage - ")
                .append(DMG_FORMAT.format(getTotalPersonalDamageDealt()));
        return stringBuilder.toString();
    }
}
