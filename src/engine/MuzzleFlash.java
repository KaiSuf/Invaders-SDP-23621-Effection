package engine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Random;

/**
 * Short orange muzzle flash at the ship gun when the player shoots.
 * Fireball (white core, yellow, orange) with a fan of rays and a few
 * sparks. Colors cool from white/yellow to orange/ember as it fades.
 *
 * Team Effection - Visual Effects.
 */
public class MuzzleFlash {

	/** Flash time in milliseconds. */
	private static final int DURATION = 90;
	/** Flash size in pixels. */
	private static final float SIZE = 8f;
	/** Number of rays in the fan. */
	private static final int RAYS = 5;
	/** Number of sparks. */
	private static final int SPARKS = 4;
	/** Ray stroke width in pixels. */
	private static final float RAY_WIDTH = 2f;

	/** Hot core color. */
	private static final Color WHITE = new Color(255, 255, 240);
	/** Yellow color. */
	private static final Color YELLOW = new Color(255, 214, 64);
	/** Orange color. */
	private static final Color ORANGE = new Color(255, 140, 26);
	/** Ember color (end of fade). */
	private static final Color EMBER = new Color(200, 60, 10);

	/** Time of last trigger, -1 when idle. */
	private long startTime = -1;
	/** Random spark angles, new for each shot. */
	private final double[] sparkAngle = new double[SPARKS];
	/** Random spark distance factor, new for each shot. */
	private final float[] sparkDist = new float[SPARKS];
	/** Random source. */
	private final Random random = new Random();

	/**
	 * Starts a new flash. Call only when a shot is really fired.
	 */
	public final void trigger() {
		this.startTime = System.currentTimeMillis();
		for (int i = 0; i < SPARKS; i++) {
			// Up-facing cone: -150 to -30 degrees.
			this.sparkAngle[i] = Math.toRadians(-150 + random.nextInt(121));
			this.sparkDist[i] = 1.2f + random.nextFloat() * 1.0f;
		}
	}

	/**
	 * @return True while the flash is visible.
	 */
	public final boolean isActive() {
		return this.startTime >= 0
				&& System.currentTimeMillis() - this.startTime < DURATION;
	}

	/**
	 * Draws the flash centered at the gun tip.
	 *
	 * @param g Graphics to draw on.
	 * @param x Gun tip x.
	 * @param y Gun tip y.
	 */
	public final void draw(final Graphics g, final int x, final int y) {
		if (g == null || !isActive())
			return;
		float k = (System.currentTimeMillis() - this.startTime)
				/ (float) DURATION; // 0 -> 1
		float alpha = 1f - k;
		float size = SIZE * (1f - 0.6f * k);

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			// Rays: fan pointing up.
			Stroke old = g2.getStroke();
			g2.setStroke(new BasicStroke(RAY_WIDTH, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g2.setColor(fade(k < 0.4f ? YELLOW : ORANGE, alpha * 0.9f));
			float ray = size * 1.8f;
			for (int i = 0; i < RAYS; i++) {
				double a = Math.toRadians(-90 + (i - RAYS / 2) * 25);
				float len = (i == RAYS / 2) ? ray * 1.3f : ray;
				g2.drawLine(x, y, x + (int) (Math.cos(a) * len),
						y + (int) (Math.sin(a) * len));
			}
			g2.setStroke(old);

			// Sparks.
			g2.setColor(fade(k < 0.5f ? ORANGE : EMBER, alpha));
			for (int i = 0; i < SPARKS; i++) {
				float d = size * this.sparkDist[i] * (1f + k);
				int sx = x + (int) (Math.cos(this.sparkAngle[i]) * d);
				int sy = y + (int) (Math.sin(this.sparkAngle[i]) * d);
				g2.fillRect(sx - 1, sy - 1, 2, 2);
			}

			// Fireball: outer orange, mid yellow, white core.
			fillCircle(g2, x, y, size,
					fade(k < 0.5f ? ORANGE : EMBER, alpha * 0.8f));
			fillCircle(g2, x, y, size * 0.65f,
					fade(k < 0.4f ? YELLOW : ORANGE, alpha));
			fillCircle(g2, x, y, size * 0.3f,
					fade(k < 0.3f ? WHITE : YELLOW, alpha));
		} finally {
			g2.dispose();
		}
	}

	/** Fills a circle centered at (x, y). */
	private static void fillCircle(final Graphics2D g, final int x,
			final int y, final float r, final Color c) {
		g.setColor(c);
		int d = Math.max(1, Math.round(r * 2));
		g.fillOval(x - d / 2, y - d / 2, d, d);
	}

	/** Returns color with alpha 0..1. */
	private static Color fade(final Color c, final float a) {
		int al = Math.max(0, Math.min(255, Math.round(a * 255)));
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), al);
	}
}
