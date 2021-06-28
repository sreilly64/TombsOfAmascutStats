package hsj.external.theatreofbloodstats;

import java.awt.Color;
import java.awt.image.BufferedImage;
import lombok.Getter;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import org.apache.commons.lang3.StringUtils;

@Getter
public class TheatreOfBloodStatsInfoBox extends InfoBox
{

	private final String room;
	private final String time;
	private final String percent;
	private final String damage;
	private final String splits;
	private final String healed;
	private final TheatreOfBloodStatsConfig config;


	TheatreOfBloodStatsInfoBox(
		BufferedImage image,
		TheatreOfBloodStatsConfig config,
		TheatreOfBloodStatsPlugin plugin,
		String room,
		String time,
		String percent,
		String damage,
		String splits,
		String healed
	)
	{
		super(image, plugin);
		this.config = config;
		this.room = room;
		this.time = time;
		this.percent = percent;
		this.damage = damage;
		this.splits = splits;
		this.healed = healed;
		setPriority(InfoBoxPriority.LOW);
	}

	@Override
	public String getText()
	{
		switch (config.infoBoxText())
		{
			case NONE:
				return "";
			case TIME:
				return StringUtils.substringBefore(time, ".");
			case DAMAGE_PERCENT:
				return Math.round(Double.parseDouble(percent)) + "%";
		}
		return "";
	}

	@Override
	public Color getTextColor()
	{
		return Color.GREEN;
	}

	@Override
	public String getTooltip()
	{
		if (!config.infoBoxTooltip())
		{
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(room);
		sb.append("</br>");

		if (config.infoBoxTooltipSplits() && !StringUtils.isEmpty(splits))
		{
			sb.append(splits);
			if (config.infoBoxTooltipDmg() || config.infoBoxTooltipHealed())
			{
				sb.append("</br>");
			}
		}

		if (config.infoBoxTooltipDmg() && !StringUtils.isEmpty(damage))
		{
			sb.append(damage).append(" (").append(percent).append("%)");
			if (config.infoBoxTooltipHealed())
			{
				sb.append("</br>");
			}
		}

		if (config.infoBoxTooltipHealed() && !StringUtils.isEmpty(healed))
		{
			sb.append(healed);
		}

		return sb.toString();
	}

	@Override
	public boolean render()
	{
		return config.showInfoBoxes();
	}
}
