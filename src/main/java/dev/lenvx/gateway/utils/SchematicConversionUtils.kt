package dev.lenvx.gateway.utils

import net.kyori.adventure.key.Key
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag
import net.querz.nbt.tag.StringTag
import net.querz.nbt.tag.Tag

object SchematicConversionUtils {

    @JvmStatic
    fun toTileEntityTag(tag: CompoundTag): CompoundTag {
        val pos = tag.getIntArray("Pos")
        tag.remove("Pos")
        tag.remove("Id")
        tag.putInt("x", pos[0])
        tag.putInt("y", pos[1])
        tag.putInt("z", pos[2])
        for (subTag in tag.values()) {
            removeStringTagQuote(subTag)
        }
        return tag
    }

    @JvmStatic
    fun removeStringTagQuote(tag: Tag<*>): Tag<*> {
        when (tag) {
            is StringTag -> {
                val value = tag.value
                if (value.startsWith('"') && value.endsWith('"')) {
                    tag.value = value.substring(1, value.length - 1)
                }
            }
            is CompoundTag -> {
                for (subTag in tag.values()) {
                    removeStringTagQuote(subTag)
                }
            }
            is ListTag<*> -> {
                for (subTag in tag) {
                    removeStringTagQuote(subTag)
                }
            }
        }
        return tag
    }

    @JvmStatic
    fun toBlockTag(input: String): CompoundTag {
        val index = input.indexOf('[')
        val tag = CompoundTag()
        if (index < 0) {
            tag.putString("Name", Key.key(input).toString())
            return tag
        }

        tag.putString("Name", Key.key(input.substring(0, index)).toString())

        val states = input.substring(index + 1, input.lastIndexOf(']')).replace(" ", "").split(",")

        val properties = CompoundTag()
        for (state in states) {
            val splitIndex = state.indexOf('=')
            if (splitIndex < 0) continue
            val key = state.substring(0, splitIndex)
            val value = state.substring(splitIndex + 1)
            properties.putString(key, value)
        }

        tag.put("Properties", properties)
        return tag
    }

