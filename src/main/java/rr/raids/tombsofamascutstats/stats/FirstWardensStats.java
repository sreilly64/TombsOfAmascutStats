package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;

import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.*;

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
        getEnemyNames().add(TUMEKENS_WARDEN_SHIELDED);
        getEnemyNames().add(ELIDINIS_WARDEN_SHIELDED);
        getEnemyNames().add(CORE);
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
        String wardensName = isElidnisWardenFought() ? ELIDINIS_WARDEN : TUMEKENS_WARDEN;
        String shieldedWardensName = wardensName.equals(ELIDINIS_WARDEN) ? ELIDINIS_WARDEN_SHIELDED : TUMEKENS_WARDEN_SHIELDED;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</br>Damage Dealt:")
                .append("</br>").append(wardensName).append(" - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(shieldedWardensName)))
                .append(" (").append(DECIMAL_FORMAT.format(getPercentageOfDamageDealt(shieldedWardensName))).append("%)")
                .append("</br>Core - ")
                .append(DMG_FORMAT.format(getPersonalDamage().get(CORE))).append(" (").append(DECIMAL_FORMAT.format(getPercentageOfDamageDealt(CORE))).append("%)")
                .append("</br>Total Damage - ")
                .append(DMG_FORMAT.format(getTotalPersonalDamageDealt()));
        return stringBuilder.toString();
    }
}
