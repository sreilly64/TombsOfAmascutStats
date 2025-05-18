package com.rr.raids.tombsofamascutstats.stats.phases;

public enum KephriPhase
{
    SHIELD_1("Shield 1 - "),
    SHIELD_2("Shield 2 - "),
    SHIELD_3("Shield 3 - "),
    GREEN_HP("Green Health - "),
    TOTAL("Total - ");

    public final String phaseName;

    KephriPhase(String phaseName)
    {
        this.phaseName = phaseName;
    }
}
