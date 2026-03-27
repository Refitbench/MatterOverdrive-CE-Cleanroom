
package matteroverdrive.compat.modules.jei;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.container.ContainerInscriber;
import matteroverdrive.data.recipes.InscriberRecipe;
import matteroverdrive.gui.GuiInscriber;
import matteroverdrive.init.MatterOverdriveRecipes;
import mezz.jei.api.ICollapsibleGroupRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

/**
 * @author shadowfacts
 */
@JEIPlugin
public class MOJEIPlugin implements IModPlugin {
	private static final String SUBTYPE_EMPTY = "";
	private static final String SUBTYPE_FULL = "full";

	@Override
	public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.battery);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.hc_battery);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.creative_battery);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.phaser);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.phaserRifle);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.plasmaShotgun);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.ionSniper);
		registerEnergySubtype(subtypeRegistry, MatterOverdrive.ITEMS.omniTool);
		if (MatterOverdrive.ITEMS.matterContainer != null) {
			subtypeRegistry.registerSubtypeInterpreter(MatterOverdrive.ITEMS.matterContainer,
					stack -> getMatterSubtype(stack));
		}
	}

	private static void registerEnergySubtype(ISubtypeRegistry subtypeRegistry, Item item) {
		if (item != null) {
			subtypeRegistry.registerSubtypeInterpreter(item, stack -> getEnergySubtype(stack));
		}
	}

	@Override
	public void registerCollapsibleGroups(ICollapsibleGroupRegistry registry) {
		registry.addGroup(
				"matteroverdrive:colored_floor_tile",
				I18n.format("tile.decorative.floor_tile.name"),
				stack -> Block.getBlockFromItem(stack.getItem()) == MatterOverdrive.BLOCKS.decorative_floor_tile);
		registry.addGroup(
				"matteroverdrive:colored_floor_tiles",
				I18n.format("tile.decorative.floor_tiles.name"),
				stack -> Block.getBlockFromItem(stack.getItem()) == MatterOverdrive.BLOCKS.decorative_floor_tiles);
		registry.addGroup(
				"matteroverdrive:colored_tritanium_plate",
				I18n.format("tile.decorative.tritanium_plate_colored.name"),
				stack -> Block.getBlockFromItem(stack.getItem()) == MatterOverdrive.BLOCKS.decorative_tritanium_plate_colored);
		registry.addGroup(
				"matteroverdrive:color_modules",
				I18n.format("item.matteroverdrive.weapon_module_color.name"),
				stack -> stack.getItem() == MatterOverdrive.ITEMS.weapon_module_color);
		registry.addGroup(
				"matteroverdrive:contracts",
				I18n.format("item.matteroverdrive.contract.name"),
				stack -> stack.getItem() == MatterOverdrive.ITEMS.contract);
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		registry.addRecipeCategories(new InscriberRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerIngredients(IModIngredientRegistration registry) {

	}

	@Override
	public void register(@Nonnull IModRegistry registry) {
		registry.handleRecipes(InscriberRecipe.class, new InscriberRecipeHandler(), InscriberRecipeCategory.UID);

		registry.addRecipes(MatterOverdriveRecipes.INSCRIBER.getRecipes(), InscriberRecipeCategory.UID);

		registry.addRecipeCatalyst(new ItemStack(MatterOverdrive.BLOCKS.inscriber), InscriberRecipeCategory.UID);

		registry.getRecipeTransferRegistry().addRecipeTransferHandler(ContainerInscriber.class,
				InscriberRecipeCategory.UID, 0, 2, 8, 36);

		registry.addRecipeClickArea(GuiInscriber.class, 32, 55, 24, 16, InscriberRecipeCategory.UID);

		registry.addAdvancedGuiHandlers(new MOAdvancedGuiHandler());

	}

	private static String getEnergySubtype(ItemStack stack) {
		if (stack.isEmpty()) {
			return SUBTYPE_EMPTY;
		}

		return matteroverdrive.items.includes.MOItemEnergyContainer.getStorage(stack).getEnergyStored() > 0
				? SUBTYPE_FULL
				: SUBTYPE_EMPTY;
	}

	private static String getMatterSubtype(ItemStack stack) {
		if (stack.isEmpty() || !stack.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
			return SUBTYPE_EMPTY;
		}

		IFluidHandler fluidHandler = stack.getCapability(
				net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
		if (fluidHandler == null) {
			return SUBTYPE_EMPTY;
		}

		FluidStack fluidStack = fluidHandler.drain(Integer.MAX_VALUE, false);
		return fluidStack != null && fluidStack.amount > 0 ? SUBTYPE_FULL : SUBTYPE_EMPTY;
	}

}