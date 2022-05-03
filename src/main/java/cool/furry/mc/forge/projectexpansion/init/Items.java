package cool.furry.mc.forge.projectexpansion.init;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.item.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
public class Items {
    public static final DeferredRegister<Item> Registry = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> FINAL_STAR_SHARD = Registry.register("final_star_shard", ItemFinalStarShard::new);
    public static final RegistryObject<Item> FINAL_STAR = Registry.register("final_star", ItemFinalStar::new);
    public static final RegistryObject<Item> MATTER_UPGRADER = Registry.register("matter_upgrader", ItemMatterUpgrader::new);
    public static final RegistryObject<Item> INFINITE_FUEL = Registry.register("infinite_fuel", ItemInfiniteFuel::new);
    public static final RegistryObject<Item> INFINITE_STEAK = Registry.register("infinite_steak", ItemInfiniteSteak::new);
    public static final RegistryObject<BlockItem> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", () -> new BlockItem(Blocks.TRANSMUTATION_INTERFACE.get(), new Item.Properties().group(Main.group).rarity(Rarity.EPIC)));
    public static final RegistryObject<BlockItem> MATTER_REPLICATOR = Registry.register("matter_replicator", () -> new BlockItem(Blocks.MATTER_REPLICATOR.get(), new Item.Properties().group(Main.group).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SPEED_UPGRADE = Registry.register("speed_upgrade", () -> new ItemUpgrade(ItemUpgrade.UpgradeType.SPEED));
    public static final RegistryObject<Item> STACK_UPGRADE = Registry.register("stack_upgrade", () -> new ItemUpgrade(ItemUpgrade.UpgradeType.STACK));
}
