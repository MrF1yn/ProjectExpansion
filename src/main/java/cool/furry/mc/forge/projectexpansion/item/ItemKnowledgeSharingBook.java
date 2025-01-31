package cool.furry.mc.forge.projectexpansion.item;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.SoundEvents;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemKnowledgeSharingBook extends Item {
    public ItemKnowledgeSharingBook() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).tab(Main.tab));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(player.isCrouching()) {
            if(!level.isClientSide) {
                CompoundTag nbt = stack.getOrCreateTag();
                nbt.putUUID(TagNames.OWNER, player.getUUID());
                nbt.putString(TagNames.OWNER_NAME, player.getScoreboardName());
                level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_STORE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                player.displayClientMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.stored").setStyle(ColorStyle.GREEN), true);
            }
            
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            CompoundTag nbt = stack.getOrCreateTag();
            if(nbt.hasUUID(TagNames.OWNER)) {
                UUID owner = nbt.getUUID(TagNames.OWNER);
                if(player.getUUID().equals(nbt.getUUID(TagNames.OWNER))) {
                    player.displayClientMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.self").setStyle(ColorStyle.RED), true);
                    return InteractionResultHolder.fail(stack);
                }
                if(!level.isClientSide) {
                    @Nullable IKnowledgeProvider ownerProvider = Util.getKnowledgeProvider(owner);
                    @Nullable IKnowledgeProvider learnerProvider = Util.getKnowledgeProvider(player);
                    if(ownerProvider == null) {
                        player.displayClientMessage(Component.translatable("text.projectexpansion.failed_to_get_knowledge_provider", Util.getPlayer(owner) == null ? owner : Objects.requireNonNull(Util.getPlayer(owner)).getDisplayName()).setStyle(ColorStyle.RED), true);
                        return InteractionResultHolder.fail(stack);
                    }
                    if(learnerProvider == null) {
                        player.displayClientMessage(Component.translatable("text.projectexpansion.failed_to_get_knowledge_provider", player.getDisplayName()).setStyle(ColorStyle.RED), true);
                        return InteractionResultHolder.fail(stack);
                    }
                    long learned = 0;
                    for(ItemInfo info : ownerProvider.getKnowledge()) {
                        if(!learnerProvider.hasKnowledge(info)) {
                            if(Config.notifyKnowledgeBookGains.get() && learned < 100) {
                                player.sendSystemMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.learned", info.createStack().getDisplayName()).setStyle(ColorStyle.GREEN));
                            }
                            learnerProvider.addKnowledge(info);
                            learned++;
                        }
                    }
                    nbt.putLong(TagNames.LAST_USED, level.getGameTime());
                    nbt.putLong(TagNames.KNOWLEDGE_GAINED, learned);
                    if(learned > 0) {
                        learnerProvider.sync((ServerPlayer) player);
                        if(learned > 100) {
                            player.sendSystemMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.learned_over_100", learned - 100).setStyle(ColorStyle.GREEN));
                        }
                        player.displayClientMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.learned_total", learned, Component.literal(nbt.getString(TagNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)).setStyle(ColorStyle.GREEN), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    } else {
                        player.displayClientMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.no_new_knowledge").setStyle(ColorStyle.RED), true);
                        level.playSound(null, player.position().x, player.position().y, player.position().z, SoundEvents.KNOWLEDGE_SHARING_BOOK_USE_NONE.get(), SoundSource.PLAYERS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                    }
                } else {
                    long gained = nbt.getLong(TagNames.KNOWLEDGE_GAINED);
                    for(int i = 0; i < 5; i++) {
                        Vec3 v1 = new Vec3(((double) level.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                            .xRot(-player.getRotationVector().x * 0.017453292F)
                            .yRot(-player.getRotationVector().y * 0.017453292F);
                        Vec3 v2 = new Vec3(((double) level.random.nextFloat() - 0.5D) * 0.3D, (double) (-level.random.nextFloat()) * 0.6D - 0.3D, 0.6D)
                            .xRot(-player.getRotationVector().x * 0.017453292F)
                            .yRot(-player.getRotationVector().y * 0.017453292F)
                            .add(player.position().x, player.position().y + (double) player.getEyeHeight(), player.position().z);
                        level.addParticle(gained > 0 ? new ItemParticleOption(ParticleTypes.ITEM, stack) : ParticleTypes.SMOKE, v2.x, v2.y, v2.z, v1.x, v1.y + 0.05D, v1.z);
                    }
                }

                stack.shrink(1);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            } else {
                player.displayClientMessage(Component.translatable("item.projectexpansion.knowledge_sharing_book.no_owner").setStyle(ColorStyle.RED), true);
                return InteractionResultHolder.fail(stack);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().hasUUID(TagNames.OWNER);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag nbt = stack.getOrCreateTag();
        if(nbt.hasUUID(TagNames.OWNER)) {
            tooltip.add(Component.translatable("item.projectexpansion.knowledge_sharing_book.selected", Component.literal(nbt.getString(TagNames.OWNER_NAME)).setStyle(ColorStyle.AQUA)).setStyle(ColorStyle.GRAY));
        }
    }
}