package engine;

import java.awt.Color;


import engine.DrawManager.SpriteType;
import entity.Ship;

/**
 * Ghost Trail ship effect (shop item "effect_ghost_trail"). While the
 * player's ship moves, a few fading copies of it follow behind. When the
 * ship stops, the copies disappear. Look only, no gameplay change.
 * AUTHORED BY: VFX TEAM (Effection)
 */
public final class GhostTrail {

	/** How many past positions are remembered (one per frame). */
	private static final int HISTORY = 17;
    /** How many frames back each ghost is drawn, nearest first. */
	private static final int[] GHOST_FRAMES = { 4, 8, 12, 16 };
	/** Opacity of each ghost (0-255), nearest first. */
	private static final int[] GHOST_ALPHA = { 150, 115, 80, 45 };
	/** Ghost colours, used in turn. */
	private static final Color[] GHOST_COLORS = {
			new Color(0, 220, 255), new Color(160, 90, 255) };
	/** Pixels the ship must move over the history to count as moving. */
	private static final int MIN_MOVE = 4;
	/** Frames looked back to measure the ship's speed. */
	private static final int SPEED_FRAMES = 4;
   	/** Speed (pixels per frame) at which ghosts are fully visible. */
	private static final double FULL_SPEED = 4.0;
   	/** How visible ghosts are at the slowest speed (0-1). */
	private static final double MIN_STRENGTH = 0.4;

	/** Past ship x positions, a ring buffer (no new objects per frame). */
	private final int[] historyX = new int[HISTORY];
	/** Past ship y positions, same slots as {@link #historyX}. */
	private final int[] historyY = new int[HISTORY];
	/** Past ship colours as RGB, same slots as {@link #historyX}. */
	private final int[] historyColor = new int[HISTORY];
	/** Slot of the newest position. */
	private int newest = -1;
	/** How many positions are stored (at most HISTORY). */
	private int count;

	/**
	 * Remembers where the ship is this frame, and its colour. Call once
	 * per frame.
	 *
	 * @param x
	 *            Ship x.
	 * @param y
	 *            Ship y.
	 * @param color
	 *            Ship colour this frame (skin colour).
	 */
	public void record(final int x, final int y, final Color color) {
		this.newest = (this.newest + 1) % HISTORY;
		this.historyX[this.newest] = x;
		this.historyY[this.newest] = y;
		this.historyColor[this.newest] = color.getRGB();
		if (this.count < HISTORY)
			this.count++;
	}

	/** @return True if the ship moved enough lately to show ghosts. */
	public boolean isMoving() {
		if (this.count < 2)
			return false;
		return Math.abs(this.historyX[this.newest] - this.historyX[slotFramesAgo(this.count - 1)]) >= MIN_MOVE;
	}

	/** Forgets every remembered position, so no ghosts show. */
	public void clear() {
		this.count = 0;
	}
	
    /**
	 * Records the ship's position, then draws its ghosts behind it.
	 * Call every frame, just before the ship is drawn.
	 *
	 * @param drawManager
	 *            Draw manager.
	 * @param ship
	 *            Player ship.
	 * @param shipColor
	 *            Skin colour of the ship this frame; ghosts keep it.
	 */
	public void draw(final DrawManager drawManager, final Ship ship,final Color shipColor) {
		// No ghosts while the ship is destroyed; start fresh after.
		if (ship.isDestroyed()) {
			clear();
			return;
		}
		
		record(ship.getPositionX(), ship.getPositionY(), shipColor);
		if (!isMoving())
			return;
		double strength = speedStrength();
		// Farthest ghost first, so nearer ghosts are drawn on top.
		for (int k = GHOST_FRAMES.length - 1; k >= 0; k--) {
			int slot = slotFramesAgo(GHOST_FRAMES[k]);
			if (slot < 0)
				continue;
			Color base = new Color(this.historyColor[slot]);
			int alpha = (int) Math.round(GHOST_ALPHA[k] * strength);
			drawManager.drawSprite(ship.getSpriteType(),
					this.historyX[slot], this.historyY[slot],
					new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
		}
	}
	
	/**
   	 * @return How visible the ghosts are (0-1): faster ship, stronger
   	 *         ghosts.
   	 */
	private double speedStrength() {
		int older = slotFramesAgo(SPEED_FRAMES);
		if (older < 0)
			return 1;
		double speed = Math.abs(this.historyX[this.newest] - this.historyX[older]) / (double) SPEED_FRAMES;
		return MIN_STRENGTH + (1 - MIN_STRENGTH) * Math.min(1, speed / FULL_SPEED);
	}

	/**
	 * @param frames
	 *            Frames back (0 = now).
	 * @return Ring buffer slot of that frame, or -1 if not stored yet.
	 */
	private int slotFramesAgo(final int frames) {
		if (frames >= this.count)
			return -1;
		return (this.newest - frames + HISTORY) % HISTORY;
	}
    
	/**
	 * Shop preview: the ship slides left and right with its ghosts behind.
	 *
	 * @param drawManager
	 *            Draw manager.
	 * @param sprite
	 *            Ship sprite to show.
	 * @param shipColor
	 *            Ship colour (selected skin).
	 * @param centerX
	 *            Horizontal centre of the preview.
	 * @param top
	 *            Top of the preview.
	 */
	public static void drawPreview(final DrawManager drawManager,
			final SpriteType sprite, final Color shipColor,
			final int centerX, final int top) {
		final int pixel = 3;
		final int frameMs = 16;
		long now = System.currentTimeMillis();
		int[] size = drawManager.getSpriteSize(sprite);
		int shipY = top + 20;
		for (int k = GHOST_FRAMES.length - 1; k >= 0; k--) {
			Color base = GHOST_COLORS[k % GHOST_COLORS.length];
			drawManager.drawSpriteScaled(sprite, previewX(centerX,
					size[0] * pixel, now - GHOST_FRAMES[k] * frameMs), shipY,
					pixel, new Color(base.getRed(), base.getGreen(),
							base.getBlue(), GHOST_ALPHA[k]));
		}
		drawManager.drawSpriteScaled(sprite, previewX(centerX,
				size[0] * pixel, now), shipY, pixel, shipColor);
	}

	/**
	 * @return Left edge of the preview ship at a given time: it slides
	 *         36 px each way and pauses at the ends.
	 */
	private static int previewX(final int centerX, final int width,
			final long time) {
		double phase = (time % 2400) / 2400.0 * 2 * Math.PI;
		double s = Math.max(-1, Math.min(1, Math.sin(phase) * 1.4));
		return centerX - width / 2 + (int) Math.round(s * 36);
	}
}
