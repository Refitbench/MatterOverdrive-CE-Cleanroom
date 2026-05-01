package matteroverdrive.items;

import matteroverdrive.blocks.BlockNewTritaniumCrate;
import matteroverdrive.items.includes.MOMachineBlockItem;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class CrateBlockItem extends MOMachineBlockItem {
	public CrateBlockItem(Block block) {
		super(block);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> infos, ITooltipFlag flagIn) {
		if (stack.hasTagCompound()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag == null) {
				super.addInformation(stack, worldIn, infos, flagIn);
				return;
			}

			int colorIdx = tag.getInteger("Color");
			boolean hasItems = tag.hasKey("Items", Constants.NBT.TAG_LIST)
					&& tag.getTagList("Items", Constants.NBT.TAG_COMPOUND).tagCount() > 0;

			boolean hasColor = colorIdx > 0 && colorIdx < BlockNewTritaniumCrate.Color.values().length;

			if (hasColor || hasItems) {
				StringBuilder sb = new StringBuilder();

				if (hasColor) {
					BlockNewTritaniumCrate.Color color = BlockNewTritaniumCrate.Color.values()[colorIdx];
					String colorName = formatColorName(color.getName());
					TextFormatting colorFmt = getColorFormatting(color);
					sb.append(colorFmt).append("[").append(colorName).append("]");
				}

				if (hasItems) {
					if (sb.length() > 0) {
						sb.append(TextFormatting.YELLOW).append(" - ");
					}
					sb.append(TextFormatting.YELLOW).append("Contents Preserved");
				}

				infos.add(sb.toString());
			}
		}

		super.addInformation(stack, worldIn, infos, flagIn);
	}

	private static TextFormatting getColorFormatting(BlockNewTritaniumCrate.Color color) {
		switch (color) {
			case RED:         return TextFormatting.RED;
			case GREEN:       return TextFormatting.DARK_GREEN;
			case BROWN:       return TextFormatting.GOLD;
			case BLUE:        return TextFormatting.BLUE;
			case PURPLE:      return TextFormatting.DARK_PURPLE;
			case CYAN:        return TextFormatting.DARK_AQUA;
			case LIGHTGRAY:   return TextFormatting.GRAY;
			case GRAY:        return TextFormatting.DARK_GRAY;
			case PINK:        return TextFormatting.LIGHT_PURPLE;
			case LIME:        return TextFormatting.GREEN;
			case YELLOW:      return TextFormatting.YELLOW;
			case LIGHTBLUE:   return TextFormatting.AQUA;
			case MAGENTA:     return TextFormatting.LIGHT_PURPLE;
			case ORANGE:      return TextFormatting.GOLD;
			case SECOND_WHITE:
			case WHITE:       return TextFormatting.WHITE;
			case BLACK:       return TextFormatting.DARK_GRAY;
			default:          return TextFormatting.AQUA;
		}
	}

	private static String formatColorName(String raw) {
		String[] parts = raw.split("_");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}
}
