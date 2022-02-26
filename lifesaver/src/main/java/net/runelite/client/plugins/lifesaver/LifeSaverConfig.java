package net.runelite.client.plugins.lifesaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BDLifeSaverPlugin")
public interface LifeSaverConfig extends Config {

	@ConfigItem(
			keyName = "watchDogTickTimer",
			name = "Watchdog Timer Ticks",
			description = "How many ticks until shut off. 0 means ignore this",
			position = 0
	)
	default int watchDogTickTimer() {
		return 100;
	}

	@ConfigItem(
			keyName = "enableUI",
			name = "Enable UI",
			description = "Enable to turn on in game UI",
			position = 1
	)
	default boolean enableUI() {
		return true;
	}


	@ConfigItem(
			keyName = "takeBreaks",
			name = "Take Breaks",
			description = "Take breaks or not",
			position = 2,
			section = "takeBreaksConfig"
	)
	default boolean takeBreaks() {
		return false;
	}

	@ConfigItem(
			keyName = "runMinMinutes",
			name = "Minimum Run Minutes",
			description = "runMinMinutes",
			position = 3,
			hidden = true,
			unhide = "takeBreaks",
			section = "takeBreaksConfig"
	)
	default int runMinMinutes() {
		return 10;
	}

	@ConfigItem(
			keyName = "runMaxMinutes",
			name = "Max Run Minutes",
			description = "runMaxMinutes",
			position = 4,
			hidden = true,
			unhide = "takeBreaks",
			section = "takeBreaksConfig"
	)
	default int runMaxMinutes() {
		return 20;
	}

	@ConfigItem(
			keyName = "breakMinMinutes",
			name = "Minimum Break Minutes",
			description = "breakMinMinutes",
			position = 5,
			hidden = true,
			unhide = "takeBreaks",
			section = "takeBreaksConfig"
	)
	default int breakMinMinutes() {
		return 1;
	}

	@ConfigItem(
			keyName = "breakMaxMinutes",
			name = "Max Break Minutes",
			description = "breakMaxMinutes",
			position = 6,
			hidden = true,
			unhide = "takeBreaks",
			section = "takeBreaksConfig"
	)
	default int breakMaxMinutes() {
		return 2;
	}

	@ConfigItem(
			keyName = "stopAfterBreaks",
			name = "Stop After X Breaks",
			description = "stopAfterBreaks",
			position = 7,
			hidden = true,
			unhide = "takeBreaks",
			section = "takeBreaksConfig"
	)
	default int stopAfterBreaks() {
		return 4;
	}
}