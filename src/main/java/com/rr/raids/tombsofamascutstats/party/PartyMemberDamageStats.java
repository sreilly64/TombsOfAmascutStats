package com.rr.raids.tombsofamascutstats.party;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.party.messages.PartyMemberMessage;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PartyMemberDamageStats extends PartyMemberMessage implements Comparable<PartyMemberDamageStats>
{
    int currentDamageDealt;
    double percentOfTotalDamageDealt;
    boolean currentlyInsideToA;

    @Override
    public int compareTo(PartyMemberDamageStats other) {
        return other.getCurrentDamageDealt() - currentDamageDealt;
    }
}
