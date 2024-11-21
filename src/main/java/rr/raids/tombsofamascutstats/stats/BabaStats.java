package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;
import rr.raids.tombsofamascutstats.stats.phases.BabaPhase;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class BabaStats extends RaidStats
{

    private Map<BabaPhase, String> phaseCompletionTimes; // key = phase name, value = phase completion time

    public BabaStats()
    {
        phaseCompletionTimes = new LinkedHashMap<>();
        for (BabaPhase phase: BabaPhase.values())
        {
            phaseCompletionTimes.put(phase, null);
        }
    }

    public void resetStats()
    {
        setStartTick(-1);
        setPreviousPhaseEndTick(-1);
        phaseCompletionTimes.replaceAll((k,v) -> null);
    }

    public String getSplitTimes()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Times:</br>");
        for (Map.Entry<BabaPhase, String> entry: phaseCompletionTimes.entrySet())
        {
            stringBuilder.append(entry.getKey().phaseName).append(entry.getValue()).append("</br>");
        }
        stringBuilder.delete(stringBuilder.lastIndexOf("</br>"), stringBuilder.length());
        return stringBuilder.toString();
//        return "Times:</br>" +
//                "Phase 1 - " + phase1CompletionTime +
//                "</br>" +
//                "Boulders 1 - " + boulder1CompletionTime +
//                "</br>" +
//                "Phase 2 - " + phase2CompletionTime +
//                "</br>" +
//                "Boulders 2 - " + boulder2CompletionTime +
//                "</br>" +
//                "Phase 3 - " + phase3CompletionTime +
//                "</br>" +
//                "Total - " + encounterCompletionTime;
    }

}
