
package matteroverdrive.client.render.tileentity;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.Reference;
import matteroverdrive.blocks.includes.MOBlock;
import matteroverdrive.tile.TileEntityInscriber;
import matteroverdrive.util.RenderUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_QUADS;

public class TileEntityRendererInscriber extends TileEntitySpecialRenderer<TileEntityInscriber> {

	// Beam textures (reuses existing plasma FX assets)
	private static final ResourceLocation BEAM_CORE_TEX  = new ResourceLocation(Reference.PATH_FX + "forcefield_plasma_2.png");
	private static final ResourceLocation BEAM_OUTER_TEX = new ResourceLocation(Reference.PATH_FX + "forcefield_plasma.png");

	// Beam geometry in block-local Y (from OBJ: cone tip verts 9-12 = Y 0.750, tray top verts 41-44 = Y 0.660)
	private static final float BEAM_BOT_Y   = 0.660f;
	private static final float BEAM_TOP_Y   = 0.750f;
	private static final float BEAM_INNER_R = 0.018f;
	private static final float BEAM_OUTER_R = 0.036f;
	private static final int   BEAM_SEGS    = 8;

	// Must match the period used in TileEntityInscriber.spawnInscriberEffects()
	private static final int FLASH_PERIOD = 6;
	private static final int FLASH_ON     = 2;

	private final Random random;
	private float nextHeadX, nextHeadY;
	private float lastHeadX, lastHeadY;
	private EntityItem item;

	public TileEntityRendererInscriber() {
		random = new Random();
	}

	@Override
	public void render(TileEntityInscriber tileEntity, double x, double y, double z, float partialTicks,
			int destroyStage, float alpha) {
		if (!tileEntity.shouldRender())
			return;
		if (item == null) {
			item = new EntityItem(tileEntity.getWorld());
			item.setItem(new ItemStack(MatterOverdrive.ITEMS.isolinear_circuit, 1, 2));
		}

		// ── Laser beam + ambient face glow (purely client-side, zero chunk-light cost) ──
		if (tileEntity.isActive()) {
			long wt       = tileEntity.getWorld().getWorldTime();
			float t       = ((float) wt + partialTicks) * 0.4f;
			float flicker = computeFlicker(wt, partialTicks);
			// Hard flash on specific ticks; a dim organic glow persists between flashes.
			boolean flashOn  = wt % FLASH_PERIOD < FLASH_ON;
			float brightness = flashOn ? Math.max(flicker, 0.65f) : flicker * 0.25f;

			// Beam cylinder
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + 0.5, y, z + 0.5);
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL_ONE, GL_ONE);
			GlStateManager.disableLighting();
			GlStateManager.depthMask(false);
			RenderUtils.disableLightmap();
			GlStateManager.disableCull();
			// Hot orange-white core
			Minecraft.getMinecraft().renderEngine.bindTexture(BEAM_CORE_TEX);
			GlStateManager.color(brightness, 0.55f * brightness, 0.30f * brightness, 1.0f);
			renderBeamCylinder(BEAM_INNER_R, BEAM_BOT_Y, BEAM_TOP_Y, t);
			// Red outer column
			Minecraft.getMinecraft().renderEngine.bindTexture(BEAM_OUTER_TEX);
			GlStateManager.color(brightness, 0.05f * brightness, 0.0f, 1.0f);
			renderBeamCylinder(BEAM_OUTER_R, BEAM_BOT_Y, BEAM_TOP_Y, -t * 0.5f);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
			GlStateManager.depthMask(true);
			GlStateManager.enableLighting();
			GlStateManager.enableCull();
			RenderUtils.enableLightmap();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}

		GlStateManager.color(0, 0, 1, 0.5f);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		RenderUtils.rotateFromBlock(tileEntity.getWorld(), tileEntity.getPos());
		IBlockState blockState = tileEntity.getWorld().getBlockState(tileEntity.getPos());
		EnumFacing rotation = blockState.getValue(MOBlock.PROPERTY_DIRECTION);
		if (rotation == EnumFacing.EAST) {
			GlStateManager.translate(-0.75, 0, 0.5);
		} else if (rotation == EnumFacing.WEST) {
			GlStateManager.translate(0.25, 0, -0.5);
		} else if (rotation == EnumFacing.NORTH) {
			GlStateManager.translate(-0.75, 0, -0.5);
		} else {
			GlStateManager.translate(0.25, 0, 0.5);
		}

		ItemStack newStack = tileEntity.getStackInSlot(TileEntityInscriber.MAIN_INPUT_SLOT_ID);
		if (newStack.isEmpty()) {
			newStack = tileEntity.getStackInSlot(TileEntityInscriber.OUTPUT_SLOT_ID);
		}
		if (!newStack.isEmpty()) {
			item.setItem(newStack);
			GlStateManager.pushMatrix();
			GlStateManager.translate(-0.23, 0.69, 0);
			GlStateManager.rotate(90, 0, 1, 0);
			GlStateManager.rotate(90, 1, 0, 0);
			item.hoverStart = 0f;
			Minecraft.getMinecraft().getRenderManager().renderEntity(item, 0, 0, 0, 0, 0, true);
			GlStateManager.popMatrix();
		}
		GlStateManager.popMatrix();
	}

	/** Organic flicker: sum of incommensurable sines so the pattern never repeats. */
	private float computeFlicker(long wt, float partialTicks) {
		float t = (float) wt + partialTicks;
		float v = 0.60f
				+ 0.22f * (float) Math.sin(t * 0.293f)
				+ 0.12f * (float) Math.sin(t * 0.717f)
				+ 0.06f * (float) Math.sin(t * 2.031f);
		return Math.max(0.15f, Math.min(1.0f, v));
	}

	private void renderBeamCylinder(float radius, float botY, float topY, float timeOffset) {
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.getBuffer();
		float scrollV = timeOffset % 1.0f;
		buf.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		for (int i = 0; i < BEAM_SEGS; i++) {
			double a1 = i       * 2.0 * Math.PI / BEAM_SEGS;
			double a2 = (i + 1) * 2.0 * Math.PI / BEAM_SEGS;
			float u1 = (float) i       / BEAM_SEGS;
			float u2 = (float) (i + 1) / BEAM_SEGS;
			float bx1 = (float) Math.cos(a1) * radius;
			float bz1 = (float) Math.sin(a1) * radius;
			float bx2 = (float) Math.cos(a2) * radius;
			float bz2 = (float) Math.sin(a2) * radius;
			buf.pos(bx1, botY, bz1).tex(u1, 1.0 + scrollV).endVertex();
			buf.pos(bx2, botY, bz2).tex(u2, 1.0 + scrollV).endVertex();
			buf.pos(bx2, topY, bz2).tex(u2,       scrollV).endVertex();
			buf.pos(bx1, topY, bz1).tex(u1,       scrollV).endVertex();
		}
		tess.draw();
	}
}