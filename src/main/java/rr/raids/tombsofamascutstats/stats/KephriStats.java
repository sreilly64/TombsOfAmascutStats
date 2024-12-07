package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;
import rr.raids.tombsofamascutstats.stats.phases.KephriPhase;
import java.util.LinkedHashMap;
import java.util.Map;

import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.*;

@Getter
@Setter
public class KephriStats extends BossStats
{

    private boolean isFirstShieldDown = true;
    private int firstShieldDownHealing = 0;
    private int shieldTotalHealing = 0;
    private Map<KephriPhase, String> phaseCompletionTimes = new LinkedHashMap<>(); // key = phase name, value = phase completion time

    public KephriStats()
    {
        super();
    }

    @Override
    void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(KEPHRI);
        getEnemyNames().add(SCARABS);
    }

    @Override
    void initializePhaseCompletionTimesMap()
    {
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
    public void resetStats()
    {
        setStartTick(-1);
        setPreviousPhaseEndTick(-1);
        isFirstShieldDown = true;
        firstShieldDownHealing = 0;
        shieldTotalHealing = 0;
        initializePhaseCompletionTimesMap();
        initializeBossDamageMaps();
    }

    @Override
    public String getInfoBoxSplitTimesString()
    {
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

    @Override
    public String getInfoBoxBossDamageString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Damage Dealt:")
                .append("</br>Kephri - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(KEPHRI)))
                .append(" (").append(DECIMAL_FORMAT.format(getPercentageOfDamageDealt(KEPHRI))).append("%)")
                .append("</br>Scarabs - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(SCARABS))).append(" (").append(DECIMAL_FORMAT.format(getPercentageOfDamageDealt(SCARABS))).append("%)")
                .append("</br>Total Damage - ")
                .append(DMG_FORMAT.format(getTotalPersonalDamageDealt()));
        return stringBuilder.toString();
    }

    public String getInfoBoxHealingStatsString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Boss Healing:")
                .append("</br>First down healed - ")
                .append(DMG_FORMAT.format(firstShieldDownHealing))
                .append("</br>Second down healed - ")
                .append(DMG_FORMAT.format(getSecondShieldDownHealing()))
                .append("</br>Total shield healed - ")
                .append(DMG_FORMAT.format(shieldTotalHealing));
        return stringBuilder.toString();
    }
}
