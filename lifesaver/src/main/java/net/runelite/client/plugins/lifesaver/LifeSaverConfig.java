package net.runelite.client.plugins.lifesaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BDLifeSaverPlugin")
public interface LifeSaverConfig extends Config {

	@ConfigItem(
			keyName = "watchDogTickTimer",
			name = "Watchdog Timer Ticks",
			description = "How many ticks until shut off",
			position = 0
	)
	default int watchDogTickTimer() {
		return 100;
	}


}