package dev.lenvx.gateway.inventory

import net.kyori.adventure.key.Key

enum class InventoryType(
    @get:JvmName("getDefaultSize") val defaultSize: Int,
    @get:JvmName("getDefaultTitle") val defaultTitle: String,
    @get:JvmName("isCreatable") val isCreatable: Boolean = true
) {

    
    CHEST(27, "Chest"),
    
    DISPENSER(9, "Dispenser"),
    
    DROPPER(9, "Dropper"),
    
    FURNACE(3, "Furnace"),
    
    WORKBENCH(10, "Crafting"),
    
    CRAFTING(5, "Crafting", false),
    
    ENCHANTING(2, "Enchanting"),
    
    BREWING(5, "Brewing"),
    
    PLAYER(41, "Player"),
    
    CREATIVE(9, "Creative", false),
    
    MERCHANT(3, "Villager", false),
    
    ENDER_CHEST(27, "Ender Chest"),
    
    ANVIL(3, "Repairing"),
    
    SMITHING(3, "Upgrade Gear"),
    
    BEACON(1, "container.beacon"),
    
    HOPPER(5, "Item Hopper"),
    
    SHULKER_BOX(27, "Shulker Box"),
    
    BARREL(27, "Barrel"),
    
    BLAST_FURNACE(3, "Blast Furnace"),
    
    LECTERN(1, "Lectern"),
    
    SMOKER(3, "Smoker"),
    
    LOOM(4, "Loom"),
    
    CARTOGRAPHY(3, "Cartography Table"),
    
    GRINDSTONE(3, "Repair & Disenchant"),
    
    STONECUTTER(2, "Stonecutter"),
    
    COMPOSTER(1, "Composter"),
    
    CHISELED_BOOKSHELF(6, "Chiseled Bookshelf");

    fun getRawType(slots: Int): Key? {
        return when (this) {
            CHEST -> Key.key("minecraft:generic_9x" + (slots / 9))
            DISPENSER, DROPPER -> Key.key("minecraft:generic_3x3")
            FURNACE -> Key.key("minecraft:furnace")
            WORKBENCH -> Key.key("minecraft:crafting")
            ENCHANTING -> Key.key("minecraft:enchantment")
            BREWING -> Key.key("minecraft:brewing_stand")
            MERCHANT -> Key.key("minecraft:merchant")
            ENDER_CHEST, BARREL -> Key.key("minecraft:generic_9x3")
            ANVIL -> Key.key("minecraft:anvil")
            SMITHING -> Key.key("minecraft:smithing")
            BEACON -> Key.key("minecraft:beacon")
            HOPPER -> Key.key("minecraft:hopper")
            SHULKER_BOX -> Key.key("minecraft:shulker_box")
            BLAST_FURNACE -> Key.key("minecraft:blast_furnace")
            LECTERN -> Key.key("minecraft:lectern")
            SMOKER -> Key.key("minecraft:smoker")
            LOOM -> Key.key("minecraft:loom")
            CARTOGRAPHY -> Key.key("minecraft:cartography_table")
            GRINDSTONE -> Key.key("minecraft:grindstone")
            STONECUTTER -> Key.key("minecraft:stonecutter")
            CRAFTING, PLAYER, CREATIVE, COMPOSTER, CHISELED_BOOKSHELF -> null
        }
    }

    enum class SlotType {
        
        RESULT,
        
        CRAFTING,
        
        ARMOR,
        
        CONTAINER,
        
        QUICKBAR,
        
        OUTSIDE,
        
        FUEL;
    }
}

