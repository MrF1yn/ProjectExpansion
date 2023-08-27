package cool.furry.mc.forge.projectexpansion;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.*;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "projectexpansion";
    public static CreativeModeTab tab;
    @SuppressWarnings("unused")
    public static final org.apache.logging.log4j.Logger Logger = LogManager.getLogger();

    public Main() {
        tab = new CreativeModeTab(MOD_ID) {

            @Override
            @Nonnull
            @OnlyIn(Dist.CLIENT)
            public ItemStack makeIcon() {
                return new ItemStack(Matter.FADING.getMatter());
            }
        };
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BlockEntityTypes.Registry.register(bus);
        Blocks.Registry.register(bus);
        Enchantments.Registry.register(bus);
        Items.Registry.register(bus);
        SoundEvents.Registry.register(bus);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldSave);
//        MinecraftForge.EVENT_BUS.addListener(this::onLoadFromFile);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.Spec, "project-expansion.toml");

        Fuel.registerAll();
        Matter.registerAll();
        Star.registerAll();
        AdvancedAlchemicalChest.register();
    }



    private void onWorldSave(WorldEvent.Save event){
        Util.offlinePlayerDataCache.entrySet().removeIf(e -> {
            try {
                Util.saveOfflineKnowledgeProvider(e.getKey());
                Util.offlineKnowledgeProviders.remove(e.getKey());
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return true;
        });
    }

//    private void onLoadFromFile(PlayerEvent.LoadFromFile event) {
//        if(!Util.offlinePlayerDataCache.containsKey(event.getPlayer().getUUID()))return;
//        try {
//            Util.saveOfflineKnowledgeProvider(event.getPlayer().getUUID());
//            Util.offlineKnowledgeProviders.remove(event.getPlayer().getUUID());
//            Util.offlinePlayerDataCache.remove(event.getPlayer().getUUID());
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem().equals(Items.INFINITE_FUEL.get())) {
                    stack.getOrCreateTag().putUUID(TagNames.OWNER, player.getUUID());
                    continue;
                }
                boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALCHEMICAL_COLLECTION.get(), stack) > 0;
                if(hasEnch && !stack.getOrCreateTag().contains(TagNames.ALCHEMICAL_COLLECTION_ENABLED)) {
                    stack.getOrCreateTag().putBoolean(TagNames.ALCHEMICAL_COLLECTION_ENABLED, true);
                }
            }
        }
    }
}
