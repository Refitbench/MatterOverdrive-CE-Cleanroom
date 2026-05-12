package matteroverdrive.data.biostats;

import com.google.common.collect.Multimap;
import matteroverdrive.compat.modules.tan.CompatTAN;
import matteroverdrive.compat.modules.tan.TANHelper;
import matteroverdrive.client.render.HoloIcons;
import matteroverdrive.entity.android_player.AndroidPlayer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BioticStatTANTemperature extends AbstractBioticStat {

    public BioticStatTANTemperature(String name, int xp) {
        super(name, xp);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(TextureMap textureMap, HoloIcons holoIcons) {
        this.icon = holoIcons.getIcon("temperature");
    }

    @Override
    public void onAndroidUpdate(AndroidPlayer android, int level) {
        if (TANHelper.isLoaded && !android.getPlayer().world.isRemote
                && android.getPlayer().ticksExisted % 29 == 0
                && isEnabled(android, level)) {
            CompatTAN.suppressTemperature(android, TANHelper.temperatureEnergyCost);
        }
    }

    @Override
    public boolean isEnabled(AndroidPlayer android, int level) {
        return super.isEnabled(android, level) && android.getEnergyStored() > 0;
    }

    @Override
    public void onActionKeyPress(AndroidPlayer androidPlayer, int level, boolean server) {
    }

    @Override
    public void onKeyPress(AndroidPlayer androidPlayer, int level, int keycode, boolean down) {
    }

    @Override
    public void onLivingEvent(AndroidPlayer androidPlayer, int level, LivingEvent event) {
    }

    @Override
    public void changeAndroidStats(AndroidPlayer androidPlayer, int level, boolean enabled) {
    }

    @Override
    public Multimap<String, AttributeModifier> attributes(AndroidPlayer androidPlayer, int level) {
        return null;
    }

    @Override
    public boolean isActive(AndroidPlayer androidPlayer, int level) {
        return false;
    }

    @Override
    public int getDelay(AndroidPlayer androidPlayer, int level) {
        return 0;
    }

    @Override
    public boolean showOnHud(AndroidPlayer android, int level) {
        return false;
    }
}
