package rr.raids.tombsofamascutstats.stats.phases;

public enum AkkhaPhase
{
    ONE_HUNDRED_TO_EIGHTY("100%-80% - "),
    SHADOW_1("Shadow 1 - "),
    EIGHTY_TO_SIXTY("80%-60% - "),
    SHADOW_2("Shadow 2 - "),
    SIXTY_TO_FORTY("60%-40% - "),
    SHADOW_3("Shadow 3 - "),
    FORTY_TO_TWENTY("40%-20% - "),
    SHADOW_4("Shadow 4 - "),
    TWENTY_TO_ENRAGE("20%-0% - "),
    ENRAGE("Enrage - "),
    TOTAL("Total - ");

    public final String phaseName;

    AkkhaPhase(String phaseName)
    {
        this.phaseName = phaseName;
    }

}
