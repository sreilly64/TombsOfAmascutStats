package com.rr.raids.tombsofamascutstats.stats;

import com.rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirstWardensStats extends BossStats
{

    private String totalCompletionTime;
    private boolean elidnisWardenFought;

    public FirstWardensStats()
    {
        super();
    }

    @Override
    void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(TombsOfAmascutStatsPlugin.TUMEKENS_WARDEN_SHIELDED);
        getEnemyNames().add(TombsOfAmascutStatsPlugin.ELIDINIS_WARDEN_SHIELDED);
        getEnemyNames().add(TombsOfAmascutStatsPlugin.CORE);
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
        elidnisWardenFought = false;
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
        String wardensName = isElidnisWardenFought() ? TombsOfAmascutStatsPlugin.ELIDINIS_WARDEN : TombsOfAmascutStatsPlugin.TUMEKENS_WARDEN;
        String shieldedWardensName = wardensName.equals(TombsOfAmascutStatsPlugin.ELIDINIS_WARDEN) ? TombsOfAmascutStatsPlugin.ELIDINIS_WARDEN_SHIELDED : TombsOfAmascutStatsPlugin.TUMEKENS_WARDEN_SHIELDED;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Damage Dealt:")
                .append("</br>").append(wardensName).append(" - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getPersonalDamage().get(shieldedWardensName)))
                .append(" (").append(TombsOfAmascutStatsPlugin.DECIMAL_FORMAT.format(getPercentageOfDamageDealt(shieldedWardensName))).append("%)")
                .append("</br>Core - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getPersonalDamage().get(TombsOfAmascutStatsPlugin.CORE)))
                .append(" (").append(TombsOfAmascutStatsPlugin.DECIMAL_FORMAT.format(getPercentageOfDamageDealt(TombsOfAmascutStatsPlugin.CORE))).append("%)")
                .append("</br>Total Damage - ")
                .append(TombsOfAmascutStatsPlugin.DMG_FORMAT.format(getTotalPersonalDamageDealt()));
        return stringBuilder.toString();
    }
}
