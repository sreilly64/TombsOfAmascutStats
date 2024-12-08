package rr.raids.tombsofamascutstats.stats.phases;

public enum BabaPhase
{
    PHASE_1("Phase 1 - "),
    BOULDERS_1("Boulders 1 - "),
    PHASE_2("Phase 2 - "),
    BOULDERS_2("Boulders 2 - "),
    PHASE_3("Phase 3 - "),
    TOTAL("Total - ");

    public final String phaseName;

    BabaPhase(String phaseName)
    {
        this.phaseName = phaseName;
    }
}
