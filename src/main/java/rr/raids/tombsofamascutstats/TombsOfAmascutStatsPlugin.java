/*
 * Copyright (c) 2023, Red Rookie
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package rr.raids.tombsofamascutstats;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;
import rr.raids.tombsofamascutstats.stats.*;
import rr.raids.tombsofamascutstats.stats.phases.AkkhaPhase;
import rr.raids.tombsofamascutstats.stats.phases.BabaPhase;
import rr.raids.tombsofamascutstats.stats.phases.KephriPhase;
import rr.raids.tombsofamascutstats.stats.phases.SecondWardensPhase;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Tombs of Amascut Stats",
	description = "Tombs of Amascut phase times and damage tracker",
	tags = {"combat", "raid", "pve", "pvm", "bosses", "toa"}
)
public class TombsOfAmascutStatsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TombsOfAmascutStatsStatsConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	public static final DecimalFormat DMG_FORMAT = new DecimalFormat("#,##0");
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.0");

	public static final String BABA = "Ba-Ba";
	public static final String KEPHRI = "Kephri";
	public static final String SCARABS = "Scarabs";
	public static final String AKKHA = "Akkha";
	public static final String AKKHAS_SHADOW = "Akkha's Shadow";
	public static final String ZEBAK = "Zebak";
	public static final String OBELISK = "Obelisk";
	public static final String TUMEKENS_WARDEN_SHIELDED = "Tumeken's Warden Shielded";
	public static final String ELIDINIS_WARDEN_SHIELDED = "Elidinis' Warden Shielded";
	public static final String CORE = "Core";
	public static final String TUMEKENS_WARDEN = "Tumeken's Warden";
	public static final String ELIDINIS_WARDEN = "Elidinis' Warden";

	private static final int KEPHRI_SHIELDED_HEALING_HITSPLAT_ID = HitsplatID.CYAN_UP;
	private static final int WARDENS_P2_DAMAGE_ME_HITSPLAT_ID = HitsplatID.DAMAGE_ME_POISE;
	private static final int WARDENS_P2_DAMAGE_OTHER_HITSPLAT_ID = HitsplatID.DAMAGE_OTHER_POISE;
	private static final int WARDENS_P2_DAMAGE_MAX_ME_HITSPLAT_ID = HitsplatID.DAMAGE_MAX_ME_POISE;

	private static final int PRECISE_TIMER = 11866;
	private static final int TICK_LENGTH = 600;
	private static final int BABA_PET_ID = 27383;
	private static final int KEPHRI_PET_ID = 27384;
	private static final int AKKHA_PET_ID = 27382;
	private static final int ZEBAK_PET_ID = 27385;
	private static final int OBELISK_ICON_ID = 21788;
	private static final int ELIDNIS_WARDEN_PET_ID = 27354;
	private static final int TUMEKENS_WARDEN_PET_ID = 27352;
	private static final int TOA_NEXUS_REGION_ID = 14160;
	private static final int BABA_PUZZLE_ROOM_REGION_ID = 15186;
	private static final int BABA_ROOM_REGION_ID = 15188;
	private static final int KEPHRI_PUZZLE_ROOM_REGION_ID = 14162;
	private static final int KEPHRI_ROOM_REGION_ID = 14164;
	private static final int AKKHA_PUZZLE_ROOM_REGION_ID = 14674;
	private static final int AKKHA_ROOM_REGION_ID = 14676;
	private static final int ZEBAK_PUZZLE_ROOM_REGION_ID = 15698;
	private static final int ZEBAK_ROOM_REGION_ID = 15700;
	private static final int WARDENS_OBELISK_ROOM_REGION_ID = 15184;
	private static final int WARDENS_P3_ROOM_REGION_ID = 15696;
	private static final int TOA_LOOT_ROOM_REGION_ID = 14672;
	private static final int TOA_LOBBY_REGION_ID = 13454;

	private static final Set<Integer> TOA_ROOM_IDS = ImmutableSet.of(
			TOA_NEXUS_REGION_ID,
			BABA_PUZZLE_ROOM_REGION_ID,
			BABA_ROOM_REGION_ID,
			KEPHRI_PUZZLE_ROOM_REGION_ID,
			KEPHRI_ROOM_REGION_ID,
			AKKHA_PUZZLE_ROOM_REGION_ID,
			AKKHA_ROOM_REGION_ID,
			ZEBAK_PUZZLE_ROOM_REGION_ID,
			ZEBAK_ROOM_REGION_ID,
			WARDENS_OBELISK_ROOM_REGION_ID,
			WARDENS_P3_ROOM_REGION_ID,
			TOA_LOOT_ROOM_REGION_ID,
			TOA_LOBBY_REGION_ID
	);

	private static final Pattern BABA_STARTED = Pattern.compile("Challenge started: Ba-Ba\\.");
	private static final Pattern KEPHRI_STARTED = Pattern.compile("Challenge started: Kephri\\.");
	private static final Pattern AKKHA_STARTED = Pattern.compile("Challenge started: Akkha\\.");
	private static final Pattern ZEBAK_STARTED = Pattern.compile("Challenge started: Zebak\\.");
	private static final Pattern WARDENS_STARTED = Pattern.compile("Challenge started: The Wardens\\.");
	private static final Pattern BABA_COMPLETE = Pattern.compile("Challenge complete: Ba-Ba\\.");
	private static final Pattern KEPHRI_COMPLETE = Pattern.compile("Challenge complete: Kephri\\.");
	private static final Pattern AKKHA_COMPLETE = Pattern.compile("Challenge complete: Akkha\\.");
	private static final Pattern ZEBAK_COMPLETE = Pattern.compile("Challenge complete: Zebak\\.");
	private static final Pattern OBELISK_COMPLETE_TUMEKEN_SPAWNS = Pattern.compile("As Elidinis' Warden falls, Tumeken's Warden powers up!");
	private static final Pattern OBELISK_COMPLETE_ELIDINIS_SPAWNS = Pattern.compile("As Tumeken's Warden falls, Elidinis' Warden powers up!");
	private static final Pattern WARDENS_P2_COMPLETE_TUMEKEN_SPAWNS = Pattern.compile("Elidinis' Warden uses the last of its power to restore Tumeken's Warden!");
	private static final Pattern WARDENS_P2_COMPLETE_ELIDINIS_SPAWNS = Pattern.compile("Tumeken's Warden uses the last of its power to restore Elidinis' Warden!");
	private static final Pattern WARDENS_COMPLETE = Pattern.compile("Challenge complete: The Wardens\\.");

	private TombsOfAmascutStatsStatsInfoBox babaInfoBox;
	private TombsOfAmascutStatsStatsInfoBox kephriInfoBox;
	private TombsOfAmascutStatsStatsInfoBox akkhaInfoBox;
	private TombsOfAmascutStatsStatsInfoBox zebakInfoBox;
	private TombsOfAmascutStatsStatsInfoBox obeliskInfoBox;
	private TombsOfAmascutStatsStatsInfoBox wardensP2InfoBox;
	private TombsOfAmascutStatsStatsInfoBox wardensP3InfoBox;

	private boolean currentlyInsideToA;
	private boolean instanced;
	private boolean preciseTimers;

	private final BabaStats babaStats = new BabaStats();
	private final KephriStats kephriStats = new KephriStats();
	private final AkkhaStats akkhaStats = new AkkhaStats();
	private final ZebakStats zebakStats = new ZebakStats();
	private final ObeliskStats obeliskStats = new ObeliskStats();
	private final FirstWardensStats firstWardensStats = new FirstWardensStats();
	private final SecondWardensStats secondWardensStats = new SecondWardensStats();
	private final Set<BossStats> bossStats = ImmutableSet.of(
			babaStats, kephriStats, akkhaStats, zebakStats, obeliskStats, firstWardensStats, secondWardensStats
	);

	@Provides
	TombsOfAmascutStatsStatsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TombsOfAmascutStatsStatsConfig.class);
	}

	@Override
	protected void shutDown() throws Exception
	{
		resetAll();
		resetAllInfoBoxes();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		int preciseTimerVar = client.getVarbitValue(PRECISE_TIMER);
		preciseTimers = preciseTimerVar == 1 ;

		int currentRegionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		currentlyInsideToA = TOA_ROOM_IDS.contains(currentRegionId);

		if (!currentlyInsideToA)
		{
			resetAll();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!currentlyInsideToA || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String strippedMessage = Text.removeTags(event.getMessage());
		List<String> messages = new ArrayList<>(Collections.emptyList());

		if (BABA_STARTED.matcher(strippedMessage).find())
		{
			babaStats.resetStats();
			babaStats.setStartTick(client.getTickCount());
		}
		else if (KEPHRI_STARTED.matcher(strippedMessage).find())
		{
			kephriStats.resetStats();
			kephriStats.setStartTick(client.getTickCount());
		}
		else if (AKKHA_STARTED.matcher(strippedMessage).find())
		{
			akkhaStats.resetStats();
			akkhaStats.setStartTick(client.getTickCount());
		}
		else if (ZEBAK_STARTED.matcher(strippedMessage).find())
		{
			zebakStats.resetStats();
			zebakStats.setStartTick(client.getTickCount());
		}
		else if (WARDENS_STARTED.matcher(strippedMessage).find())
		{
			obeliskStats.resetStats();
			firstWardensStats.resetStats();
			secondWardensStats.resetStats();
			resetWardensInfoBoxes();
			obeliskStats.setStartTick(client.getTickCount());
		}
		else if (BABA_COMPLETE.matcher(strippedMessage).find())
		{
			if (babaStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			babaStats.getPhaseCompletionTimes().put(BabaPhase.PHASE_3, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
			babaStats.getPhaseCompletionTimes().put(BabaPhase.TOTAL, formatTime(currentTick - babaStats.getStartTick()));

			if (config.chatboxSplits())
			{
				for (Map.Entry<BabaPhase, String> entry: babaStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Ba-Ba - ", DMG_FORMAT.format(babaStats.getPersonalDamage().get(BABA)) + " (" + DECIMAL_FORMAT.format(babaStats.getPercentageOfDamageDealt(BABA)) + "%)"));
			}

			String damage = babaStats.getInfoBoxBossDamageString();
			String splits = babaStats.getInfoBoxSplitTimesString();

			babaInfoBox = createInfoBox(BABA_PET_ID, BABA, babaStats.getPhaseCompletionTimes().get(BabaPhase.TOTAL), DECIMAL_FORMAT.format(babaStats.getPercentageOfDamageDealt(BABA)), damage, splits, "");
			infoBoxManager.addInfoBox(babaInfoBox);
			babaStats.resetStats();
		}
		else if (KEPHRI_COMPLETE.matcher(strippedMessage).find())
		{
			if (kephriStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			kephriStats.getPhaseCompletionTimes().put(KephriPhase.GREEN_HP, formatTime(currentTick - kephriStats.getPreviousPhaseEndTick()));
			kephriStats.getPhaseCompletionTimes().put(KephriPhase.TOTAL, formatTime(currentTick - kephriStats.getStartTick()));

			if (config.chatboxSplits())
			{
				for (Map.Entry<KephriPhase, String> entry: kephriStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("First down shield healed - ", DMG_FORMAT.format(kephriStats.getFirstShieldDownHealing())));
				messages.add(getStatsChatMessage("Second down shield healed - ", DMG_FORMAT.format(kephriStats.getSecondShieldDownHealing())));
				messages.add(getStatsChatMessage("Total shield healed - ", DMG_FORMAT.format(kephriStats.getShieldTotalHealing())));
				messages.add(getStatsChatMessage("Damage dealt to Kephri - ", DMG_FORMAT.format(kephriStats.getPersonalDamage().get(KEPHRI)) + " (" + DECIMAL_FORMAT.format(kephriStats.getPercentageOfDamageDealt(KEPHRI)) + "%)"));
				messages.add(getStatsChatMessage("Damage dealt to Scarabs - ", DMG_FORMAT.format(kephriStats.getPersonalDamage().get(SCARABS)) + " (" + DECIMAL_FORMAT.format(kephriStats.getPercentageOfDamageDealt(SCARABS)) + "%)"));
				messages.add(getStatsChatMessage("Total damage dealt - ", DMG_FORMAT.format(kephriStats.getTotalPersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(kephriStats.getTotalPercentageOfDamageDealt()) + "%)"));
			}

			String damage = kephriStats.getInfoBoxBossDamageString();
			String splits = kephriStats.getInfoBoxSplitTimesString();
			String healing = kephriStats.getInfoBoxHealingStatsString();

			kephriInfoBox = createInfoBox(KEPHRI_PET_ID, KEPHRI, kephriStats.getPhaseCompletionTimes().get(KephriPhase.TOTAL), DECIMAL_FORMAT.format(kephriStats.getTotalPercentageOfDamageDealt()), damage, splits, healing);
			infoBoxManager.addInfoBox(kephriInfoBox);
			kephriStats.resetStats();
		}
		else if (AKKHA_COMPLETE.matcher(strippedMessage).find())
		{
			if (akkhaStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.ENRAGE, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.TOTAL, formatTime(currentTick - akkhaStats.getStartTick()));

			if (config.chatboxSplits())
			{
				for (Map.Entry<AkkhaPhase, String> entry: akkhaStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Akkha - ", DMG_FORMAT.format(akkhaStats.getPersonalDamage().get(AKKHA)) + " (" + DECIMAL_FORMAT.format(akkhaStats.getPercentageOfDamageDealt(AKKHA)) + "%)"));
				messages.add(getStatsChatMessage("Damage dealt to Akkha's Shadows - ", DMG_FORMAT.format(akkhaStats.getPersonalDamage().get(AKKHAS_SHADOW)) + " (" + DECIMAL_FORMAT.format(akkhaStats.getPercentageOfDamageDealt(AKKHAS_SHADOW)) + "%)"));
				messages.add(getStatsChatMessage("Total damage dealt - ", DMG_FORMAT.format(akkhaStats.getTotalPersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(akkhaStats.getTotalPercentageOfDamageDealt()) + "%)"));
			}

			String damage = akkhaStats.getInfoBoxBossDamageString();
			String splits = akkhaStats.getInfoBoxSplitTimesString();

			akkhaInfoBox = createInfoBox(AKKHA_PET_ID, AKKHA, akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.TOTAL), DECIMAL_FORMAT.format(akkhaStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(akkhaInfoBox);
			akkhaStats.resetStats();
		}
		else if (ZEBAK_COMPLETE.matcher(strippedMessage).find())
		{
			if (zebakStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			zebakStats.setTotalCompletionTime(formatTime(currentTick - zebakStats.getStartTick()));

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Zebak - ", DMG_FORMAT.format(zebakStats.getPersonalDamage().get(ZEBAK)) + " (" + DECIMAL_FORMAT.format(zebakStats.getPercentageOfDamageDealt(ZEBAK)) + "%)"));
			}

			String damage = zebakStats.getInfoBoxBossDamageString();
			String splits = zebakStats.getInfoBoxSplitTimesString();

			zebakInfoBox = createInfoBox(ZEBAK_PET_ID, ZEBAK, zebakStats.getTotalCompletionTime(), DECIMAL_FORMAT.format(zebakStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(zebakInfoBox);
			zebakStats.resetStats();
		}
		else if (OBELISK_COMPLETE_TUMEKEN_SPAWNS.matcher(strippedMessage).find() || OBELISK_COMPLETE_ELIDINIS_SPAWNS.matcher(strippedMessage).find())
		{
			if (obeliskStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			obeliskStats.setTotalCompletionTime(formatTime(currentTick - obeliskStats.getStartTick()));

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to Obelisk - ", DMG_FORMAT.format(obeliskStats.getPersonalDamage().get(OBELISK)) + " (" + DECIMAL_FORMAT.format(obeliskStats.getPercentageOfDamageDealt(OBELISK)) + "%)"));
			}

			String damage = obeliskStats.getInfoBoxBossDamageString();
			String splits = obeliskStats.getInfoBoxSplitTimesString();

			obeliskInfoBox = createInfoBox(OBELISK_ICON_ID, OBELISK, obeliskStats.getTotalCompletionTime(), DECIMAL_FORMAT.format(obeliskStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(obeliskInfoBox);
			obeliskStats.resetStats();

			firstWardensStats.setStartTick(client.getTickCount());
		}
		else if (WARDENS_P2_COMPLETE_ELIDINIS_SPAWNS.matcher(strippedMessage).find() || WARDENS_P2_COMPLETE_TUMEKEN_SPAWNS.matcher(strippedMessage).find())
		{
			if (firstWardensStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			firstWardensStats.setTotalCompletionTime(formatTime(currentTick - firstWardensStats.getStartTick()));
			//set flag that denotes which Warden was fought first
			firstWardensStats.setElidnisWardenFought(WARDENS_P2_COMPLETE_TUMEKEN_SPAWNS.matcher(strippedMessage).find());
			secondWardensStats.setElidnisWardenFought(!firstWardensStats.isElidnisWardenFought());

			String wardensName = firstWardensStats.isElidnisWardenFought() ? ELIDINIS_WARDEN : TUMEKENS_WARDEN;
			String shieldedWardensName = wardensName.equals(ELIDINIS_WARDEN) ? ELIDINIS_WARDEN_SHIELDED : TUMEKENS_WARDEN_SHIELDED;
			int iconId = wardensName.equals(ELIDINIS_WARDEN) ? ELIDNIS_WARDEN_PET_ID : TUMEKENS_WARDEN_PET_ID;

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Damage dealt to "+wardensName+" - ", DMG_FORMAT.format(firstWardensStats.getPersonalDamage().get(shieldedWardensName)) + " (" + DECIMAL_FORMAT.format(firstWardensStats.getPercentageOfDamageDealt(shieldedWardensName)) + "%)"));
				messages.add(getStatsChatMessage("Damage dealt to Core - ", DMG_FORMAT.format(firstWardensStats.getPersonalDamage().get(CORE)) + " (" + DECIMAL_FORMAT.format(firstWardensStats.getPercentageOfDamageDealt(CORE)) + "%)"));
				messages.add(getStatsChatMessage("Total damage dealt - ", DMG_FORMAT.format(firstWardensStats.getTotalPersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(firstWardensStats.getTotalPercentageOfDamageDealt()) + "%)"));
			}

			String damage = firstWardensStats.getInfoBoxBossDamageString();
			String splits = firstWardensStats.getInfoBoxSplitTimesString();

			wardensP2InfoBox = createInfoBox(iconId, wardensName, firstWardensStats.getTotalCompletionTime(), DECIMAL_FORMAT.format(firstWardensStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(wardensP2InfoBox);
			firstWardensStats.resetStats();

			secondWardensStats.setStartTick(client.getTickCount());
		}
		else if (WARDENS_COMPLETE.matcher(strippedMessage).find())
		{
			if (secondWardensStats.getStartTick() < 0)
			{
				return;
			}

			messages.clear();

			//calculate final phase time and total kill time
			int currentTick = client.getTickCount();
			secondWardensStats.getPhaseCompletionTimes().put(SecondWardensPhase.ENRAGE_TO_KILL, formatTime(currentTick - secondWardensStats.getPreviousPhaseEndTick()));
			secondWardensStats.getPhaseCompletionTimes().put(SecondWardensPhase.TOTAL, formatTime(currentTick - secondWardensStats.getStartTick()));

			String wardensName = secondWardensStats.isElidnisWardenFought() ? ELIDINIS_WARDEN: TUMEKENS_WARDEN;
			int iconId = wardensName.equals(ELIDINIS_WARDEN) ? ELIDNIS_WARDEN_PET_ID : TUMEKENS_WARDEN_PET_ID;

			if (config.chatboxSplits())
			{
				for (Map.Entry<SecondWardensPhase, String> entry: secondWardensStats.getPhaseCompletionTimes().entrySet())
				{
					String phaseName = entry.getKey().phaseName;
					String phaseTime = entry.getValue();
					messages.add(getStatsChatMessage(phaseName, phaseTime));
				}
			}

			if (config.chatboxDmg())
			{
				messages.add(getStatsChatMessage("Energy Siphon damage - ", DMG_FORMAT.format(secondWardensStats.getEnergySiphonBossDamage()) + " (" + DECIMAL_FORMAT.format(secondWardensStats.getPercentageOfEnergySiphonDamage()) + "%)"));
				messages.add(getStatsChatMessage("Start to Enrage damage dealt - ", DMG_FORMAT.format(secondWardensStats.getPreEnragePersonalDamage()) + " (" + DECIMAL_FORMAT.format(secondWardensStats.getPercentageOfPreEnragePhaseDamageDealt()) + "%)"));
				messages.add(getStatsChatMessage("Enrage damage dealt - ", DMG_FORMAT.format(secondWardensStats.getEnragePhasePersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(secondWardensStats.getPercentageOfEnragePhaseDamageDealt()) + "%)"));
				messages.add(getStatsChatMessage("Total damage dealt - ", DMG_FORMAT.format(secondWardensStats.getTotalPersonalDamageDealt()) + " (" + DECIMAL_FORMAT.format(secondWardensStats.getTotalPercentageOfDamageDealt()) + "%)"));
			}

			String damage = secondWardensStats.getInfoBoxBossDamageString();
			String splits = secondWardensStats.getInfoBoxSplitTimesString();

			wardensP3InfoBox = createInfoBox(iconId, wardensName, secondWardensStats.getPhaseCompletionTimes().get(SecondWardensPhase.TOTAL), DECIMAL_FORMAT.format(secondWardensStats.getTotalPercentageOfDamageDealt()), damage, splits, "");
			infoBoxManager.addInfoBox(wardensP3InfoBox);
			secondWardensStats.resetStats();
		}

		if (!messages.isEmpty())
		{
			for (String m : messages)
			{
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(m)
					.build());
			}
			messages.clear();
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();
		int currentTick = client.getTickCount();

		switch (npcId)
		{
			case NpcID.BABA_11780: //Ba-Ba leaps to the top of the room and starts throwing boulders
				if (StringUtils.isEmpty(babaStats.getPhaseCompletionTimes().get(BabaPhase.PHASE_1))) //if a time for phase 1 has not yet been recorded
				{
					babaStats.getPhaseCompletionTimes().put(BabaPhase.PHASE_1, formatTime(currentTick - babaStats.getStartTick()));
				}
				else
				{
					babaStats.getPhaseCompletionTimes().put(BabaPhase.PHASE_2, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
				}
				babaStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.BABA: //end of first boulder phase
				babaStats.getPhaseCompletionTimes().put(BabaPhase.BOULDERS_1, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
				babaStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.BABA_11779: //end of second boulder phase
				babaStats.getPhaseCompletionTimes().put(BabaPhase.BOULDERS_2, formatTime(currentTick - babaStats.getPreviousPhaseEndTick()));
				babaStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.KEPHRI_11720: //Kephri's shield is depleted and Scarab Swarm phase starts
				if (kephriStats.isFirstShieldDown())
				{
					kephriStats.getPhaseCompletionTimes().put(KephriPhase.SHIELD_1, formatTime(currentTick - kephriStats.getStartTick()));
				}
				else
				{
					kephriStats.getPhaseCompletionTimes().put(KephriPhase.SHIELD_2, formatTime(currentTick - kephriStats.getPreviousPhaseEndTick()));
				}
				kephriStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.KEPHRI: //Kephri starts attacking again after regaining her shield
				if (kephriStats.isFirstShieldDown())
				{
					kephriStats.setFirstShieldDownHealing(kephriStats.getShieldTotalHealing());
					kephriStats.setFirstShieldDown(false);
				}
				kephriStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.KEPHRI_11721: //Kephri's green health bar becomes exposed
				kephriStats.getPhaseCompletionTimes().put(KephriPhase.SHIELD_3, formatTime(currentTick - kephriStats.getPreviousPhaseEndTick()));
				kephriStats.setPreviousPhaseEndTick(currentTick);
				break;
			case NpcID.AKKHA_11795: //Enrage phase Akkha
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.TWENTY_TO_ENRAGE, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
				akkhaStats.setPreviousPhaseEndTick(currentTick);
				break;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		NPC npc = event.getNpc();

		if (npc.getId() == NpcID.AKKHAS_SHADOW)
		{
			int currentTick = client.getTickCount();
			if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.ONE_HUNDRED_TO_EIGHTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.ONE_HUNDRED_TO_EIGHTY, formatTime(currentTick - akkhaStats.getStartTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.EIGHTY_TO_SIXTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.EIGHTY_TO_SIXTY, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SIXTY_TO_FORTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SIXTY_TO_FORTY, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.FORTY_TO_TWENTY)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.FORTY_TO_TWENTY, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			akkhaStats.setPreviousPhaseEndTick(currentTick);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		NPC npc = event.getNpc();

		if (npc.getId() == NpcID.AKKHAS_SHADOW)
		{
			int currentTick = client.getTickCount();
			if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_1)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_1, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_2)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_2, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_3)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_3, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			else if (StringUtils.isEmpty(akkhaStats.getPhaseCompletionTimes().get(AkkhaPhase.SHADOW_4)))
			{
				akkhaStats.getPhaseCompletionTimes().put(AkkhaPhase.SHADOW_4, formatTime(currentTick - akkhaStats.getPreviousPhaseEndTick()));
			}
			akkhaStats.setPreviousPhaseEndTick(currentTick);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOADING)
		{
			return;
		}

		boolean prevInstance = instanced;
		instanced = client.getLocalPlayer().getWorldView().isInstance();

		if (prevInstance && !instanced) //going from raid into lobby
		{
			resetAll();
		}
		else if (!prevInstance && !instanced) //going from lobby into raid
		{
			resetAll();
			resetAllInfoBoxes();
		}
		else if (!prevInstance && instanced) //going from lobby into anywhere else
		{
			resetAll();
			resetAllInfoBoxes();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!currentlyInsideToA)
		{
			return;
		}

		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		String npcName = Text.removeTags(npc.getName());

		if (npcName == null)
		{
			return;
		}

		if (npcName.contains("Scarab"))
		{
			npcName = SCARABS; //group all damage to Scarab Overlords under one name
		}

		Hitsplat hitsplat = event.getHitsplat();

		if (isWardensP2Hitsplat(hitsplat))
		{
			npcName = npcName + " Shielded"; //save shield damage as separate enemy name due to damage Wardens receive from attacking the Core being saved already under Wardens' names
		}

		if (hitsplat.isMine())
		{
			for (BossStats bossStats: bossStats)
			{
				if (bossStats.getEnemyNames().contains(npcName))
				{
					bossStats.addToPersonalDamage(npcName, hitsplat.getAmount());
				}
			}
		}
		else if (hitsplat.isOthers())
		{
			for (BossStats bossStats: bossStats)
			{
				if (bossStats.getEnemyNames().contains(npcName))
				{
					bossStats.addToTotalDamage(npcName, hitsplat.getAmount());
				}
			}

			if (isAWarden(npcName) && (secondWardensStats.getStartTick() < 0) && secondWardensStats.isEnergySiphonsKilled())
			{
				//if a Warden receives damage in P3 and Energy Siphon projectiles were detected, attribute damage to Energy Siphons
				secondWardensStats.addToEnergySiphonBossDamage(hitsplat.getAmount());
				secondWardensStats.setEnergySiphonsKilled(false);
			}
		}
		else if (hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			if (isAWarden(npcName))
			{
				if (secondWardensStats.isWardensEnrageHeal()) //the Wardens heal twice, once at the very start of Phase 3 and once when they enter enrage phase/phase 4
				{
					//on the second heal, record time and damage up to that point
					int currentTick = client.getTickCount();
					secondWardensStats.getPhaseCompletionTimes().put(SecondWardensPhase.START_TO_ENRAGE, formatTime(currentTick - secondWardensStats.getStartTick()));
					secondWardensStats.setPreviousPhaseEndTick(currentTick);
					secondWardensStats.setPreEnragePersonalDamage(secondWardensStats.getPersonalDamage().get(npcName));
					secondWardensStats.setPreEnrageTotalDamage(secondWardensStats.getTotalDamage().get(npcName));
					secondWardensStats.setWardensEnrageHeal(false);
				}
				else
				{
					//set enrage heal flag to true as the next heal that Wardens receives will be from the start of enrage
					secondWardensStats.setWardensEnrageHeal(true);
				}
			}
		}
		else if (hitsplat.getHitsplatType() == KEPHRI_SHIELDED_HEALING_HITSPLAT_ID && npcName.equals(KEPHRI)) //Hitsplat ID is shared with Palm of Resourcefulness
		{
			kephriStats.addToKephriShieldTotalHealing(hitsplat.getAmount());
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (event.getProjectile().getId() == 2226) //ID 2226 is that of the energy siphons (red skulls) as they fly to or from the Warden in P3
		{
			log.info("energy siphon projectile event position: {}", event.getPosition().toString());
			if (areLocalPointsEqual(event.getPosition(), secondWardensStats.getLastEnergySiphonPosition()))
			{
				if (!secondWardensStats.isEnergySiphonsKilled())
				{
					secondWardensStats.setEnergySiphonsKilled(true);
				}
			}
			secondWardensStats.setLastEnergySiphonPosition(event.getPosition());
		}
	}

	private boolean areLocalPointsEqual(LocalPoint localPointOne, LocalPoint localPointTwo)
	{
		if (localPointOne == null || localPointTwo == null)
		{
			return false;
		}
		return localPointOne.getX() == localPointTwo.getX() &&
				localPointOne.getY() == localPointTwo.getY() &&
				localPointOne.getWorldView() == localPointTwo.getWorldView();
	}

	private boolean isAWarden(String npcName)
	{
		if (npcName == null)
		{
			return false;
		}
		return npcName.equalsIgnoreCase(ELIDINIS_WARDEN) || npcName.equalsIgnoreCase(TUMEKENS_WARDEN);
	}

	private boolean isWardensP2Hitsplat(Hitsplat hitsplat)
	{
		int hitSplatId = hitsplat.getHitsplatType();
		return hitSplatId == WARDENS_P2_DAMAGE_ME_HITSPLAT_ID
				|| hitSplatId == WARDENS_P2_DAMAGE_MAX_ME_HITSPLAT_ID
				|| hitSplatId == WARDENS_P2_DAMAGE_OTHER_HITSPLAT_ID;
	}

	private TombsOfAmascutStatsStatsInfoBox createInfoBox(int itemId, String room, String time, String percent, String damage, String splits, String healed)
	{
		BufferedImage image = itemManager.getImage(itemId);
		return new TombsOfAmascutStatsStatsInfoBox(image, config, this, room, time, percent, damage, splits, healed);
	}

	private String formatTime(int ticks)
	{
		int millis = ticks * TICK_LENGTH;
		String hundredths = String.valueOf(millis % 1000).substring(0, 1);

		if (preciseTimers)
		{
			return String.format("%d:%02d.%s",
				TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
				TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
				hundredths);
		}
		else
		{
			return String.format("%d:%02d",
				TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
				TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
		}
	}

	private String getStatsChatMessage(String key, String value)
	{
		return new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append(key)
				.append(Color.RED, value)
				.build();
	}

	private void resetAll()
	{
		babaStats.resetStats();
		kephriStats.resetStats();
		akkhaStats.resetStats();
		zebakStats.resetStats();
		obeliskStats.resetStats();
		firstWardensStats.resetStats();
		secondWardensStats.resetStats();
	}

	private void resetWardensInfoBoxes()
	{
		infoBoxManager.removeInfoBox(obeliskInfoBox);
		infoBoxManager.removeInfoBox(wardensP2InfoBox);
		infoBoxManager.removeInfoBox(wardensP3InfoBox);
	}

	private void resetAllInfoBoxes()
	{
		infoBoxManager.removeInfoBox(babaInfoBox);
		infoBoxManager.removeInfoBox(kephriInfoBox);
		infoBoxManager.removeInfoBox(akkhaInfoBox);
		infoBoxManager.removeInfoBox(zebakInfoBox);
		infoBoxManager.removeInfoBox(obeliskInfoBox);
		infoBoxManager.removeInfoBox(wardensP2InfoBox);
		infoBoxManager.removeInfoBox(wardensP3InfoBox);
	}
}
