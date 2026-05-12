package matteroverdrive.mixins.client.tan;

import matteroverdrive.compat.modules.tan.TANHelper;
import matteroverdrive.entity.android_player.AndroidPlayer;
import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import matteroverdrive.init.OverdriveBioticStats;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import toughasnails.handler.temperature.TemperatureOverlayHandler;

/**
 * Suppresses TAN's temperature thermometer overlays when an android has the 
 * Thermal Regulation bionetic stat active.
 */
@SideOnly(Side.CLIENT)
@Mixin(value = TemperatureOverlayHandler.class, remap = false)
public class MixinTemperatureOverlayHandler {

    @Inject(method = "onPostRenderOverlay", at = @At("HEAD"), cancellable = true)
    private void mo_suppressForAndroid(RenderGameOverlayEvent.Post event, CallbackInfo ci) {
        ElementType type = event.getType();
        if (type != ElementType.EXPERIENCE && type != ElementType.PORTAL) return;

        // Only suppress HUD in suppress mode. Threshold mode leaves the thermometer visible
        // so the player can see their temperature and manage it to save power.
        if (!TANHelper.temperatureSuppressMode) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        AndroidPlayer android = MOPlayerCapabilityProvider.GetAndroidCapability(mc.player);
        if (android.isAndroid()
                && android.isUnlocked(OverdriveBioticStats.tanTemperature, 1)
                && OverdriveBioticStats.tanTemperature.isEnabled(android, 1)) {
            ci.cancel();
        }
    }
}
