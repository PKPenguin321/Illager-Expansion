package me.sandbox.block;

import me.sandbox.item.ModItemGroup;
import me.sandbox.sounds.ModBlockSoundGroup;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import me.sandbox.Sandbox;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BlockRegistry {

    // Ores
    public static final Block endergon_ore = registerBlock("endergon_ore",
            new Block(FabricBlockSettings.of(Material.STONE).sounds(ModBlockSoundGroup.ENDERGON_ORE).strength(6f).requiresTool()), ModItemGroup.SandBoxDecorations);

    //Decoration Blocks

    //Coloured Lights
    public static final Block black_light = registerBlock("black_light",
            new Block(FabricBlockSettings.of(Material.REDSTONE_LAMP).strength(0.2f).luminance(1)), ModItemGroup.SandBoxDecorations);




    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new Identifier(Sandbox.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        return Registry.register(Registry.ITEM, new Identifier(Sandbox.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(group)));
    }

    public static void registerModBlocks() {
        Sandbox.LOGGER.info("Registering blocks...");
    }
}