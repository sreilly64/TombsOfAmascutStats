package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public abstract class BossStats
{

    private int startTick = -1;
    private int previousPhaseEndTick = -1;
    private Set<String> enemyNames = new HashSet<>();
    private final Map<String, Integer> personalDamage = new HashMap<>(); // key = enemy name, value = damage dealt to the enemy by the player
    private final Map<String, Integer> totalDamage = new HashMap<>(); // key = enemy name, value = total damage dealt to the enemy

    public abstract void resetStats();
    public abstract String getInfoBoxSplitTimesString();
    public abstract String getInfoBoxBossDamageString();

    public void addToPersonalDamage(String enemyName, int damageAmount)
    {
        addToTotalDamage(enemyName, damageAmount);

        if (!personalDamage.containsKey(enemyName))
        {
            return;
        }
        int currentPersonalDamage = personalDamage.get(enemyName);
        personalDamage.put(enemyName, currentPersonalDamage + damageAmount);
    }

    public void addToTotalDamage(String enemyName, int damageAmount)
    {
        if (!totalDamage.containsKey(enemyName))
        {
            return;
        }
        int currentTotalDamage = totalDamage.get(enemyName);
        totalDamage.put(enemyName, currentTotalDamage + damageAmount);
    }

    public double getPercentageOfDamageDealt(String enemyName)
    {
        if (!enemyNames.contains(enemyName))
        {
            return 0;
        }
        return (double) getPersonalDamage().get(enemyName) / getTotalDamage().get(enemyName) * 100;
    }

    public int getTotalPersonalDamageDealt()
    {
        int totalPersonalDamageDealt = 0;
        for (Integer damageAmount: personalDamage.values())
        {
            totalPersonalDamageDealt += damageAmount;
        }
        return totalPersonalDamageDealt;
    }

    public int getTotalDamageDealt()
    {
        int totalDamageDealt = 0;
        for (Integer damageAmount: totalDamage.values())
        {
            totalDamageDealt += damageAmount;
        }
        return totalDamageDealt;
    }

    public double getTotalPercentageOfDamageDealt()
    {
        return (double) getTotalPersonalDamageDealt() / getTotalDamageDealt() * 100;
    }
}
