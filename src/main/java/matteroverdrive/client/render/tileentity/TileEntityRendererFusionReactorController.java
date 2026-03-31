
package matteroverdrive.client.render.tileentity;

import matteroverdrive.Reference;
import matteroverdrive.blocks.includes.MOBlock;
import matteroverdrive.client.data.Color;
import matteroverdrive.machines.fusionReactorController.TileEntityMachineFusionReactorController;
import matteroverdrive.util.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;

import java.text.DecimalFormat;

import static org.lwjgl.opengl.GL11.GL_ONE;

public class TileEntityRendererFusionReactorController
		extends TileEntitySpecialRenderer<TileEntityMachineFusionReactorController> {
	public TileEntityRendererFusionReactorController() {

	}

	@Override
	@SuppressWarnings("null")
	public void render(@Nonnull TileEntityMachineFusionReactorController controller, double x, double y, double z, float ticks,
			int destoryStage, float a) {
		if (!controller.shouldRender())
			return;
		if (!controller.isValidStructure()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, z);
			EnumFacing direction = getWorld().getBlockState(controller.getPos()).getValue(MOBlock.PROPERTY_DIRECTION);

			for (int i = 0; i < TileEntityMachineFusionReactorController.positionsCount; i++) {
				Vec3d pos = controller.getPosition(i, direction);

				if (TileEntityMachineFusionReactorController.blocks[i] == 255) {
					for (int offset = -TileEntityMachineFusionReactorController.MAX_GRAVITATIONAL_ANOMALY_DISTANCE;
							offset <= TileEntityMachineFusionReactorController.MAX_GRAVITATIONAL_ANOMALY_DISTANCE; offset++) {
						renderHologramCube(controller.getRelativePosition(
								TileEntityMachineFusionReactorController.positions[i * 2],
								offset,
								TileEntityMachineFusionReactorController.positions[(i * 2) + 1],
								direction));
					}
				} else {
					renderHologramCube(pos);
				}

			}
			GlStateManager.popMatrix();
		}

		renderInfo(x, y, z, controller);
	}

	@SuppressWarnings("null")
	private void renderHologramCube(Vec3d pos) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(pos.x, pos.y, pos.z);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL_ONE, GL_ONE);
		bindTexture(TileEntityRendererPatternMonitor.screenTextureBack);
		GlStateManager.translate(0.1, 0.1, 0.1);
		RenderUtils.drawCube(0.8, 0.8, 0.8, Reference.COLOR_HOLO);
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	@SuppressWarnings("null")
	private void renderInfo(double x, double y, double z, TileEntityMachineFusionReactorController controller) {
		EnumFacing side = controller.getWorld().getBlockState(controller.getPos()).getValue(MOBlock.PROPERTY_DIRECTION);

		Color color = Reference.COLOR_HOLO;
		if (!controller.isValidStructure()) {
			color = Reference.COLOR_HOLO_RED;
		}

		RenderUtils.beginDrawinngBlockScreen(x, y, z, side, color, controller);

		TileEntityMachineFusionReactorController.MonitorInfo monitorInfo = controller.getMonitorInfo();
		String[] info;
		if (monitorInfo == TileEntityMachineFusionReactorController.MonitorInfo.OK) {
			info = controller.getMonitorInfo().localize().replaceAll("\\$\\{power}", "100") // TODO
					.replaceAll("\\$\\{charge}",
							DecimalFormat.getPercentInstance()
									.format((double) controller.getEnergyStorage().getEnergyStored()
											/ (double) controller.getEnergyStorage().getMaxEnergyStored()))
					.replaceAll("\\$\\{matter}",
							DecimalFormat.getPercentInstance()
									.format((double) controller.getMatterStorage().getMatterStored()
											/ (double) controller.getMatterStorage().getCapacity()))
					.split("\\$\\{newline}");
		} else {
			info = controller.getMonitorInfo().localize().split("\\$\\{newline}");
		}

		RenderUtils.drawScreenInfoWithGlobalAutoSize(info, color, side, 10, 10, 4);

		RenderUtils.endDrawinngBlockScreen();

	}

}