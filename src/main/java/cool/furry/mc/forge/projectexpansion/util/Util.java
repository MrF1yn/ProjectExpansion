package cool.furry.mc.forge.projectexpansion.util;

import com.google.common.base.Preconditions;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static cool.furry.mc.forge.projectexpansion.util.Util.getOfflineKnowledgeProvider;
import static moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;

@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);
    public static Map<UUID, IKnowledgeProvider> offlineKnowledgeProviders = new HashMap<>();
    public static Map<UUID, CompoundTag> offlinePlayerDataCache = new HashMap<>();

    public static @Nullable
    ServerPlayer getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    public static @Nullable
    ServerPlayer getPlayer(@Nullable Level level, UUID uuid) {
        return level == null || level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (stackCopy.isDamageableItem()) stackCopy.setDamageValue(0);
        return NBTManager.getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, Item rawItem, Item cleanItem) {
        return addKnowledge(player, provider, ItemInfo.fromItem(rawItem), ItemInfo.fromItem(cleanItem));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemStack rawStack, ItemStack cleanStack) {
        return addKnowledge(player, provider, ItemInfo.fromStack(rawStack), ItemInfo.fromStack(cleanStack));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemInfo rawInfo, ItemInfo cleanInfo) {
        if (cleanInfo.createStack().isEmpty()) return AddKnowledgeResult.FAIL;

        if (!provider.hasKnowledge(cleanInfo)) {
            if (MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, rawInfo, cleanInfo)))
                return AddKnowledgeResult.FAIL;

            provider.addKnowledge(cleanInfo);
            return AddKnowledgeResult.SUCCESS;
        }

        return AddKnowledgeResult.UNKNOWN;
    }

    public static int safeIntValue(BigInteger val) {
        try {
            return val.intValueExact();
        } catch (ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public static int safeIntValue(BigDecimal val) {
        try {
            return val.intValueExact();
        } catch (ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public static long safeLongValue(BigInteger val) {
        try {
            return val.longValueExact();
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    public static int mod(int a, int b) {
        a = a % b;
        return a < 0 ? a + b : a;
    }

    public static void markDirty(BlockEntity block) {
        if (block.getLevel() != null) markDirty(block.getLevel(), block);
    }

    public static void markDirty(Level level, BlockEntity block) {
        markDirty(level, block.getBlockPos());
    }

    public static void markDirty(Level level, BlockPos pos) {
        level.getChunkAt(pos).setUnsaved(true);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(UUID uuid) {
        @Nullable ServerPlayer player = getPlayer(uuid);
        if(player == null) {
            return getOfflineKnowledgeProvider(uuid);
        }
        return getKnowledgeProvider(player);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(Player player) {
        try {
            return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElseThrow(NullPointerException::new);
        } catch(NullPointerException ignore) {
            return null;
        }
    }

    public static void saveOfflineKnowledgeProvider(UUID uuid) throws IOException {
        CompoundTag knowledgeProvider = offlineKnowledgeProviders.get(uuid).serializeNBT();
        CompoundTag playerDat = offlinePlayerDataCache.get(uuid);
        playerDat.getCompound("ForgeCaps").put(KnowledgeImpl.Provider.NAME.toString(), knowledgeProvider);
        NbtIo.writeCompressed(playerDat,
                new File(ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), uuid.toString() + ".dat"));
        PECore.LOGGER.info("Saved offline knowledge provider for " + uuid.toString());
    }


    public static @Nullable IKnowledgeProvider getOfflineKnowledgeProvider(UUID uuid) {
        if (offlineKnowledgeProviders.containsKey(uuid)) {
            return offlineKnowledgeProviders.get(uuid);
        }
//        Preconditions.checkState(Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER, "CRITICAL: Trying to read filesystem on client!!");
        if(Thread.currentThread().getThreadGroup() == SidedThreadGroups.CLIENT)return null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        File playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        if (playerData.exists()) {
            File player = new File(playerData, uuid.toString() + ".dat");
            if (player.exists() && player.isFile()) {
                try {
                    FileInputStream in = new FileInputStream(player);
                    IKnowledgeProvider provider = KnowledgeImpl.getDefault();
                    try {
                        CompoundTag playerDat = NbtIo.readCompressed(in);
                        CompoundTag knowledgeProvider = playerDat.getCompound("ForgeCaps").getCompound(KnowledgeImpl.Provider.NAME.toString());
                        provider.deserializeNBT(knowledgeProvider);
                        PECore.debugLog("Reading offline data for UUID: {}", new Object[]{uuid});
                        offlineKnowledgeProviders.put(uuid, provider);
                        offlinePlayerDataCache.put(uuid, playerDat);
                    } catch (Throwable var10) {
                        try {
                            in.close();
                        } catch (Throwable var9) {
                            var10.addSuppressed(var9);
                        }

                        throw var10;
                    }
                    in.close();
                    return provider;
                } catch (IOException var11) {
                    PECore.LOGGER.warn("Failed to read offline data for API calls for UUID: {}", uuid);
                }
            }
        }
        return null;
    }

    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList) {
        return spreadEMC(emc, storageList, null);
    }
    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList, @Nullable Long maxPer) {
        if(emc.equals(BigInteger.ZERO) || storageList.isEmpty()) return emc;
        List<IEmcStorage> notAccepting = new ArrayList<>();
        emc = stepBigInteger(emc, (val) -> {
            long div = val / storageList.size();
            if(maxPer != null && maxPer < div) div = maxPer;
            parentLoop: while (val > 0 && notAccepting.size() < storageList.size()) {
                for(IEmcStorage storage : storageList) {
                    if(notAccepting.contains(storage)) continue;
                    if(val == 0) break parentLoop;
                    if(val < div) div = val;
                    long oldVal = val;
                    val -= storage.insertEmc(div, EmcAction.EXECUTE);
                    if(val.equals(oldVal)) notAccepting.add(storage);
                    if(maxPer != null && oldVal - val >= maxPer) notAccepting.add(storage);
                }
            }
            return val;
        });
        return emc;
    }

    public static BigInteger stepBigInteger(BigInteger value, Function<Long, Long> func) {
        return stepBigInteger(value, Long.MAX_VALUE, func);
    }

    public static BigInteger stepBigInteger(BigInteger value, Long step, Function<Long, Long> func) {
        return stepBigInteger(value, step, (a, b) -> func.apply(a));
    }

    public static BigInteger stepBigInteger(BigInteger value, BiFunction<Long, BigInteger, Long> func) {
        return stepBigInteger(value, Long.MAX_VALUE, func);
    }

    // consumer values: step, leftover
    // consumer return: leftover (from step)
    public static BigInteger stepBigInteger(BigInteger value, Long step, BiFunction<Long, BigInteger, Long> func) {
        if(value.equals(BigInteger.ZERO)) return value;
        long localValue;
        while((localValue = Math.min(step, safeLongValue(value))) > 0L) {
            value = value.subtract(BigInteger.valueOf(localValue));
            Long unused = func.apply(localValue, value);
            if(unused > 0L) {
                value = value.add(BigInteger.valueOf(unused));
                break;
            }
        }
        return value;
    }

    public enum AddKnowledgeResult {
        FAIL,
        UNKNOWN,
        SUCCESS,
    }
}
