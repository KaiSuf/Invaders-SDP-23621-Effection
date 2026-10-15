package engine;
 
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.util.Random;
 
import entity.Ship;
 
/**
 * Warp In ship effect (shop item "effect_warp_in"). The player's ship
 * beams in at the start of each level: corner
 * brackets lock on, a rift opens at the top, a beam strikes down, the
 * ship rains in as a hologram and a scan line turns it solid.
 * <p>
 * Everything is worked out from the warp's progress (0 to 1) and a random
 * seed, so every frame can be drawn on its own. While the ship is not solid
 * yet the warp draws it and GameScreen skips the normal ship
 * ({@link #update} returns true). The ship turns solid on "GO!".
 * Look only.
 * AUTHORED BY: VFX TEAM (Effection)
 */
public final class WarpIn {
 
	/** Length of the level start warp, in milliseconds. */
	private static final int START_DURATION = 2200;
	/** Time from "GO!" to the end of the countdown, in milliseconds. */
	private static final int GO_TIME = 1000;
	/** Timeline (parts of the warp): lock-on brackets close in. */
	private static final double LOCK_END = 0.2;
	/** Timeline: the beam falls from the rift to the ground. */
	private static final double STRIKE_START = 0.18, STRIKE_END = 0.28;
	/** Timeline: hologram pixels rain in. */
	private static final double RAIN_START = 0.26, RAIN_END = 0.6;
	/** Timeline: one pixel's fall. */
	private static final double PIXEL_FALL = 0.12;
	/** Timeline: the scan line sweeps up, then the ship is solid. */
	private static final double SCAN_START = 0.6, IMPACT = 0.68;
 
	/** Random source; a new seed per warp so each one looks a bit new. */
	private final Random random = new Random();
	/** Moment the current warp starts, or -1 when none is planned. */
	private long startTime = -1;
	/** Length of the current warp, in milliseconds. */
	private int duration;
	/** Seed for the current warp. */
	private long seed;
 
	/**
	 * Plans the level start warp so the ship turns solid on "GO!".
	 *
	 * @param countdownMillis
	 *            Length of the level start countdown (INPUT_DELAY).
	 */
	public void startLevel(final int countdownMillis) {
		plan(countdownMillis - GO_TIME - (long) (IMPACT * START_DURATION),
				START_DURATION);
	}
 
	/** Plans a warp starting in {@code delay} ms, lasting {@code length}. */
	private void plan(final long delay, final int length) {
		this.startTime = System.currentTimeMillis() + Math.max(0, delay);
		this.duration = length;
		this.seed = this.random.nextLong();
	}
 
	/** @return Progress of the current warp (below 0 before it starts). */
	private double progress() {
		return (System.currentTimeMillis() - this.startTime)
				/ (double) this.duration;
	}
 
	/**
	 * Says whether the warp hides the ship. Call once per frame, before
	 * drawing the ship.
	 *
	 * @return True while the warp draws the ship instead of GameScreen.
	 */
	public boolean update() {
		return this.startTime >= 0 && progress() < IMPACT;
	}
 
	/**
	 * Draws the warp, if one is running.
	 *
	 * @param g
	 *            Graphics of the back buffer.
	 * @param image
	 *            Ship sprite (its normal, not destroyed, sprite).
	 * @param ship
	 *            Player ship.
	 * @param color
	 *            Ship colour this frame (skin colour).
	 * @param topY
	 *            Top of the play area; the rift opens here.
	 */
	public void draw(final Graphics2D g, final boolean[][] image,
			final Ship ship, final Color color, final int topY) {
		if (this.startTime < 0 || image == null)
			return;
		double p = progress();
		if (p >= 1)
			this.startTime = -1;
		else if (p >= 0)
			drawWarp(g, image, ship.getPositionX(), ship.getPositionY(),
					color, topY, p, this.seed);
	}
 
