package com.rr.raids.tombsofamascutstats.stats;

import com.rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin;
import com.rr.raids.tombsofamascutstats.stats.phases.KephriPhase;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class KephriStats extends BossStats
{

    private boolean isFirstShieldDown = true;
    private int firstShieldDownHealing = 0;
    private int shieldTotalHealing = 0;
    private Map<KephriPhase, String> phaseCompletionTimes; // key = phase name, value = phase completion time

    public KephriStats()
    {
        super();
        phaseCompletionTimes = new LinkedHashMap<>();
    }

    public void addToKephriShieldTotalHealing(int amount)
    {
        shieldTotalHealing += amount;
    }

    public int getSecondShieldDownHealing()
    {
        return shieldTotalHealing - firstShieldDownHealing;
    }

    public String getInfoBoxHealingStatsString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Boss Healing:")
                .append("</br>First down healed - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(firstShieldDownHealing))
                .append("</br>Second down healed - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getSecondShieldDownHealing()))
                .append("</br>Total shield healed - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(shieldTotalHealing));
        return stringBuilder.toString();
    }

    @Override
    void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(TombsOfAmascutStatsPlugin.KEPHRI);
        getEnemyNames().add(TombsOfAmascutStatsPlugin.SCARABS);
    }

    @Override
    void initializePhaseCompletionTimes()
    {
        if (phaseCompletionTimes == null)
        {
            phaseCompletionTimes = new LinkedHashMap<>();
        }
        for (KephriPhase phase: KephriPhase.values())
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
        isFirstShieldDown = true;
        firstShieldDownHealing = 0;
        shieldTotalHealing = 0;
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
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getPersonalDamage().get(TombsOfAmascutStatsPlugin.KEPHRI)))
                .append(" (").append(TombsOfAmascutStatsPlugin.DECIMAL_FORMAT.format(getPercentageOfDamageDealt(TombsOfAmascutStatsPlugin.KEPHRI))).append("%)")
                .append("</br>Scarabs - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getPersonalDamage().get(TombsOfAmascutStatsPlugin.SCARABS))).append(" (").append(TombsOfAmascutStatsPlugin.DECIMAL_FORMAT.format(getPercentageOfDamageDealt(TombsOfAmascutStatsPlugin.SCARABS))).append("%)")
                .append("</br>Total Damage - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getTotalPersonalDamageDealt()));
        return stringBuilder.toString();
    }
}