    @JvmStatic
    fun getLegacyBlock(id: Int, data: Int): String {
        return when (id) {
            0 -> "minecraft:air"
            1 -> when (data) {
                1 -> "minecraft:granite"
                2 -> "minecraft:polished_granite"
                3 -> "minecraft:diorite"
                4 -> "minecraft:polished_diorite"
                5 -> "minecraft:andesite"
                6 -> "minecraft:polished_andesite"
                else -> "minecraft:stone"
            }
            2 -> "minecraft:grass_block"
            3 -> when (data) {
                1 -> "minecraft:coarse_dirt"
                2 -> "minecraft:podzol"
                else -> "minecraft:dirt"
            }
            4 -> "minecraft:cobblestone"
            5 -> when (data) {
                1 -> "minecraft:spruce_planks"
                2 -> "minecraft:birch_planks"
                3 -> "minecraft:jungle_planks"
                4 -> "minecraft:acacia_planks"
                5 -> "minecraft:dark_oak_planks"
                else -> "minecraft:oak_planks"
            }
            6 -> "minecraft:oak_sapling"
            7 -> "minecraft:bedrock"
            8, 9 -> "minecraft:water"
            10, 11 -> "minecraft:lava"
            12 -> if (data == 1) "minecraft:red_sand" else "minecraft:sand"
            13 -> "minecraft:gravel"
            14 -> "minecraft:gold_ore"
            15 -> "minecraft:iron_ore"
            16 -> "minecraft:coal_ore"
            17 -> when (data % 4) {
                1 -> "minecraft:spruce_log"
                2 -> "minecraft:birch_log"
                3 -> "minecraft:jungle_log"
                else -> "minecraft:oak_log"
            }
            18 -> when (data % 4) {
                1 -> "minecraft:spruce_leaves"
                2 -> "minecraft:birch_leaves"
                3 -> "minecraft:jungle_leaves"
                else -> "minecraft:oak_leaves"
            }
            19 -> "minecraft:sponge"
            20 -> "minecraft:glass"
            21 -> "minecraft:lapis_ore"
            22 -> "minecraft:lapis_block"
            23 -> "minecraft:dispenser"
            24 -> when (data) {
                1 -> "minecraft:chiseled_sandstone"
                2 -> "minecraft:smooth_sandstone"
                else -> "minecraft:sandstone"
            }
            25 -> "minecraft:note_block"
            30 -> "minecraft:cobweb"
            31 -> when (data) {
                0 -> "minecraft:dead_bush"
                2 -> "minecraft:fern"
                else -> "minecraft:grass"
            }
            32 -> "minecraft:dead_bush"
            33 -> "minecraft:piston"
            35 -> when (data) {
                1 -> "minecraft:orange_wool"
                2 -> "minecraft:magenta_wool"
                3 -> "minecraft:light_blue_wool"
                4 -> "minecraft:yellow_wool"
                5 -> "minecraft:lime_wool"
                6 -> "minecraft:pink_wool"
                7 -> "minecraft:gray_wool"
                8 -> "minecraft:light_gray_wool"
                9 -> "minecraft:cyan_wool"
                10 -> "minecraft:purple_wool"
                11 -> "minecraft:blue_wool"
                12 -> "minecraft:brown_wool"
                13 -> "minecraft:green_wool"
                14 -> "minecraft:red_wool"
                15 -> "minecraft:black_wool"
                else -> "minecraft:white_wool"
            }
            41 -> "minecraft:gold_block"
            42 -> "minecraft:iron_block"
            43, 44 -> "minecraft:stone_slab"
            45 -> "minecraft:bricks"
            46 -> "minecraft:tnt"
            47 -> "minecraft:bookshelf"
            48 -> "minecraft:mossy_cobblestone"
            49 -> "minecraft:obsidian"
            50 -> "minecraft:torch"
            51 -> "minecraft:fire"
            52 -> "minecraft:spawner"
            53 -> "minecraft:oak_stairs"
            54 -> "minecraft:chest"
            56 -> "minecraft:diamond_ore"
            57 -> "minecraft:diamond_block"
            58 -> "minecraft:crafting_table"
            60 -> "minecraft:farmland"
            61, 62 -> "minecraft:furnace"
            65 -> "minecraft:ladder"
            66 -> "minecraft:rail"
            67 -> "minecraft:cobblestone_stairs"
            69 -> "minecraft:lever"
            70 -> "minecraft:stone_pressure_plate"
            72 -> "minecraft:oak_pressure_plate"
            73, 74 -> "minecraft:redstone_ore"
            75, 76 -> "minecraft:redstone_torch"
            77 -> "minecraft:stone_button"
            78 -> "minecraft:snow"
            79 -> "minecraft:ice"
            80 -> "minecraft:snow_block"
            81 -> "minecraft:cactus"
            82 -> "minecraft:clay"
            85 -> "minecraft:oak_fence"
            86 -> "minecraft:carved_pumpkin"
            87 -> "minecraft:netherrack"
            88 -> "minecraft:soul_sand"
            89 -> "minecraft:glowstone"
            91 -> "minecraft:lit_pumpkin"
            95 -> "minecraft:white_stained_glass"
            98 -> when (data) {
                1 -> "minecraft:mossy_stone_bricks"
                2 -> "minecraft:cracked_stone_bricks"
                3 -> "minecraft:chiseled_stone_bricks"
                else -> "minecraft:stone_bricks"
            }
            101 -> "minecraft:iron_bars"
            102 -> "minecraft:glass_pane"
            103 -> "minecraft:melon"
            110 -> "minecraft:mycelium"
            112 -> "minecraft:nether_bricks"
            113 -> "minecraft:nether_brick_fence"
            114 -> "minecraft:nether_brick_stairs"
            116 -> "minecraft:enchanting_table"
            121 -> "minecraft:end_stone"
            122 -> "minecraft:dragon_egg"
            123, 124 -> "minecraft:redstone_lamp"
            126 -> "minecraft:oak_slab"
            129 -> "minecraft:emerald_ore"
            130 -> "minecraft:ender_chest"
            133 -> "minecraft:emerald_block"
            134 -> "minecraft:spruce_stairs"
            135 -> "minecraft:birch_stairs"
            136 -> "minecraft:jungle_stairs"
            138 -> "minecraft:beacon"
            139 -> "minecraft:cobblestone_wall"
            145 -> "minecraft:anvil"
            152 -> "minecraft:redstone_block"
            153 -> "minecraft:nether_quartz_ore"
            154 -> "minecraft:hopper"
            155 -> when (data) {
                1 -> "minecraft:chiseled_quartz_block"
                2 -> "minecraft:quartz_pillar"
                else -> "minecraft:quartz_block"
            }
            156 -> "minecraft:quartz_stairs"
            159 -> "minecraft:white_terracotta"
            161 -> when (data % 4) {
                1 -> "minecraft:dark_oak_leaves"
                else -> "minecraft:acacia_leaves"
            }
            162 -> when (data % 4) {
                1 -> "minecraft:dark_oak_log"
                else -> "minecraft:acacia_log"
            }
            163 -> "minecraft:acacia_stairs"
            164 -> "minecraft:dark_oak_stairs"
            165 -> "minecraft:slime_block"
            166 -> "minecraft:barrier"
            167 -> "minecraft:iron_trapdoor"
            168 -> when (data) {
                1 -> "minecraft:prismarine_bricks"
                2 -> "minecraft:dark_prismarine"
                else -> "minecraft:prismarine"
            }
            169 -> "minecraft:sea_lantern"
            170 -> "minecraft:hay_block"
            172 -> "minecraft:terracotta"
            173 -> "minecraft:coal_block"
            174 -> "minecraft:packed_ice"
            175 -> "minecraft:sunflower"
            206 -> "minecraft:end_stone_bricks"
            210 -> "minecraft:repeating_command_block"
            211 -> "minecraft:chain_command_block"
            212 -> "minecraft:blue_ice"
            else -> "minecraft:stone"
        }
    }
}
