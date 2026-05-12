package matteroverdrive.compat.modules.tan;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.compat.Compat;
import matteroverdrive.entity.android_player.AndroidPlayer;
import matteroverdrive.init.OverdriveBioticStats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import toughasnails.api.TANBlocks;
import toughasnails.api.config.GameplayOption;
import toughasnails.api.config.SyncedConfig;
import toughasnails.api.stat.capability.ITemperature;
import toughasnails.api.stat.capability.IThirst;
import toughasnails.api.temperature.Temperature;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.temperature.TemperatureScale;
import toughasnails.api.thirst.ThirstHelper;

@Compat(CompatTAN.ID)
public class CompatTAN {

    public static final String ID = "toughasnails";

    @SuppressWarnings("null")
    @Compat.Init
    public static void init(FMLInitializationEvent event) {
        TANHelper.isLoaded = true;
        TANHelper.thirstEnabled = SyncedConfig.getBooleanValue(GameplayOption.ENABLE_THIRST);
        TANHelper.temperatureEnabled = SyncedConfig.getBooleanValue(GameplayOption.ENABLE_TEMPERATURE);
        TANHelper.enabled = MatterOverdrive.CONFIG_HANDLER.tanCompatEnabled;
        TANHelper.thirstEnergyCost = MatterOverdrive.CONFIG_HANDLER.tanThirstEnergyCost;
        TANHelper.temperatureEnergyCost = MatterOverdrive.CONFIG_HANDLER.tanTemperatureEnergyCost;
        TANHelper.temperatureSuppressMode = MatterOverdrive.CONFIG_HANDLER.tanTemperatureSuppressMode;
        // Set unlock item requirements only when TAN is present.
        OverdriveBioticStats.tanTemperature.addReqiredItm(new ItemStack(TANBlocks.temperature_coil, 5, 0)); // cooling coil
        OverdriveBioticStats.tanTemperature.addReqiredItm(new ItemStack(TANBlocks.temperature_coil, 5, 1)); // heating coil
    }

    /**
     * Extracts RF proportional to thirst deficit, restores that many thirst points.
     * Only called when android has sufficient power.
     */
    public static void suppressThirst(AndroidPlayer android, int energyPerPoint) {
        if (!TANHelper.thirstEnabled) return;
        IThirst thirst = ThirstHelper.getThirstData(android.getPlayer());
        if (thirst == null) return;
        int thirstNeeded = 20 - thirst.getThirst();
        float exhaustion = thirst.getExhaustion();
        // Skip all work when thirst is full and TAN hasn't accumulated any exhaustion yet.
        if (thirstNeeded <= 0 && exhaustion <= 0.0f) return;
        if (thirstNeeded > 0) {
            int extracted = android.extractEnergyRaw(thirstNeeded * energyPerPoint, false);
            int pointsRestored = extracted / energyPerPoint;
            if (pointsRestored > 0) {
                thirst.setThirst(thirst.getThirst() + pointsRestored);
                thirst.setHydration(5.0f);
            }
        }
        if (exhaustion > 0.0f) {
            thirst.setExhaustion(0.0f);
        }
    }

    /**
     * Regulates android body temperature according to the configured mode.
     *
     * Suppress mode:
     *   Jumps directly to neutral. No energy cost. Hides TAN thermometer HUD.
     *
     * Threshold mode:
     *   Acts when temperature leaves the safe ranges. Nudges one step toward
     *   neutral and extracts energyPerStep RF. If lacks energy the correction 
     *   is skipped, allowing drift.
     */
    public static void suppressTemperature(AndroidPlayer android, int energyPerStep) {
        if (!TANHelper.temperatureEnabled) return;
        ITemperature temp = TemperatureHelper.getTemperatureData(android.getPlayer());
        if (temp == null) return;
        int current = temp.getTemperature().getRawValue();
        int neutral = TemperatureScale.getScaleMidpoint();

        if (TANHelper.temperatureSuppressMode) {
            // Freeze directly at neutral — no energy cost.
            int delta = current - neutral;
            if (delta != 0) {
                temp.addTemperature(new Temperature(-delta));
            }
        } else {
            // ICY = 0-5, HOT = 20-25.
            // COOL = 6-10, WARM = 15-19.
            if (current > 6 && current < 19) return;
            int delta = current - neutral;
            if (delta == 0) return;
            int direction = (int) Math.signum(-delta);
            int extracted = android.extractEnergyRaw(energyPerStep, false);
            if (extracted >= energyPerStep) {
                temp.addTemperature(new Temperature(direction));
            }
        }
    }
}