	/** Draws one moment (p = 0 to 1) of the warp, in order. */
	private static void drawWarp(final Graphics2D g, final boolean[][] image,
			final int x, final int y, final Color color, final int topY,
			final double p, final long seed) {
		Composite oldComposite = g.getComposite();
		Paint oldPaint = g.getPaint();
		Color light = mix(color, Color.WHITE, 0.55);
		int w = image.length * 2;
		int h = image[0].length * 2;
		double cx = x + w / 2.0;
		double riftY = topY + 10;
 
		// 1. Rift: a glowing tear that opens, crackles and closes.
		double open = p < IMPACT ? ease((p - 0.04) / (STRIKE_START - 0.04))
				: 1 - ease((p - IMPACT) / 0.18);
		if (open > 0) {
			double rw = 60 * open;
			double rh = 9 * open;
			float glow = (float) Math.max(1, rw * 0.75);
			g.setComposite(AlphaComposite.SrcOver);
			g.setPaint(new RadialGradientPaint((float) cx, (float) riftY,
					glow, new float[] { 0f, 1f }, new Color[] {
							alpha(color, 110), alpha(color, 0) }));
			g.fillOval((int) (cx - glow), (int) (riftY - glow * 0.35),
					(int) (glow * 2), (int) (glow * 0.7));
			setAlpha(g, 0.9f);
			g.setColor(color);
			g.fillOval((int) (cx - rw / 2), (int) (riftY - rh / 2),
					(int) rw, (int) Math.max(1, rh));
			g.setColor(new Color(10, 0, 20));
			g.fillOval((int) (cx - rw / 2 + 3), (int) (riftY - rh / 2 + 2),
					(int) Math.max(0, rw - 6), (int) Math.max(0, rh - 4));
			Random crackle = new Random(seed + (long) (p * 60));
			setAlpha(g, (float) open);
			for (int i = 0; i < 6; i++) {
				double a = crackle.nextDouble() * Math.PI * 2;
				g.setColor(i % 2 == 0 ? Color.WHITE : light);
				g.fillRect((int) (cx + Math.cos(a) * (rw / 2 + 2)),
						(int) (riftY + Math.sin(a) * (rh / 2 + 2)), 2, 2);
			}
		}
 
		// 2. Lock-on: corner brackets blink and close in, then fade.
		double lockFade = 1 - (p - LOCK_END) / (STRIKE_END - LOCK_END);
		double q = Math.min(1, p / LOCK_END);
		if (lockFade > 0 && (q >= 1 || ((int) (q * 10)) % 2 == 0)) {
			int m = (int) (4 + 34 * (1 - ease(q)));
			setAlpha(g, (float) Math.min(1, lockFade));
			g.setColor(light);
			for (int corner = 0; corner < 4; corner++) {
				int sx = corner % 2 == 0 ? -1 : 1;
				int sy = corner < 2 ? -1 : 1;
				int bx = sx < 0 ? x - m : x + w + m - 2;
				int by = sy < 0 ? y - m : y + h + m - 2;
				g.fillRect(sx < 0 ? bx : bx - 4, by, 6, 2);
				g.fillRect(bx, sy < 0 ? by : by - 4, 2, 6);
			}
		}
 
		// 3. Beam: strikes down from the rift, pulses, then thins away.
		if (p >= STRIKE_START && p < IMPACT + 0.1) {
			double bottom = riftY + (y + h - riftY)
					* ease((p - STRIKE_START) / (STRIKE_END - STRIKE_START));
			double fade = p < IMPACT ? 1 : 1 - (p - IMPACT) / 0.1;
			double width = p < RAIN_START ? 2 : p >= IMPACT ? 2 * fade
					: 2 + w * 0.75 * Math.sin(Math.PI * (p - RAIN_START)
							/ (IMPACT - RAIN_START));
			float half = (float) (width / 2 + 10 * fade);
			float in = (float) Math.min(0.9, width / 2 / half);
			g.setComposite(AlphaComposite.SrcOver);
			g.setPaint(new LinearGradientPaint((float) cx - half, 0,
					(float) cx + half, 0, new float[] { 0f, 0.5f - in / 2,
							0.5f, 0.5f + in / 2, 1f },
					new Color[] { alpha(color, 0), alpha(color,
							(int) (150 * fade)), alpha(light,
							(int) (235 * fade)), alpha(color,
							(int) (150 * fade)), alpha(color, 0) }));
			g.fillRect((int) (cx - half), (int) riftY, (int) (half * 2) + 1,
					(int) (bottom - riftY));
			setAlpha(g, (float) fade);
			g.setColor(Color.WHITE);
			g.fillRect((int) cx - 1, (int) riftY, 2, (int) (bottom - riftY));
			if (p < STRIKE_END + 0.03)
				g.fillRect((int) cx - 3, (int) bottom - 3, 6, 6);
		}
 
		// 4. Sparks swirl in, and the ship rains in as a hologram that a
		// scan line turns solid from the bottom up.
		if (p >= RAIN_START && p < IMPACT) {
			Random sparks = new Random(seed);
			for (int i = 0; i < 26; i++) {
				double begin = RAIN_START + sparks.nextDouble() * 0.24;
				double a0 = sparks.nextDouble() * Math.PI * 2;
				double r0 = 34 + sparks.nextDouble() * 44;
				double k = (p - begin) / 0.18;
				if (k < 0 || k > 1)
					continue;
				double r = r0 * (1 - ease(k));
				setAlpha(g, (float) Math.sin(Math.PI * k));
				g.setColor(i % 3 == 0 ? Color.WHITE : light);
				g.fillRect((int) (cx + Math.cos(a0 + k * 2.6) * r),
						(int) (y + h / 2 + Math.sin(a0 + k * 2.6) * r * 0.55),
						2, 2);
			}
			int rows = image[0].length;
			double scan = (p - SCAN_START) / (IMPACT - SCAN_START);
			double scanRow = rows - scan * rows;
			Random pixels = new Random(~seed);
			for (int i = 0; i < image.length; i++)
				for (int j = 0; j < rows; j++) {
					double order = (rows - 1 - j) / (double) rows * 0.6
							+ pixels.nextDouble() * 0.4;
					int drop = 20 + pixels.nextInt(50);
					double k = (p - RAIN_START - order
							* (RAIN_END - RAIN_START - PIXEL_FALL)) / PIXEL_FALL;
					if (!image[i][j] || k <= 0)
						continue;
					int px = x + i * 2;
					int py = y + j * 2;
					if (k < 1) {
						int fy = (int) (py - drop * (1 - ease(k)));
						setAlpha(g, (float) (0.4 * (1 - k)));
						g.setColor(light);
						g.fillRect(px, fy - 9, 2, 9);
						setAlpha(g, (float) Math.min(1, k * 2));
						g.setColor(Color.WHITE);
					} else if (scan > 0 && j >= scanRow) {
						setAlpha(g, 1f);
						g.setColor(color);
					} else {
						setAlpha(g, (j + (int) (p * 70)) % 3 == 0 ? 0.35f
								: 0.75f);
						g.setColor(light);
					}
					g.fillRect(px, k < 1 ? (int) (py - drop * (1 - ease(k)))
							: py, 2, 2);
				}
			if (scan > 0) {
				setAlpha(g, 0.9f);
				g.setColor(Color.WHITE);
				g.fillRect(x - 4, (int) (y + scanRow * 2), w + 8, 1);
			}
		}
 
		g.setComposite(oldComposite);
		g.setPaint(oldPaint);
	}
 
	/** Ease-out curve, fast then soft (input clamped to 0-1). */
	private static double ease(final double t) {
		double u = 1 - Math.max(0, Math.min(1, t));
		return 1 - u * u * u;
	}
 
	/** Sets how see-through the next drawings are (0-1). */
	private static void setAlpha(final Graphics2D g, final float a) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				Math.max(0f, Math.min(1f, a))));
	}
 
	/** @return The colour with a new alpha (0-255). */
	private static Color alpha(final Color c, final int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(),
				Math.max(0, Math.min(255, a)));
	}
 
	/** @return Colour a blended toward b by t (0-1). */
	private static Color mix(final Color a, final Color b, final double t) {
		return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}
}

