package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;
import rr.raids.tombsofamascutstats.stats.phases.KephriPhase;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class KephriStats extends RaidStats
{

    private Map<KephriPhase, String> phaseCompletionTimes; // key = phase name, value = phase completion time
    private boolean isFirstShieldDown = true;
    private int firstShieldDownHealing = 0;
    private int shieldTotalHealing = 0;

    public KephriStats()
    {
        initializePhaseCompletionTimeMap();
    }

    private void initializePhaseCompletionTimeMap()
    {
        phaseCompletionTimes = new LinkedHashMap<>();
        for (KephriPhase phase: KephriPhase.values())
        {
            phaseCompletionTimes.put(phase, null);
        }
    }

    public void addToKephriShieldTotalHealing(int amount)
    {
        shieldTotalHealing += amount;
    }

    public int getSecondShieldDownHealing()
    {
        return shieldTotalHealing - firstShieldDownHealing;
    }

    @Override
    public void resetStats() {
        setStartTick(-1);
        setPreviousPhaseEndTick(-1);
        isFirstShieldDown = true;
        firstShieldDownHealing = 0;
        shieldTotalHealing = 0;
        phaseCompletionTimes.replaceAll((k,v) -> null);
    }

    @Override
    public String getSplitTimes() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Times:</br>");
        for (Map.Entry<KephriPhase, String> entry: phaseCompletionTimes.entrySet())
        {
            String phaseName = entry.getKey().phaseName;
            String phaseTime = entry.getValue();
            stringBuilder.append(phaseName).append(phaseTime).append("</br>");
        }
        stringBuilder.delete(stringBuilder.lastIndexOf("</br>"), stringBuilder.length());
        return stringBuilder.toString();
    }
}
