package cool.furry.mc.forge.projectexpansion.mixin;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection; Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"))
    public void placeNewPlayer(Connection connection, ServerPlayer player, CallbackInfo ci) {
        if(!Util.offlinePlayerDataCache.containsKey(player.getUUID()))return;
        try {
            Util.saveOfflineKnowledgeProvider(player.getUUID());
            Util.offlineKnowledgeProviders.remove(player.getUUID());
            Util.offlinePlayerDataCache.remove(player.getUUID());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
