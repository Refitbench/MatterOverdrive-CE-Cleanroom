package matteroverdrive.handler;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.Reference;
import matteroverdrive.entity.android_player.AndroidPlayer;
import matteroverdrive.util.IConfigSubscriber;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigurationHandler {
	public static final String CATEGORY_MATTER = "matter registry";
	public static final String CATEGORY_WEAPON = "weapon values";
	
	public static final String KEY_WEAPON_ENERGY_PACK_VALUE = "energy pack value";
	public static final String KEY_WEAPON_PHASER_RANGE_VALUE = "Phaser range";
	public static final String KEY_WEAPON_PHASER_HEAT_VALUE = "Phaser max heat";
	public static final String KEY_WEAPON_PHASER_DAMAGE_VALUE = "Phaser base damage";
	public static final String KEY_WEAPON_PHASER_CAPACITY_VALUE = "Phaser energy cap";
	public static final String KEY_WEAPON_PHASER_COOLDOWN_VALUE = "Phaser shot cooldown";
	public static final String KEY_WEAPON_PHASER_USE_VALUE = "Phaser max use time";
	public static final String KEY_WEAPON_PHASER_ENERGY_VALUE = "Phaser energy per shot";
	public static final String KEY_WEAPON_PHASERRIFLE_RANGE_VALUE = "Phaser Rifle range";
	public static final String KEY_WEAPON_PHASERRIFLE_HEAT_VALUE = "Phaser Rifle max heat";
	public static final String KEY_WEAPON_PHASERRIFLE_DAMAGE_VALUE = "Phaser Rifle base damage";
	public static final String KEY_WEAPON_PHASERRIFLE_CAPACITY_VALUE = "Phaser Rifle energy cap";
	public static final String KEY_WEAPON_PHASERRIFLE_SPEED_VALUE = "Phaser Rifle shot speed";
	public static final String KEY_WEAPON_PHASERRIFLE_COOLDOWN_VALUE = "Phaser Rifle shot cooldown";
	public static final String KEY_WEAPON_PHASERRIFLE_USE_VALUE = "Phaser Rifle max use time";
	public static final String KEY_WEAPON_PHASERRIFLE_ENERGY_VALUE = "Phaser Rifle energy per shot";
	public static final String KEY_WEAPON_PLASMASHOTGUN_RANGE_VALUE = "Plasma Shotgun range";
	public static final String KEY_WEAPON_PLASMASHOTGUN_HEAT_VALUE = "Plasma Shotgun max heat";
	public static final String KEY_WEAPON_PLASMASHOTGUN_DAMAGE_VALUE = "Plasma Shotgun base damage";
	public static final String KEY_WEAPON_PLASMASHOTGUN_CAPACITY_VALUE = "Plasma Shotgun energy cap";
	public static final String KEY_WEAPON_PLASMASHOTGUN_SPEED_VALUE = "Plasma Shotgun shot speed";
	public static final String KEY_WEAPON_PLASMASHOTGUN_COOLDOWN_VALUE = "Plasma Shotgun shot cooldown";
	public static final String KEY_WEAPON_PLASMASHOTGUN_CHARGE_VALUE = "Plasma Shotgun max charge time";
	public static final String KEY_WEAPON_PLASMASHOTGUN_ENERGY_VALUE = "Plasma Shotgun energy per shot";
	public static final String KEY_WEAPON_ION_RANGE_VALUE = "ion sniper range";
	public static final String KEY_WEAPON_ION_HEAT_VALUE = "ion sniper max heat";
	public static final String KEY_WEAPON_ION_DAMAGE_VALUE = "ion sniper base damage";
	public static final String KEY_WEAPON_ION_CAPACITY_VALUE = "ion sniper energy cap";
	public static final String KEY_WEAPON_ION_SPEED_VALUE = "ion sniper shot speed";
	public static final String KEY_WEAPON_ION_COOLDOWN_VALUE = "ion sniper shot cooldown";
	public static final String KEY_WEAPON_ION_USE_VALUE = "ion sniper max use time";
	public static final String KEY_WEAPON_ION_ENERGY_VALUE = "ion sniper energy per shot";
	public static final String KEY_WEAPON_OMNITOOL_RANGE_VALUE = "Omni Tool range";
	public static final String KEY_WEAPON_OMNITOOL_HEAT_VALUE = "Omni Tool max heat";
	public static final String KEY_WEAPON_OMNITOOL_DAMAGE_VALUE = "Omni Tool base damage";
	public static final String KEY_WEAPON_OMNITOOL_CAPACITY_VALUE = "Omni Tool energy cap";
	public static final String KEY_WEAPON_OMNITOOL_SPEED_VALUE = "Omni Tool shot speed";
	public static final String KEY_WEAPON_OMNITOOL_COOLDOWN_VALUE = "Omni Tool shot cooldown";
	public static final String KEY_WEAPON_OMNITOOL_USE_VALUE = "Omni Tool max use time";
	public static final String KEY_WEAPON_OMNITOOL_ENERGY_VALUE = "Omni Tool energy per shot";
	
	public static final String CATEGORY_WORLD_GEN = "world gen";
	public static final String CATEGORY_MACHINES = "machine options";
	public static final String CATEGORY_MATTER_ITEMS = CATEGORY_MATTER + ".add matter";
	public static final String CATEGORY_WORLD_SPAWN_ORES = "spawn ores";
	public static final String CATEGORY_WORLD_SPAWN_OTHER = "spawn other";
	public static final String CATEGORY_WORLD_SPAWN = "spawn";
	public static final String CATEGORY_ABILITIES = "abilities";
	public static final String CATEGORY_CLIENT = "client";
	public static final String CATEGORY_ANDROID_HUD = CATEGORY_CLIENT + "." + "android_hud";
	public static final String CATEGORY_SERVER = "server";
	public static final String CATEGORY_STARMAP = "starmap";
	public static final String KEY_STARMAP_ENABLED = "starmap_enabled";
	public static final String CATEGORY_ENCHANTMENTS = "enchantments";
	public static final String CATEGORY_ENTITIES = "entities";
	public static final String CATEGORY_ANDROID_PLAYER = CATEGORY_ENTITIES + "." + "android_player";
	public static final String CATEGORY_GENERAL = "_general";
	public static final String CATEGORY_COMPATIBILITY = "compatibility";
	public static final String CATEGORY_DEBUG = "debug";
	public static final String KEY_AUTOMATIC_RECIPE_CALCULATION = "automatic matter calculation from recipe";
	public static final String KEY_AUTOMATIC_FURNACE_CALCULATION = "automatic matter calculation from furnace";
	public static final String KEY_AUTOMATIC_INSCRIBER_CALCULATION = "automatic matter calculation from inscriber";
	public static final String KEY_MBLACKLIST = "blacklist";
	public static final String KEY_BLACKLIST_MODS = "mod_blacklist";
	public static final String KEY_VERSION_CHECK_CHAT = "version_check_chat";
	public static final String KEY_GRAVITATIONAL_ANOMALY_FALLING_BLOCKS = "gravitational anomaly falling blocks";
	public static final String KEY_GRAVITATIONAL_ANOMALY_BLOCK_ENTITIES = "gravitational anomaly block entities";
	public static final String KEY_GRAVITATIONAL_ANOMALY_SPAWN_CHANCE = "gravitational anomaly spawn chance";
	public static final String KEY_GRAVITATIONAL_ANOMALY_SPAWN_LEVEL = "gravitational anomaly spawn y level";
	public static final String KEY_GRAVITATIONAL_ANOMALY_VANILLA_FLUIDS = "gravitational anomaly vanilla fluids";
	public static final String KEY_GRAVITATIONAL_ANOMALY_FORGE_FLUIDS = "gravitational anomaly forge fluids";
	public static final String KEY_GRAVITATIONAL_ANOMALY_SCAN_BATCH_SIZE = "scan batch size";
	public static final String KEY_GRAVITATIONAL_ANOMALY_BLOCKS_PER_BATCH = "blocks per batch";
	public static final String KEY_GRAVITATIONAL_ANOMALY_BATCH_TICK_RATE = "batch tick rate";
	public static final String KEY_GRAVITATIONAL_ANOMALY_IDLE_SCAN_TICKS = "idle scan ticks";
	public static final String KEY_GRAVITATIONAL_ANOMALY_ENTITY_SCAN_RATE = "entity scan rate";
	public static final String KEY_GRAVITATIONAL_ANOMALY_DEBUG_SCAN_WIREFRAME = "gravitational anomaly scan wireframe debug";
	public static final String KEY_EQUALIZER_DAMAGE_RATE = "equalizer damage rate";
	public static final String KEY_GRAVITATIONAL_STABILIZER_ENERGY_CAPACITY = "gravitational stabilizer energy capacity";
	public static final String KEY_GRAVITATIONAL_STABILIZER_MAX_ENERGY_RECEIVE = "gravitational stabilizer max energy receive";
	public static final String KEY_GRAVITATIONAL_STABILIZER_BASE_ENERGY_PER_TICK = "gravitational stabilizer base energy per tick";
	public static final String KEY_GRAVITATIONAL_STABILIZER_MAX_ENERGY_PER_TICK = "gravitational stabilizer max energy per tick";
	public static final String KEY_GRAVITATIONAL_STABILIZER_RESCAN_RATE = "gravitational stabilizer rescan rate";
	public static final String KEY_HOUSE = "generate house";
	public static final String KEY_ANDROID_HOUSE_SPAWN_CHANCE = "android house spawn chance";
	public static final String KEY_SAND_PIT_SPAWN_CHANCE = "sand pit spawn chance";
	public static final String KEY_CRASHED_SHIP_SPAWN_CHANCE = "crashed ship spawn chance";
	public static final String KEY_UNDERWATER_BASE_SPAWN_CHANCE = "underwater base spawn chance";
	public static final String KEY_CARGO_SHIP_SPAWN_CHANCE = "cargo ship spawn chance";
	public static final String KEY_TRITANIUM_VEINS_PER_CHUNK = "tritanium veins per chunk";
	public static final String KEY_TRITANIUM_VEIN_SIZE = "tritanium vein size";
	public static final String KEY_DILITHIUM_VEINS_PER_CHUNK = "dilithium veins per chunk";
	public static final String KEY_DILITHIUM_VEIN_SIZE = "dilithium vein size";
	public static final String KEY_ANDROID_ENERGY_WATCH_ID = "android energy watch id";
	public static final String KEY_MATTER_REGISTRATION_DEBUG = "matter registration";
	public static final String KEY_STRUCTURE_REGISTRATION_DEBUG = "structure worldgen registration";
	public static final String KEY_MATTER_CALCULATION_DEBUG = "matter calculation";

	public final File configDir;

	public final Configuration config;
	private final Set<IConfigSubscriber> subscribers;
	public boolean starmapEnabled;
	public boolean pylonEnabled;
	public boolean showFilledItems;
	public boolean showInDevItems;
	public boolean legacyTritaniumCrateMigrationEnabled;
	public boolean versionCheckChat;

	public ConfigurationHandler(File configDir) {
		this.configDir = configDir;
		config = new Configuration(new File(configDir, "MatterOverdrive/MatterOverdrive.cfg"), "1.0");
		subscribers = new HashSet<>();
	}

	public void init() {
		config.load();
		ConfigCategory category = config.getCategory(CATEGORY_MATTER);
		category.setComment("Configuration for the Matter ");
		updateCategoryLang(category);
		config.get(CATEGORY_MATTER, KEY_MBLACKLIST, new String[0]).setComment(
				"Blacklist for items in the matter registry. Automatic Recipe calculation will ignore recipes with these items. Just add the unlocalized name or the ore dictionary name in the list.");
		config.get(CATEGORY_MATTER, KEY_BLACKLIST_MODS, new String[0]).setComment(
				"Blacklist for mods (mod ID). Automatic Recipe calculation will ignore recipes with items from this mod");
		category = config.getCategory(CATEGORY_MATTER_ITEMS);
		category.setComment(
				"Registration of new items and the amount of matter they contain. Add them like so: I:[registered name or ore Dictionary name](meta)=[matter amount]. () - optional parameter. Example I:dye2=10 I:egg=29");
		updateCategoryLang(category);
		category = config.getCategory(CATEGORY_CLIENT);
		updateCategoryLang(category);
		category.setComment("Options for the Matter overdrive client");
		category = config.getCategory(CATEGORY_SERVER);
		updateCategoryLang(category);
		category.setComment("Options for the Matter Overdrive server");
		category = config.getCategory(CATEGORY_ENTITIES);
		updateCategoryLang(category);
		category.setComment("Options for Matter Overdrive Entities. Such as their Entity IDs.");
		category = config.getCategory(CATEGORY_DEBUG);
		updateCategoryLang(category);
		category.setComment("Debug Options. Such as Debug Log for Matter Recipe Calculation");
		starmapEnabled = config.getBoolean(KEY_STARMAP_ENABLED, CATEGORY_DEBUG, false,
				"[INCOMPLETE FEATURE] Enable the Starmap feature. Under active development.");
		pylonEnabled = config.getBoolean("dimensional_pylon_enabled", CATEGORY_DEBUG, false,
				"[INCOMPLETE FEATURE] Enable the Dimensional Pylon feature. Under active development.");
		showInDevItems = config.getBoolean("showInDevItems", CATEGORY_DEBUG, false,
				"Shows items which are incomplete / have no function");
		category = config.getCategory(CATEGORY_ANDROID_HUD);
		updateCategoryLang(category);
		category.setComment("Positioning and colors of Android HUD elements");
		category = config.getCategory(CATEGORY_WEAPON);
		updateCategoryLang(category);
		category.setComment("Weapon Options.");

		category = config.getCategory(CATEGORY_MACHINES);
		category.setComment("Machine Options.");
		updateCategoryLang(category);
		category = config.getCategory(CATEGORY_WORLD_GEN);
		category.setComment("World Generation options.");
		updateCategoryLang(category);
		category = config.getCategory(CATEGORY_ABILITIES);
		category.setComment("Android Player Abilities");
		updateCategoryLang(category);
		category = config.getCategory(CATEGORY_STARMAP);
		category.setComment("Starmap feature options.");
		updateCategoryLang(category);

		category = config.getCategory(CATEGORY_COMPATIBILITY);
		category.setComment("Option for other mods");
		updateCategoryLang(category);

		category = config.getCategory(CATEGORY_GENERAL);
		category.setComment("General mod settings");
		updateCategoryLang(category);
		showFilledItems = config.getBoolean("show_filled_items", CATEGORY_GENERAL, false,
			"When false, filled variants of energy/matter items (batteries, containers) "
				+ "are hidden from creative inventory and JEI. Empty variants still appear.");


		legacyTritaniumCrateMigrationEnabled = config.getBoolean("legacy_tritanium_crate_migration",
				CATEGORY_GENERAL, true,
				"Migrates old per-color tritanium crate blocks to the unified new_tritanium_crate block. "
				+ "Disable only if you are certain your world never contained the old crate variants.");
		versionCheckChat = config.getBoolean(KEY_VERSION_CHECK_CHAT, CATEGORY_GENERAL, true,
				"When true, operators receive a chat notification on login if a newer version of Matter Overdrive is available.");

		config.get(CATEGORY_WORLD_GEN, CATEGORY_WORLD_SPAWN_ORES, true,
				"Should ores such as dilithium and tritanium ore spawn in the world. This applies for all ores !")
				.setLanguageKey(String.format("config.%s.name", CATEGORY_WORLD_SPAWN_ORES.replace(' ', '_')));
		config.get(CATEGORY_WORLD_GEN, CATEGORY_WORLD_SPAWN, true,
				"Should the Matter Overdrive Villager house be Generated?")
				.setLanguageKey(String.format("config.%s.name", CATEGORY_WORLD_SPAWN_OTHER.replace(' ', '_')));
		config.get(CATEGORY_WORLD_GEN,
				String.format("%s.%s", CATEGORY_WORLD_SPAWN, MatterOverdrive.BLOCKS.dilithium_ore.getTranslationKey()),
				true)
				.setLanguageKey(String.format("%s.name", MatterOverdrive.BLOCKS.dilithium_ore.getTranslationKey()));
		config.get(CATEGORY_WORLD_GEN,
				String.format("%s.%s", CATEGORY_WORLD_SPAWN, MatterOverdrive.BLOCKS.tritaniumOre.getTranslationKey()),
				true).setLanguageKey(String.format("%s.name", MatterOverdrive.BLOCKS.tritaniumOre.getTranslationKey()));

		config.getBoolean(KEY_AUTOMATIC_RECIPE_CALCULATION, CATEGORY_MATTER, true,
				"Shoud Matter be automaticly calculated from Recipes");
		AndroidPlayer.loadConfigs(this);

		save();
	}

	public void postInit() {
		config.load();
		for (IConfigSubscriber subscriber : subscribers) {
			subscriber.onConfigChanged(this);
		}
		save();
	}

	private void updateCategoryLang(ConfigCategory category) {
		category.setLanguageKey(String.format("config.%s.name", category.getName().replace(' ', '_')));
	}

	public boolean getBool(String key, String category, Boolean def, String comment) {
		return config.getBoolean(key, category, def, comment);
	}

	public boolean getBool(String key, String category, Boolean def) {
		return config.getBoolean(key, category, def, null);
	}

	public int getInt(String key, String category, Integer def, String comment) {
		Property property = config.get(category, key, def);
		property.setComment(comment);
		return property.getInt(def);
	}

	public int getInt(String key, String category, Integer def) {
		return getInt(key, category, def, "");
	}

	public void setInt(String key, String category, Integer def) {
		config.get(category, key, def).set(def);
	}

	public String[] getStringList(String category, String key) {
		return config.get(category, key, new String[0]).getStringList();
	}

	public String[] getStringList(String category, String key, String comment) {
		return config.get(category, key, new String[0], comment).getStringList();
	}

	public ConfigCategory getCategory(String cat) {
		return config.getCategory(cat);
	}

	public double getMachineDouble(String machine, String prop, double def, double min, double max, String comment) {
		Property p = config.get(CATEGORY_MACHINES + "." + machine.replaceFirst("tile.", ""), prop, def);
		p.setComment(comment);
		p.setLanguageKey(String.format("%s.config.%s", machine, prop));
		p.setMinValue(min);
		p.setMaxValue(max);
		return p.getDouble(def);
	}

	public double getMachineDouble(String machine, String prop, double def, String comment) {
		Property p = config.get(String.format("%s.%s", CATEGORY_MACHINES, machine.replaceFirst("tile.", "")), prop,
				def);
		p.setComment(comment);
		p.setLanguageKey(String.format("%s.config.%s", machine, prop));
		return p.getDouble(def);
	}

	public boolean getMachineBool(String machine, String prop, boolean def, String comment) {
		Property p = config.get(String.format("%s.%s", CATEGORY_MACHINES, machine.replaceFirst("tile.", "")), prop,
				def);
		p.setComment(comment);
		p.setLanguageKey(String.format("%s.config.%s", machine, prop));
		return p.getBoolean(def);
	}

	public int getMachineInt(String machine, String prop, int def, String comment) {
		Property p = config.get(String.format("%s.%s", CATEGORY_MACHINES, machine.replaceFirst("tile.", "")), prop,
				def);
		p.setComment(comment);
		p.setLanguageKey(String.format("%s.config.%s", machine, prop));
		return p.getInt(def);
	}

	public void initMachineCategory(String machine) {
		config.setCategoryLanguageKey(String.format("%s.%s", CATEGORY_MACHINES, machine.replaceFirst("tile.", "")),
				String.format("%s.name", machine));
	}

	public void save() {
		if (config.hasChanged()) {
			config.save();
		}
	}

	public void subscribe(IConfigSubscriber subscriber) {
        subscribers.add(subscriber);
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.getModID().equals(Reference.MOD_ID)) {
			starmapEnabled = config.getBoolean(KEY_STARMAP_ENABLED, CATEGORY_DEBUG, false, "");
			pylonEnabled = config.getBoolean("dimensional_pylon_enabled", CATEGORY_DEBUG, false, "");
			showFilledItems = config.getBoolean("show_filled_items", CATEGORY_GENERAL, false, "");
			showInDevItems = config.getBoolean("showInDevItems", CATEGORY_DEBUG, false, "");
			config.save();
		}

		for (IConfigSubscriber subscriber : subscribers) {
			subscriber.onConfigChanged(this);
		}
	}

	public void addCategoryToGui(List<IConfigElement> list) {
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_CLIENT)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_SERVER)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_WORLD_GEN)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_MACHINES)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_MATTER)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_WEAPON)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_ABILITIES)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_ENTITIES)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_DEBUG)));
		list.add(new ConfigElement(getCategory(ConfigurationHandler.CATEGORY_COMPATIBILITY)));
	}
}
