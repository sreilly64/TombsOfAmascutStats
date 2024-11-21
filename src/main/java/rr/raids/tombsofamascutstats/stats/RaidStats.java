package rr.raids.tombsofamascutstats.stats;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class RaidStats {

    private int startTick = -1;
    private int previousPhaseEndTick = -1;

    public abstract void resetStats();
    public abstract String getSplitTimes();
}
