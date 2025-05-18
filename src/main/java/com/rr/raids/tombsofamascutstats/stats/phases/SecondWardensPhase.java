package com.rr.raids.tombsofamascutstats.stats.phases;

public enum SecondWardensPhase
{
    START_TO_ENRAGE("Start to Enrage - "),
    ENRAGE_TO_KILL("Enrage to Kill - "),
    TOTAL("Total - ");

    public final String phaseName;

    SecondWardensPhase(String phaseName)
    {
        this.phaseName = phaseName;
    }
}
