package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;
import rr.raids.tombsofamascutstats.stats.phases.SecondWardensPhase;
import java.util.LinkedHashMap;
import java.util.Map;

import static rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin.*;

@Getter
@Setter
public class SecondWardensStats extends BossStats
{

    private boolean wardensEnrageHeal = false;
    private boolean energySiphonsKilled = false;
    private boolean elidnisWardenFought;
    private int energySiphonBossDamage;
    private int preEnragePersonalDamage;
    private int preEnrageTotalDamage;
    private LocalPoint lastEnergySiphonPosition;
    private Map<SecondWardensPhase, String> phaseCompletionTimes = new LinkedHashMap<>(); // key = phase name, value = phase completion time


    public SecondWardensStats()
    {
        super();
    }

    public void addToEnergySiphonBossDamage(int amount)
    {
        if (amount < 0)
        {
            return;
        }
        energySiphonBossDamage += amount;
    }

    public int getEnragePhasePersonalDamageDealt()
    {
        String wardensName = isElidnisWardenFought() ? ELIDINIS_WARDEN : TUMEKENS_WARDEN;
        return getPersonalDamage().get(wardensName) - preEnragePersonalDamage;
    }

    public int getEnragePhaseTotalDamageDealt()
    {
        String wardensName = isElidnisWardenFought() ? ELIDINIS_WARDEN : TUMEKENS_WARDEN;
        return getTotalDamage().get(wardensName) - preEnrageTotalDamage;
    }

    public double getPercentageOfEnragePhaseDamageDealt()
    {
        return (double) getEnragePhasePersonalDamageDealt() / getEnragePhaseTotalDamageDealt() *  100;
    }

    public double getPercentageOfPreEnragePhaseDamageDealt()
    {
        return (double) preEnragePersonalDamage / preEnrageTotalDamage * 100;
    }

    public double getPercentageOfEnergySiphonDamage()
    {
        return (double) energySiphonBossDamage / getTotalDamageDealt() * 100;
    }

    @Override
    void initializeSetOfEnemyNames()
    {
        getEnemyNames().add(ELIDINIS_WARDEN);
        getEnemyNames().add(TUMEKENS_WARDEN);
    }

    @Override
    void initializePhaseCompletionTimes()
    {
        for (SecondWardensPhase phase: SecondWardensPhase.values())
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
        wardensEnrageHeal = false;
        energySiphonsKilled = false;
        elidnisWardenFought = false;
        energySiphonBossDamage = 0;
        preEnragePersonalDamage = 0;
        preEnrageTotalDamage = 0;
        lastEnergySiphonPosition = null;
    }

    @Override
    public String getInfoBoxSplitTimesString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Times:</br>");
        for (Map.Entry<SecondWardensPhase, String> entry: phaseCompletionTimes.entrySet())
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
                .append("</br>Energy Siphon damage - ")
                .append(DMG_FORMAT.format(energySiphonBossDamage))
                .append(" (").append(DECIMAL_FORMAT.format(getPercentageOfEnergySiphonDamage())).append("%)")
                .append("</br>P3 start to Enrage - ")
                .append(DMG_FORMAT.format(preEnragePersonalDamage))
                .append(" (").append(DECIMAL_FORMAT.format(getPercentageOfPreEnragePhaseDamageDealt())).append("%)")
                .append("</br>Enrage to kill - ")
                .append(DMG_FORMAT.format(getEnragePhasePersonalDamageDealt()))
                .append(" (").append(DECIMAL_FORMAT.format(getPercentageOfEnragePhaseDamageDealt())).append("%)")
                .append("</br>Total Damage - ")
                .append(DMG_FORMAT.format(getTotalPersonalDamageDealt()));
        return stringBuilder.toString();
    }
}
