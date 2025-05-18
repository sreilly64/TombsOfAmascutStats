package com.rr.raids.tombsofamascutstats.party;

import javax.inject.Inject;

import com.rr.raids.tombsofamascutstats.TombsOfAmascutStatsStatsConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import com.rr.raids.tombsofamascutstats.TombsOfAmascutStatsPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Slf4j
public class PartyDamageOverlay extends OverlayPanel
{
    private boolean currentlyInsideToA;
    private final PartyService partyService;
    private final TombsOfAmascutStatsStatsConfig config;
    private final List<PartyMemberDamageStats> partyMemberDamageStatsList;

    @Inject
    public PartyDamageOverlay(PartyService partyService, TombsOfAmascutStatsStatsConfig config)
    {
        this.partyService = partyService;
        this.config = config;
        this.partyMemberDamageStatsList = new ArrayList<>();
        setPosition(OverlayPosition.TOP_LEFT);
    }

    public void removePartyMember(long memberIdToRemove)
    {
        partyMemberDamageStatsList.removeIf(member -> member.getMemberId() == memberIdToRemove);
    }

    public void resetPartyMemberDamageStats()
    {
        partyMemberDamageStatsList.forEach(member ->
        {
            member.setCurrentDamageDealt(0);
            member.setPercentOfTotalDamageDealt(0.0);
        });
    }

    @Override
    public Dimension render (Graphics2D graphics)
    {
        if (!config.partyPluginOverlayToggle() || !partyService.isInParty() || !currentlyInsideToA)
        {
            return null;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Party Damage:")
                .build());

        Collections.sort(partyMemberDamageStatsList);
        int rank = 1;
        for (PartyMemberDamageStats member: partyMemberDamageStatsList)
        {
            if (partyService.getMemberById(member.getMemberId()) == null || !member.isCurrentlyInsideToA())
            {
                continue; //do not render a player's stats on the overlay if the id is null or if the player is not currently inside of ToA
            }

            String memberDisplayName = partyService.getMemberById(member.getMemberId()).getDisplayName();
            Color displayNameColor = Color.WHITE;

            if (partyService.getLocalMember().getMemberId() == member.getMemberId())
            {
                displayNameColor = config.selfNameColor();
            }

            if (config.truncatePlayerNamesToggle())
            {
                memberDisplayName = memberDisplayName.substring(0, Math.min(config.maxDisplayNameLength(), memberDisplayName.length()));
            }

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(rank +". " + memberDisplayName)
                    .leftColor(displayNameColor)
                    .right(member.getCurrentDamageDealt() + " (" + TombsOfAmascutStatsPlugin.DECIMAL_FORMAT.format(member.getPercentOfTotalDamageDealt()) + "%)")
                    .build());
            rank++;
        }

        return super.render(graphics);
    }

    public void updatePartyMemberDamageStats(PartyMemberDamageStats partyMemberDamageStatsUpdate)
    {
        for (PartyMemberDamageStats member: partyMemberDamageStatsList)
        {
            if (member.getMemberId() != partyMemberDamageStatsUpdate.getMemberId())
            {
                continue;
            }

            if (partyMemberDamageStatsUpdate.getCurrentDamageDealt() != 0)
            {
                member.setCurrentDamageDealt(partyMemberDamageStatsUpdate.getCurrentDamageDealt());
            }
            if (partyMemberDamageStatsUpdate.getPercentOfTotalDamageDealt() != 0.0)
            {
                member.setPercentOfTotalDamageDealt(partyMemberDamageStatsUpdate.getPercentOfTotalDamageDealt());
            }
            member.setCurrentlyInsideToA(partyMemberDamageStatsUpdate.isCurrentlyInsideToA());
            return;
        }
        // if the update's memberId was not found, then add new player's stats to list
        partyMemberDamageStatsList.add(partyMemberDamageStatsUpdate);
    }
}
