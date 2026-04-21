
package matteroverdrive.items;

import java.util.List;

import org.lwjgl.input.Keyboard;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.Reference;
import matteroverdrive.api.internal.ItemModelProvider;
import matteroverdrive.client.ClientUtil;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.util.IConfigSubscriber;
import matteroverdrive.util.MOStringHelper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public class SpacetimeEqualizer extends ItemArmor implements ItemModelProvider, IBauble, IConfigSubscriber {
	public static boolean DAMPEN_MOVEMENT = true;
	public static int MAX_DURABILITY = 240;

	public SpacetimeEqualizer(String name) {
		super(ItemArmor.ArmorMaterial.IRON, 0, EntityEquipmentSlot.CHEST);
		setTranslationKey(Reference.MOD_ID + "." + name);
		setRegistryName(new ResourceLocation(Reference.MOD_ID, name));
		this.setCreativeTab(MatterOverdrive.TAB_OVERDRIVE);
		setMaxDamage(MAX_DURABILITY);
	}

	@Override
	public void initItemModel() {
		ClientUtil.registerModel(this, this.getRegistryName().toString());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemstack, @Nullable World worldIn, List<String> infos, ITooltipFlag flagIn) {
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
			addDetails(itemstack, Minecraft.getMinecraft().player, infos);
		} else {
			infos.add(MOStringHelper.MORE_INFO);
		}
	}

	@SideOnly(Side.CLIENT)
	public void addDetails(ItemStack itemstack, EntityPlayer player, List<String> infos) {
		if (MOStringHelper.hasTranslation(getTranslationKey() + ".details")) {
			infos.add(TextFormatting.GRAY + MOStringHelper.translateToLocal(getTranslationKey() + ".details"));
		}
		if (DAMPEN_MOVEMENT && MOStringHelper.hasTranslation(getTranslationKey() + ".effect.dampening")) {
			infos.add(TextFormatting.YELLOW + MOStringHelper.translateToLocal(getTranslationKey() + ".effect.dampening"));
		}
		int drainRate = matteroverdrive.tile.TileEntityGravitationalAnomaly.EQUALIZER_DAMAGE_RATE;
		if (drainRate > 0 && MOStringHelper.hasTranslation(getTranslationKey() + ".effect.drain")) {
			double seconds = drainRate / 20.0;
			String rateStr = seconds == (long) seconds
					? String.valueOf((long) seconds)
					: String.format("%.1f", seconds);
			infos.add(TextFormatting.DARK_AQUA + MOStringHelper.translateToLocal(
					getTranslationKey() + ".effect.drain", rateStr));
		}
	}

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
		return Reference.PATH_ARMOR + this.getTranslationKey().substring(5) + "_"
				+ (this.armorType == EntityEquipmentSlot.CHEST ? "2" : "1") + ".png";
	}

	@Override
	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
		// Return empty map so vanilla doesn't insert a blank line + armor stat section
		return HashMultimap.create();
	}

	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
		applyEqualizerEffect(player);
	}

	// --- IBauble ---

	@Override
	public BaubleType getBaubleType(ItemStack stack) {
		return BaubleType.BODY;
	}

	@Override
	public void onWornTick(ItemStack stack, EntityLivingBase player) {
		if (player instanceof EntityPlayer) {
			applyEqualizerEffect((EntityPlayer) player);
		}
	}

	private void applyEqualizerEffect(EntityPlayer player) {
		if (!DAMPEN_MOVEMENT || player.capabilities.isFlying) return;
		player.motionX *= 0.5;
		if (player.motionY > 0) {
			player.motionY *= 0.9;
		}
		player.motionZ *= 0.5;
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		config.initMachineCategory("spacetime_equalizer");
		DAMPEN_MOVEMENT = config.getMachineBool("spacetime_equalizer", "dampen_movement", true,
				"When true, wearing the Space-Time Equalizer reduces the player's movement speed");
		MAX_DURABILITY = config.getMachineInt("spacetime_equalizer", "max_durability", 240,
				"Maximum durability of the Space-Time Equalizer (default 240 = ~40 min continuous use near an anomaly at the default drain rate)");
		setMaxDamage(MAX_DURABILITY);
	}

}
