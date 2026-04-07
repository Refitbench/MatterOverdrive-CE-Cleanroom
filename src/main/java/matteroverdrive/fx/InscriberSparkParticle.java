package matteroverdrive.fx;

import matteroverdrive.client.render.RenderParticlesHandler;
import matteroverdrive.proxy.ClientProxy;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Additive-blended welding-arc spark for the Inscriber.
 * Flies outward and upward from the cone tip, then curves downward
 * under gravity to produce a realistic arc trajectory.
 * Deliberately has NO inward-pull so it does not visually interact
 * with nearby Gravitational Stabilizer beam particles.
 */
@SideOnly(Side.CLIENT)
public class InscriberSparkParticle extends MOEntityFX {

	private static final double GRAVITY = 0.020;
	private static final double DRAG    = 0.88;

	private final float baseScale;
	private final float baseRed, baseGreen, baseBlue;

	/** Full-control constructor used by beam-glow and arc-spark effects. */
	public InscriberSparkParticle(World world, double x, double y, double z,
			double vx, double vy, double vz,
			float r, float g, float b,
			float scale, int maxAge) {
		super(world, x, y, z);
		this.motionX = vx;
		this.motionY = vy;
		this.motionZ = vz;

		this.baseRed   = r;
		this.baseGreen = g;
		this.baseBlue  = b;

		this.particleRed   = r;
		this.particleGreen = g;
		this.particleBlue  = b;
		this.particleAlpha = 1.0f;

		this.baseScale     = scale;
		this.particleScale = scale;
		this.particleMaxAge = maxAge;

		this.particleTexture = ClientProxy.renderHandler.getRenderParticlesHandler()
				.getSprite(RenderParticlesHandler.star);
	}

	/** Convenience constructor for arc sparks (randomised scale and age). */
	public InscriberSparkParticle(World world, double x, double y, double z,
			double vx, double vy, double vz,
			float r, float g, float b) {
		this(world, x, y, z, vx, vy, vz, r, g, b,
				0.09f + (float) Math.random() * 0.10f,
				10 + (int) (Math.random() * 8));
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;

		if (particleAge++ >= particleMaxAge) {
			setExpired();
			return;
		}

		// Gravity pulls the spark into a downward arc (no inward pull)
		motionY -= GRAVITY;

		// Drag slows horizontal flight so the arc looks natural
		motionX *= DRAG;
		motionY *= DRAG;
		motionZ *= DRAG;

		setBoundingBox(getBoundingBox().offset(motionX, motionY, motionZ));
		posX = (getBoundingBox().minX + getBoundingBox().maxX) / 2.0;
		posY = getBoundingBox().minY;
		posZ = (getBoundingBox().minZ + getBoundingBox().maxZ) / 2.0;
	}

	@Override
	public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks,
			float rotationX, float rotationZ, float rotationYZ,
			float rotationXY, float rotationXZ) {
		float ageF = ((float) particleAge + partialTicks) / (float) particleMaxAge;

		// Sparks appear at full brightness instantly, then dim as they cool
		float brightness = Math.max(0f, 1.0f - ageF * ageF);

		particleRed   = baseRed   * brightness;
		particleGreen = baseGreen * brightness;
		particleBlue  = baseBlue  * brightness;
		// Shrink slightly as brightness drops so they don't linger as dim blobs
		particleScale = baseScale * (0.4f + 0.6f * brightness);

		super.renderParticle(buffer, entity, partialTicks,
				rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
	}

	@Override
	public int getBrightnessForRender(float f) {
		return 0xF000F0;
	}
}
