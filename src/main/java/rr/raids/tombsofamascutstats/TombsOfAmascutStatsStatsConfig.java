package rr.raids.tombsofamascutstats;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("tombsofamascutstats")
public interface TombsOfAmascutStatsStatsConfig extends Config
{
	@ConfigSection(
		name = "Chatbox Messages",
		description = "Configure settings for the chatbox messages",
		position = 0
	)
	String CHAT_SETTINGS = "chatSettings";

	@ConfigItem(
		keyName = "chatboxDmg",
		name = "Print Damage To Chat",
		description = "Print personal damage and percentage of total damage to the chat",
		section = CHAT_SETTINGS,
		position = 0
	)
	default boolean chatboxDmg()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatboxSplits",
		name = "Print Splits To Chat",
		description = "Print detailed room splits to the chat",
		section = CHAT_SETTINGS,
		position = 2
	)
	default boolean chatboxSplits()
	{
		return true;
	}

	@ConfigSection(
		name = "Info Boxes",
		description = "Configure settings for the infoboxes",
		position = 1
	)
	String INFO_BOX_SETTINGS = "infoBoxSettings";

	@ConfigItem(
		keyName = "showInfoBoxes",
		name = "Info Boxes",
		description = "Show info boxes",
		section = INFO_BOX_SETTINGS,
		position = 1
	)
	default boolean showInfoBoxes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "infoBoxText",
		name = "Info Box Text",
		description = "The text displayed on the info boxes",
		section = INFO_BOX_SETTINGS,
		position = 1
	)
	default InfoBoxText infoBoxText()
	{
		return InfoBoxText.DAMAGE_PERCENT;
	}

	@ConfigItem(
		keyName = "infoBoxTooltip",
		name = "Info Box Tooltip",
		description = "Display info box tooltip when hovered over with mouse",
		section = INFO_BOX_SETTINGS,
		position = 2
	)
	default boolean infoBoxTooltip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "infoBoxTooltipDmg",
		name = "Info Box Tooltip Damage",
		description = "Display damage info in the info box tooltip",
		section = INFO_BOX_SETTINGS,
		position = 3
	)
	default boolean infoBoxTooltipDmg()
	{
		return true;
	}

	@ConfigItem(
		keyName = "infoBoxTooltipSplits",
		name = "Info Box Tooltip Splits",
		description = "Display splits in the info box tooltip",
		section = INFO_BOX_SETTINGS,
		position = 5
	)
	default boolean infoBoxTooltipSplits()
	{
		return true;
	}

	@ConfigSection(
			name = "Party Plugin Overlay",
			description = "Configure settings for the Party Plugin Overlay",
			position = 2
	)
	String PARTY_PLUGIN_OVERLAY_SETTINGS = "partyPluginOverlaySettings";

	@ConfigItem(
			keyName = "partyPluginOverlayToggle",
			name = "Display Party Damage Overlay",
			description = "If in a Party via Party plugin, toggles whether to display an overlay of all party members' current damage contributions",
			section = PARTY_PLUGIN_OVERLAY_SETTINGS,
			position = 0
	)
	default boolean partyPluginOverlayToggle()
	{
		return true;
	}

	@ConfigItem(
			keyName = "truncatePlayerNamesToggle",
			name = "Truncate Player Names",
			description = "Toggles whether or not to truncate player display names on the Party Damage Overlay",
			section = PARTY_PLUGIN_OVERLAY_SETTINGS,
			position = 1
	)
	default boolean truncatePlayerNamesToggle()
	{
		return false;
	}

	@ConfigItem(
			keyName = "maxPlayerNameLength",
			name = "Truncated Name Length",
			description = "When \"Truncate Player Names\" is enabled, this field determines how many characters of each player's names are displayed on the Party Damage Overlay",
			section = PARTY_PLUGIN_OVERLAY_SETTINGS,
			position = 2
	)
	@Range(
			min = 1,
			max = 12
	)
	default int maxDisplayNameLength()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "selfNameColor",
			name = "Self Name Color",
			description = "Sets the color of your own name in the Party Damage Overlay",
			section = PARTY_PLUGIN_OVERLAY_SETTINGS,
			position = 3
	)
	default Color selfNameColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
			keyName = "printPartyDamageToChatToggle",
			name = "Print Party Damage to Chat",
			description = "If in a Party via Party plugin, toggles whether to print all party members' final damage contributions after a boss is defeated",
			section = PARTY_PLUGIN_OVERLAY_SETTINGS,
			position = 4
	)
	default boolean printPartyDamageToChatToggle()
	{
		return true;
	}
}
