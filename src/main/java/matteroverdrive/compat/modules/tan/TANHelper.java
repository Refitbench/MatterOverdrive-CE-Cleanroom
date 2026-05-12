package matteroverdrive.compat.modules.tan;

/**
 * Lightweight sentinel for ToughAsNails presence.
 */
public class TANHelper {
    public static boolean isLoaded = false;
    /** Cached at init; avoids a SyncedConfig lookup every tick. */
    public static boolean thirstEnabled = false;
    public static boolean temperatureEnabled = false;
    public static boolean temperatureSuppressMode = false;
    /** True when TAN is loaded AND MO TAN compat is enabled in config. */
    public static boolean enabled = false;
    public static int thirstEnergyCost = 256;
    public static int temperatureEnergyCost = 128;
}
