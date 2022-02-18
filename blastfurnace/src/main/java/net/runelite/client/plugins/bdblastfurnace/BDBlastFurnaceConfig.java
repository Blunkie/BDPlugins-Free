package net.runelite.client.plugins.bdblastfurnace;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BDOneClickBF")
public interface BDBlastFurnaceConfig extends Config {
    @ConfigItem(
            position = 0,
            keyName = "barType",
            name = "Bar",
            description = "Bars to make"
    )
    default BarType barType()
    {
        return BarType.ADAMANTITE;
    }
}