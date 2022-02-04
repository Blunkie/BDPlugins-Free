package net.runelite.client.plugins.oneclickutils;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;


@ConfigGroup("oneclickutils")
public interface OneClickUtilsConfig extends Config {

    @ConfigItem(
            position = 0,
            keyName = "desiredInventory",
            name = "Desired Inventory",
            description = "Syntax: ItemID, amount, stackable. Ex: <995,10,true> Use. -1 for amount to fill. Put stackable items first"
    )
    default String desiredInventory()
    {
        return "11978,2\n3144,5";
    }

    @ConfigItem(
            position = 1,
            keyName = "exactInventory",
            name = "Exact Inventory",
            description = "Require this exact inventory (don't allow other items, and have exact amounts of everything)."
    )
    default boolean exactInventory() { return false; }

    @ConfigItem(
            position = 0,
            keyName = "desiredEquipment",
            name = "Desired Equipment",
            description = "Syntax: EquipmentItemID1, EquipmentItemID2, ..."
    )
    default String desiredEquipment()
    {
        return "2552, 11978";
    }


}
