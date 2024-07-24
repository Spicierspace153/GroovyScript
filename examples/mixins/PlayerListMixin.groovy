package mixins

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.TextComponentString;

@Mixin(targets='net.minecraft.server.management.PlayerList', remap=false)
class PlayerListMixin {

    @Inject(method = "initializeConnectionToPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;firePlayerLoggedIn(Lnet/minecraft/entity/player/EntityPlayer;)V"))
    void init(NetworkManager netManager, EntityPlayerMP playerIn, NetHandlerPlayServer nethandlerplayserver, CallbackInfo ci) {
        playerIn.sendMessage(new TextComponentString("Player " + playerIn.getName() + " logged in"));
    }

}

